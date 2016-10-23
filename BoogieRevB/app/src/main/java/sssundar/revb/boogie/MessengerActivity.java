package sssundar.revb.boogie;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class MessengerActivity extends Activity {

    public static final String backendServer = "ec2-54-183-116-184.us-west-1.compute.amazonaws.com";

    // Views & Buttons
    public DrawingView drawView;

    // Threads
    public Thread screen_sharer;

    public void getUIObjects () {
        drawView = (DrawingView) findViewById(R.id.doodler);
    }

    // Methods
    public void start_screen_share_thread () {
        screen_sharer = new Thread(new Runnable()
        {
            private long startSSMillis = System.currentTimeMillis();
            private ConnectivityManager cm = null;

            private void resetTimer() {
                this.startSSMillis = System.currentTimeMillis();
            }

            @Override
            public void run()
            {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
                cm = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                while (!Thread.interrupted()) {
                    if ((System.currentTimeMillis() - this.startSSMillis) > 200) {
                        resetTimer();

                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        boolean isConnected = (activeNetwork != null) && activeNetwork.isConnected() && (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);

                        if ((drawView.canvasBitmap != null) && isConnected) {
                            sampleScreen();
                        }
                    }
                }

                Log.d("BoogieThread", "Screen interrupted.");
            }

            private void sampleScreen() {
                int N = 512;
                int h = drawView.canvasBitmap.getHeight();
                int w = drawView.canvasBitmap.getWidth();
                int nh = (int) (h * ((N * 1.0) / w));
                Bitmap scaled = Bitmap.createScaledBitmap(drawView.canvasBitmap, N, nh, true);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] imageByte = stream.toByteArray();
                String url = "http://" + backendServer + ":9080/upload";
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    ec2.setRequestMethod("POST");
                    ec2.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    ec2.setDoOutput(true);
                    ec2.setDoInput(true);
                    DataOutputStream dos = new DataOutputStream(ec2.getOutputStream());
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"myFile\";filename=\""+ "screenshare.png" + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);
                    dos.write(imageByte,0,imageByte.length);
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    int serverResponseCode = ec2.getResponseCode();
                    String serverResponseMessage = ec2.getResponseMessage();
                    Log.i("Boogie", "HTTP Response is : "
                            + serverResponseMessage + ": " + serverResponseCode);
                    dos.flush();
                    dos.close();
                } catch (SocketTimeoutException e) {
                    Log.d("Boogie", "SocketTimeout", e);
                } catch (InterruptedIOException e) {
                    Log.d("Boogie", "InterruptedIOException during message upload to backend", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.d("Boogie", "Exception during message upload to backend", e);
                    e.printStackTrace();
                }
            }
        });

        screen_sharer.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_messenger);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getUIObjects();
        start_screen_share_thread();
    }

    private void stop_screen_share_thread() {
        while (!screen_sharer.isInterrupted()) {
            screen_sharer.interrupt();
        }
        screen_sharer = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop_screen_share_thread();
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
}
