package sssundar.reva.boogie;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.net.sip.*;

import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;


public class DoodlerActivity extends Activity implements OnClickListener {

    public DrawingView drawView;
    public ImageButton eraseBtn, callIssueBtn, hangUpBtn;
    public ImageButton callAmmaBtn, callBaluBtn, callSushBtn, callManiBtn;

    // onStart  start up background threads (presence, call manager, screen sampler)
    // onResume start listeners (connectivity, sip) and allow registration and presence polling subject to network (check once then rely on broadcasts)
    // onPause end active call if exists, disallow advertising presence and invalidate registration, and kill listeners
    // onStop kill background threads
    // onBackPressed do nothing

    // The screen_sampler during ongoing calls
    public Thread screen_sampler;

    // This, subject to network, first registers, then polls (watching for call quality issues noted by the user).
    // It registers roughly every minute and polls every 5 seconds or so.
    public Thread presence_poller;



    // 0 = Sushant, 1 = Mani, 2 = Amma, 3 = Balu
    // 1 = Source Mode
    // 0, 2, 3 = Receiver Mode
    public final static int userID= 2;

    // TODO INSERT CREDENTIALS

    // First index screen_sampler = null;is user (userID)
    // Second index is credentials (0 = extension, 1 = asterisk secret, 2 = cherrypy secret, 3 = sip proxy, 4 = sip address)
    public final String[][] credentials = {sush_credentials, mani_credentials, amma_credentials, balu_credentials};


    // Variables handled by presence_poller
    public volatile boolean network_active = false;
    public volatile boolean should_report_quality_issue = false;
    public volatile boolean[] presence_array = new boolean[4];

    // Variables handled by call_ui_manager
    public IncomingCallReceiver callReceiver;
    public SipManager manager = null;
    public SipProfile me = null;
    public boolean registered = false;
    public SipAudioCall call = null;
    public int callerID = -1;

    private void start_background_threads () {
        // Start Network Registrar, Presence, and Call Quality Monitor
        presence_poller = new Thread(new Runnable() {
            private ConnectivityManager cm = null;
            private int network_toasts = 1;

            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Log.d("Boogie", "Presence thread was interrupted before it could start");
                }

                cm = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                while (!Thread.interrupted()) {
                    poll();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Log.d("Boogie", "Presence thread was interrupted mid-nap");
                    }
                }
            }

            private String readIt(InputStream stream, int len) throws Exception {
                Reader reader = null;
                reader = new InputStreamReader(stream, "UTF-8");
                char[] buffer = new char[len];
                reader.read(buffer);
                return new String(buffer);
            }

            private void poll () {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = (activeNetwork != null) && activeNetwork.isConnected();
                boolean old_network = network_active;
                network_active = isConnected && (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);

                if (!old_network && network_active) {
                    // Network was inactive and we need to get things bootstrapped again
                    closeLocalProfile();
                    initializeManager(); // autoregisters
                    network_toasts = 1;
                }

                if (!network_active) {
                    network_toasts -= 1;
                    if (network_toasts >= 0) {
                        toast_network_loss();
                        if (call != null) {
                            try {
                                call.endCall();
                            } catch (SipException e) {
                                call.close();
                            }
                            clean_up_after_call();
                        }
                        closeLocalProfile();
                        for (int k = 0; k < 4; k++) {
                            presence_array[k] = false;
                        }
                        updateSidebarWithPresence();
                    }
                    return;
                }

                // Poll for Presence
                InputStream is = null;
                String url = "http://" + credentials[0][3] + ":8080/";
                String quality = "";
                int cID = callerID;
                if ((userID == 1) && should_report_quality_issue && (cID >= 0)) {
                    quality = "1";
                    should_report_quality_issue = false;
                } else {
                    quality = "0";
                }

                if (cID >= 0) {
                    url += "/?extension=" + credentials[userID][0] + "&&password=" + credentials[userID][2] + "&&presence=yes&&callquality=" + quality + "&&with_extension=" + credentials[cID][0];
                } else {
                    url += "/?extension=" + credentials[userID][0] + "&&password=" + credentials[userID][2] + "&&presence=yes&&callquality=0";
                }

                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    ec2.setReadTimeout(1000 /* milliseconds */);
                    ec2.setConnectTimeout(1500 /* milliseconds */);
                    ec2.setRequestMethod("GET");
                    ec2.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
                    ec2.setDoInput(true);
                    ec2.connect();
                    int response = ec2.getResponseCode();
                    is = ec2.getInputStream();
                    String contentAsString = readIt(is, 5); // Max 10 byte presence string.
                    for (int k = 0; k < 4; k++) {
                        if (contentAsString.charAt(k) == '1') {
                            presence_array[k] = true;
                        } else {
                            presence_array[k] = false;
                        }
                    }

                    if (contentAsString.charAt(4) == '1') {
                        toast_call_quality_issue();
                    }

                } catch (Exception e) {
                    Log.d("Boogie", "Exception in Presence Poll.", e);
                    for (int k = 0; k < 4; k++) {
                        presence_array[k] = false;
                    }
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e) {
                            Log.d("Boogie", "Exception closing Presence Poll Input Stream.");
                        }
                    }
                }

                updateSidebarWithPresence();

        }

        });
        presence_poller.start();
    }
    private void kill_background_threads () {
        // Kill Network, Registrar, Presence, and Call Quality Monitor
        presence_poller.interrupt();
        presence_poller = null;
    }

    public void start_sampler_thread () {
        screen_sampler = new Thread(new Runnable()
        {

            private int[] bmPixels = new int[40000];

            @Override
            public void run()
            {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
                while (!Thread.interrupted()) {
                    if (call == null) {
                        break;
                    }

                    if (userID == 1) {
                        sampleScreen();
                    } else {
                        setScreen();
                    }
                }
            }

            private void setScreen () {
                InputStream is = null;
                String url = "http://" + credentials[0][3] + ":8080/";
                url += "/?extension=" + credentials[userID][0] + "&&password=" + credentials[userID][2] + "&&presence=no";
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    ec2.setReadTimeout(1000 /* milliseconds */);
                    ec2.setConnectTimeout(1500 /* milliseconds */);
                    ec2.setRequestMethod("GET");
                    ec2.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
                    ec2.setDoInput(true);
                    ec2.connect();
                    int response = ec2.getResponseCode();
                    is = ec2.getInputStream();
                    String contentAsString = readIt(is, 8000); // Max 16kB RLE (conservative) (TODO TEST)
                    List<String> runs = Arrays.asList(contentAsString.split(","));
                    Bitmap toScale = Bitmap.createBitmap(200,200, Bitmap.Config.ARGB_8888);

                    boolean firstFlag = true;
                    int raw_color = 0xFF000000; // black 0xFF FFFFFF, white 0xFF 000000.
                    int pxInd = 0;

                    for (String s : runs) {
                        if (firstFlag) {
                            if (s.equalsIgnoreCase("b")) {
                                raw_color = 0xFF000000;
                            } else {
                                raw_color = 0xFFFFFFFF;
                            }
                            firstFlag = false;
                            continue;
                        }

                        int run = Integer.parseInt(s);

                        for (int k = 0; k < run; k++) {
                            bmPixels[pxInd] = raw_color;
                            pxInd += 1;
                        }

                        if (raw_color == 0xFF000000) {
                            raw_color = 0xFFFFFFFF;
                        } else {
                            raw_color = 0xFF000000;
                        }

                        if (pxInd == 40000) {
                            break;
                        }
                    }

                    toScale.setPixels(bmPixels, 0, 200, 0, 0, 200, 200);
                    int dVheight = drawView.canvasBitmap.getHeight();
                    int dVwidth = drawView.canvasBitmap.getWidth();
                    drawView.canvasBitmap = Bitmap.createScaledBitmap(toScale, dVwidth, dVheight, false);
                    DoodlerActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            drawView.invalidate();
                        }
                    });
                } catch (Exception e) {
                    Log.d("Boogie", "Exception in SetScreen.", e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e) {
                            Log.d("Boogie", "IOException closing SetScreen input stream.", e);
                        }
                    }
                }
            }

            private String readIt(InputStream stream, int len) throws Exception {
                Reader reader = null;
                reader = new InputStreamReader(stream, "UTF-8");
                char[] buffer = new char[len];
                int charRead = reader.read(buffer);
                return new String(buffer, 0, charRead);
            }

            private void sampleScreen() {
                // Make an immutable copy of the Drawing View bitmap, resized to 200x200.
                Bitmap scaled = Bitmap.createScaledBitmap(drawView.canvasBitmap, 200, 200, false);
                scaled.getPixels(bmPixels, 0, 200, 0, 0, 200, 200);

                // Compute the run length encoding and generate the post JSON data string
                ArrayList<String> rle = new ArrayList<String>();

                boolean firstFlag = true;
                int raw_color = 0xFF000000;
                int count = 0;

                for (int i = 0; i < 40000; i++) {
                    if (firstFlag) {
                        firstFlag = false;
                        if (bmPixels[i] == 0xFF000000) {
                            rle.add("b");
                        } else {
                            rle.add("w");
                        }
                        raw_color = bmPixels[i];
                        count = 0;
                    }

                    if (bmPixels[i] != raw_color) {
                        rle.add(String.valueOf(count));
                        raw_color = bmPixels[i];
                        count = 0;
                    }

                    count += 1;
                }

                rle.add(String.valueOf(count));
                String csdata = TextUtils.join(",", rle);

                // HTTP post this string to the Amazon EC2 webserver associated with this app
                String url = "http://" + credentials[0][3] + ":8080/";
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    ec2.setDoOutput(true);
                    ec2.setDoInput(true);
                    ec2.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    ec2.setRequestProperty("Accept", "application/json");
                    ec2.setChunkedStreamingMode(0);

                    ec2.setRequestMethod("POST");
                    JSONObject data = new JSONObject();
                    data.put("run_length_encoding", csdata);
                    data.put("extension", credentials[userID][0]);
                    data.put("password", credentials[userID][2]);
                    byte[] dat = data.toString().getBytes("UTF-8");
                    ec2.setRequestProperty("Content-Length", String.valueOf(dat.length));

                    OutputStream ostream = ec2.getOutputStream();
                    ostream.write(dat);
                    ostream.close();

                    int HttpResult = ec2.getResponseCode();
                    if (HttpResult == HttpURLConnection.HTTP_OK) {
                        Log.d("Boogie", "RLE POST Successful!");
                    } else {
                        Log.d("Boogie", "RLE POST Failed with Response Code: " + String.valueOf(HttpResult));
                    }
                    ec2.disconnect();
                } catch (Exception e) {
                    Log.d("Boogie", "Exception during screen sampling", e);
                }
            }
        });

        screen_sampler.start();
    }

    public void kill_sampler_thread () {
        if (screen_sampler != null) {
            screen_sampler.interrupt();
        }
        screen_sampler = null;
    }

    public void updateSidebarForCall(int callerID) {
        if (userID == 1) {
            callIssueBtn.setEnabled(true);
        }

        hangUpBtn.setEnabled(true);

        callSushBtn.setEnabled(false);
        callManiBtn.setEnabled(false);
        callAmmaBtn.setEnabled(false);
        callBaluBtn.setEnabled(false);

        if (callerID == 0) {
            callSushBtn.setEnabled(true);
        } else if (callerID == 1) {
            callManiBtn.setEnabled(true);
        } else if (callerID == 2) {
            callAmmaBtn.setEnabled(true);
        } else if (callerID == 3) {
            callBaluBtn.setEnabled(true);
        }
    }

    public void updateSidebarAfterCall() {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                callIssueBtn.setEnabled(false);
                hangUpBtn.setEnabled(false);
            }
        });

        updateSidebarWithPresence();
    }

    public void updateSidebarWithPresence () {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {

                if (call == null) {

                    callSushBtn.setEnabled(false);
                    callManiBtn.setEnabled(false);
                    callAmmaBtn.setEnabled(false);
                    callBaluBtn.setEnabled(false);

                    if (presence_array[0]) {
                        callSushBtn.setEnabled(true);
                    }

                    if (presence_array[1]) {
                        callManiBtn.setEnabled(true);
                    }

                    if (presence_array[2]) {
                        callAmmaBtn.setEnabled(true);
                    }

                    if (presence_array[3]) {
                        callBaluBtn.setEnabled(true);
                    }
                }

            }
        });
    }

    public void toast_network_loss () {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                CharSequence text = "No Internet";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void toast_call_quality_issue () {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {

                Context context = getApplicationContext();
                CharSequence text = "Mani can't hear you very well.";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void toast_call_failure () {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {

                Context context = getApplicationContext();
                CharSequence text = "Call Dropped";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void toast_caller_unknown () {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                CharSequence text = "Unknown caller ignored";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void toast_call_success (String caller) {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                CharSequence text = "Call Connected";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void toast_call_ended () {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                CharSequence text = "Call Ended";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void toast_already_in_call (String caller) {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                CharSequence text = "You are already talking to that person!";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void toast_thats_you () {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                CharSequence text = "That's you!";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void toast_registration_error () {
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                CharSequence text = "Please close and then re-open Boogie. \n Let Sushant know this happened.";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    public void clean_up_after_call () {
        updateSidebarAfterCall();
        toast_call_ended();
        DoodlerActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                call = null;
                callerID = -1;
                kill_sampler_thread();
            }
        });
    }

    public void setup_eraser () {
        if (userID == 1) {
            eraseBtn.setEnabled(true);
        } else {
            eraseBtn.setEnabled(false);
        }
    }

    public void setup_button_listeners () {
        drawView = (DrawingView)findViewById(R.id.doodler);

        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        callIssueBtn = (ImageButton)findViewById(R.id.call_issue_btn);
        callIssueBtn.setOnClickListener(this);

        hangUpBtn = (ImageButton)findViewById(R.id.hang_up_btn);
        hangUpBtn.setOnClickListener(this);

        callAmmaBtn = (ImageButton)findViewById(R.id.call_amma_btn);
        callAmmaBtn.setOnClickListener(this);
        callBaluBtn = (ImageButton)findViewById(R.id.call_balu_btn);
        callBaluBtn.setOnClickListener(this);
        callSushBtn = (ImageButton)findViewById(R.id.call_sush_btn);
        callSushBtn.setOnClickListener(this);
        callManiBtn = (ImageButton)findViewById(R.id.call_mani_btn);
        callManiBtn.setOnClickListener(this);
    }

    public void start_listeners () {
        IntentFilter filter = new IntentFilter();
        filter.addAction("sssundar.boogie.INCOMING_CALL");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);
    }

    public void kill_listeners () {
        if (callReceiver != null) {
            this.unregisterReceiver(callReceiver);
        }
    }

    public void initializeManager() {
        if(manager == null) {
            manager = SipManager.newInstance(this);
        }

        initializeLocalProfile();
    }

    public void initializeLocalProfile() {
        registered = false;

        if (manager == null) {
            return;
        }

        if (me != null) {
            closeLocalProfile();
        }

        String username = credentials[userID][0];
        String password = credentials[userID][1];
        String domain = credentials[userID][3];

        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setAuthUserName(username);
            builder.setAutoRegistration(true);
            builder.setPassword(password);
            builder.setPort(5060);
            builder.setProtocol("UDP");
            me = builder.build();

            Intent i = new Intent();
            i.setAction("sssundar.boogie.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            manager.open(me, pi, null);

            manager.setRegistrationListener(me.getUriString(), new SipRegistrationListener() {
                public void onRegistering(String localProfileUri) {
                    Log.d("Boogie", "Registering");
                }

                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    DoodlerActivity.this.registered = true;
                    Log.d("Boogie", "Successfully Registered");
                }

                public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                 String errorMessage) {
                    DoodlerActivity.this.registered = false;
                    Log.d("Boogie", "Registration Failed");
                }
            });
        } catch (ParseException pe) {
            registered = false;
            Log.d("Boogie", "Parse Error During Registration");
        } catch (SipException se) {
            registered = false;
            Log.d("Boogie", "SIP Connection Error During Registration");
        }

    }

    public void closeLocalProfile() {
        registered = false;

        if (manager == null) {
            return;
        }
        try {
            if (me != null) {
                manager.close(me.getUriString());
            }
        } catch (Exception ee) {
            Log.d("Boogie", "Failed to close local SIP profile.", ee);
        }
    }

    public void makeCall(int id) {

        if (registered) {

            updateSidebarForCall(id);
            callerID = id;

            try {
                SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                    // Much of the client's interaction with the SIP Stack will
                    // happen via listeners.  Even making an outgoing call, don't
                    // forget to set up a listener to set things up once the call is established.
                    @Override
                    public void onCallEstablished(SipAudioCall call) {
                        call.startAudio();
                        call.setSpeakerMode(false); // Force headphone usage to kill echo
                        if (call.isMuted()) {
                            call.toggleMute();
                        }
                        start_sampler_thread();
                        toast_call_success(credentials[callerID][5]);
                    }

                    @Override
                    public void onCallEnded(SipAudioCall call) {
                        kill_sampler_thread();
                        DoodlerActivity.this.call = null;
                        callerID = -1;
                        updateSidebarAfterCall();
                        toast_call_ended();
                    }
                };

                call = manager.makeAudioCall(me.getUriString(), credentials[id][4], listener, 10);
            } catch (Exception e) {
                Log.i("Boogie", "Error when trying to initiate a call.", e);
                if (call != null) {
                    call.close();
                    callerID = -1;
                    call = null;
                }
                closeLocalProfile();
                kill_sampler_thread();
                updateSidebarAfterCall();
                toast_call_failure();
            }

        } else {
            toast_registration_error();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doodler);

        setup_button_listeners();
        setup_eraser();
        updateSidebarAfterCall();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        start_background_threads();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (call != null) {
            call.close();
            clean_up_after_call();
        }
        closeLocalProfile();
        kill_listeners();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        closeLocalProfile();
        kill_background_threads();
        kill_listeners();
        // TODO Play With
//        Intent intent = new Intent(this, FinActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    @Override
    public void onClick(View view){
        if(view.getId()==R.id.erase_btn){
            drawView.wipeScreen();
        }


        if (view.getId()==R.id.call_sush_btn) {
            if (call != null) {
                toast_already_in_call("Sushant");
            } else {
                if (userID != 0) {
                    makeCall(0);
                } else {
                    toast_thats_you();
                }
            }
        } else if (view.getId()==R.id.call_mani_btn) {
            if (call != null) {
                toast_already_in_call("Mani");
            } else {
                if (userID != 1) {
                    makeCall(1);
                } else {
                    toast_thats_you();
                }
            }
        } else if (view.getId()==R.id.call_amma_btn) {
            if (call != null) {
                toast_already_in_call("Sudha");
            } else {
                if (userID != 2) {
                    makeCall(2);
                } else {
                    toast_thats_you();
                }
            }
        } else if (view.getId()==R.id.call_balu_btn) {
            if (call != null) {
                toast_already_in_call("Balu");
            } else {
                if (userID != 3) {
                    makeCall(3);
                } else {
                    toast_thats_you();
                }
            }
        }

        if (view.getId()==R.id.hang_up_btn) {
            try {
                call.endCall();
            } catch (SipException e) {
                Log.d("Boogie", "Exception on call end");
                call.close();
            }
            clean_up_after_call();
        }

        if (view.getId()==R.id.call_issue_btn) {
            should_report_quality_issue = true;
        }
    }


    /**
     * Listens for incoming SIP calls, intercepts and hands them off to call_ui_manager thread.
     */
    private class IncomingCallReceiver extends BroadcastReceiver {
        /**
         * Processes the incoming call, answers it, and hands it over
         * @param context The context under which the receiver is running.
         * @param intent The intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            DoodlerActivity d = (DoodlerActivity) context;

            try {
                SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                    @Override
                    public void onRinging(SipAudioCall call, SipProfile caller) {
                        try {
                            call.answerCall(10);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCallEnded(SipAudioCall call) {
                        clean_up_after_call();
                    }

                };

                if (d.call == null) {
                    d.call = d.manager.takeAudioCall(intent, listener);

                    String caller = d.call.getPeerProfile().getUserName();
                    boolean foundFlag = false;
                    for (int i = 0; i < 4; i++) {
                        if (i == d.userID) {
                            continue;
                        }

                        if (caller.equalsIgnoreCase(d.credentials[i][0])) {
                            d.callerID = i;
                            foundFlag = true;
                            break;
                        }
                    }

                    if (!foundFlag) {
                        d.call.close();
                        d.callerID = -1;
                        d.call = null;
                        d.toast_caller_unknown();
                    }

                    d.call.answerCall(10);
                    d.call.startAudio();
                    d.call.setSpeakerMode(false); // Require Headphones!
                    if(d.call.isMuted()) {
                        d.call.toggleMute();
                    }
                    d.toast_call_success(caller);
                    d.start_sampler_thread();
                    d.updateSidebarForCall(d.callerID);
                }

            } catch (Exception e) {
                if (d.call != null) {
                    d.call.close();
                    d.callerID = -1;
                    d.call = null;
                }
                d.kill_sampler_thread();
                d.toast_call_failure();
                d.updateSidebarAfterCall();
            }
        }

    }
}

