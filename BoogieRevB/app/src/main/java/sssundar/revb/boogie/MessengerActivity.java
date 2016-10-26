package sssundar.revb.boogie;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sush on 10/9/16.
 */
public class MessengerActivity extends Activity implements View.OnTouchListener {

    // Credentials & Configuration
    // 0 = sushant, password0
    // 1 = sudha, password1
    // 2 = mani, password2
    // TODO Comments in spacer_medium - 35 for thatha, 2 for us.
    public static final int userID = 2;
    public static final String password = "password2";
    public static final String username = "mani";
    public static final String backendServer = "ec2-54-183-116-184.us-west-1.compute.amazonaws.com";
    public static final int N = 512;    // Screen Share Downsampled Pixel Width    

    // Views & Buttons
    public DrawingView drawView;
    public ImageButton highMessage_Amma, highMessage_Mani, highMessage_Sushant;
    public ImageButton highmidMessage_Amma, highmidMessage_Mani, highmidMessage_Sushant;
    public ImageButton midMessage_Amma, midMessage_Mani, midMessage_Sushant;
    public ImageButton lowmidMessage_Amma, lowmidMessage_Mani, lowmidMessage_Sushant;
    public ImageButton lowMessage_Amma, lowMessage_Mani, lowMessage_Sushant;
    public ImageButton presence_button;
    public ImageButton action_button;

    // Storage Paths
    public String dir_current_output = null;
    public String dir_queued_output = null;
    public String dir_ongoing_output = null;
    public String dir_messages = null;

    // Synchronous State
    public volatile boolean message_history_is_being_accessed = false;
    public volatile ArrayList<ArrayList<String>> message_history;
    public int unique_output_id;
    private boolean highMessagePressed;
    private boolean highmidMessagePressed;
    private boolean midMessagePressed;
    private boolean lowmidMessagePressed;
    private boolean lowMessagePressed;
    private boolean playbackStarted;
    private boolean highMessageGotDrawingLock;
    private boolean highmidMessageGotDrawingLock;
    private boolean midMessageGotDrawingLock;
    private boolean lowmidMessageGotDrawingLock;
    private boolean lowMessageGotDrawingLock;
    private boolean actionButtonGotDrawingLock;
    public volatile boolean drawing_view_bitmap_is_being_accessed = false;    

    // Audio Handling (Recording, Playback)
    private MediaRecorder myAudioRecorder = null;
    private MediaPlayer myMediaPlayer = null;
    private AudioManager myAudioManager = null;

    // Threads
    public Thread message_handler, screen_sharer;

    public void getUIObjects () {
        drawView = (DrawingView) findViewById(R.id.doodler);

        presence_button = (ImageButton) findViewById(R.id.presence_button);
        action_button = (ImageButton) findViewById(R.id.action_button);
        action_button.setOnTouchListener(this);

        highMessage_Amma = (ImageButton) findViewById(R.id.sudha_high);
        highmidMessage_Amma = (ImageButton) findViewById(R.id.sudha_highmid);
        midMessage_Amma = (ImageButton) findViewById(R.id.sudha_mid);
        lowmidMessage_Amma = (ImageButton) findViewById(R.id.sudha_lowmid);
        lowMessage_Amma = (ImageButton) findViewById(R.id.sudha_low);

        highMessage_Sushant = (ImageButton) findViewById(R.id.sushant_high);
        highmidMessage_Sushant = (ImageButton) findViewById(R.id.sushant_highmid);
        midMessage_Sushant = (ImageButton) findViewById(R.id.sushant_mid);
        lowmidMessage_Sushant = (ImageButton) findViewById(R.id.sushant_lowmid);
        lowMessage_Sushant = (ImageButton) findViewById(R.id.sushant_low);

        highMessage_Mani = (ImageButton) findViewById(R.id.mani_high);
        highmidMessage_Mani = (ImageButton) findViewById(R.id.mani_highmid);
        midMessage_Mani = (ImageButton) findViewById(R.id.mani_mid);
        lowmidMessage_Mani = (ImageButton) findViewById(R.id.mani_lowmid);
        lowMessage_Mani = (ImageButton) findViewById(R.id.mani_low);

        highMessage_Amma.setOnTouchListener(this);
        highmidMessage_Amma.setOnTouchListener(this);
        midMessage_Amma.setOnTouchListener(this);
        lowmidMessage_Amma.setOnTouchListener(this);
        lowMessage_Amma.setOnTouchListener(this);

        highMessage_Sushant.setOnTouchListener(this);
        highmidMessage_Sushant.setOnTouchListener(this);
        midMessage_Sushant.setOnTouchListener(this);
        lowmidMessage_Sushant.setOnTouchListener(this);
        lowMessage_Sushant.setOnTouchListener(this);

        highMessage_Mani.setOnTouchListener(this);
        highmidMessage_Mani.setOnTouchListener(this);
        midMessage_Mani.setOnTouchListener(this);
        lowmidMessage_Mani.setOnTouchListener(this);
        lowMessage_Mani.setOnTouchListener(this);
    }

    // Methods
    public void setPresenceState (int howMany) {
        switch (howMany) {
            case 0:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        presence_button.setSelected(false);
                        presence_button.setActivated(false);
                    }
                });
                break;
            case 1:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        presence_button.setSelected(false);
                        presence_button.setActivated(true);
                    }
                });
                break;
            case 2:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        presence_button.setSelected(true);
                        presence_button.setActivated(false);
                    }
                });
                break;
            case 3:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        presence_button.setSelected(true);
                        presence_button.setActivated(true);
                    }
                });
                break;
            default:
                break;
        }
    }

    public void set_action_state (boolean pressed) {
        action_button.setEnabled(true);
        if (!pressed) {
            action_button.setActivated(false);
        } else {
            action_button.setActivated(true);
        }
    }

    // 0 = sushant, unread
    // 1 = sushant, read
    // 2 = sudha, unread
    // 3 = sudha, read
    // 4 = mani, unread
    // 5 = mani, read
    // 6 = blank (all disabled)
    public void setLowMessage (int state) {
        switch (state) {
            case 0:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowMessage_Sushant.setEnabled(true);
                        lowMessage_Sushant.setActivated(true);

                        lowMessage_Amma.setEnabled(false);
                        lowMessage_Mani.setEnabled(false);

                        lowMessage_Sushant.setVisibility(View.VISIBLE);
                        lowMessage_Amma.setVisibility(View.INVISIBLE);
                        lowMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 1:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowMessage_Sushant.setEnabled(true);
                        lowMessage_Sushant.setActivated(false);

                        lowMessage_Amma.setEnabled(false);
                        lowMessage_Mani.setEnabled(false);

                        lowMessage_Sushant.setVisibility(View.VISIBLE);
                        lowMessage_Amma.setVisibility(View.INVISIBLE);
                        lowMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 2:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowMessage_Amma.setEnabled(true);
                        lowMessage_Amma.setActivated(true);

                        lowMessage_Sushant.setEnabled(false);
                        lowMessage_Mani.setEnabled(false);

                        lowMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowMessage_Amma.setVisibility(View.VISIBLE);
                        lowMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 3:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowMessage_Amma.setEnabled(true);
                        lowMessage_Amma.setActivated(false);

                        lowMessage_Sushant.setEnabled(false);
                        lowMessage_Mani.setEnabled(false);

                        lowMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowMessage_Amma.setVisibility(View.VISIBLE);
                        lowMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 4:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowMessage_Mani.setEnabled(true);
                        lowMessage_Mani.setActivated(true);

                        lowMessage_Sushant.setEnabled(false);
                        lowMessage_Amma.setEnabled(false);

                        lowMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowMessage_Amma.setVisibility(View.INVISIBLE);
                        lowMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 5:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowMessage_Mani.setEnabled(true);
                        lowMessage_Mani.setActivated(false);

                        lowMessage_Sushant.setEnabled(false);
                        lowMessage_Amma.setEnabled(false);

                        lowMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowMessage_Amma.setVisibility(View.INVISIBLE);
                        lowMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 6:
            default:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowMessage_Mani.setEnabled(false);
                        lowMessage_Amma.setEnabled(false);
                        lowMessage_Sushant.setEnabled(false);

                        lowMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowMessage_Amma.setVisibility(View.INVISIBLE);
                        lowMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
        }
    }
    public void setLowMidMessage (int state) {
        switch (state) {
            case 0:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowmidMessage_Sushant.setEnabled(true);
                        lowmidMessage_Sushant.setActivated(true);

                        lowmidMessage_Amma.setEnabled(false);
                        lowmidMessage_Mani.setEnabled(false);

                        lowmidMessage_Sushant.setVisibility(View.VISIBLE);
                        lowmidMessage_Amma.setVisibility(View.INVISIBLE);
                        lowmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 1:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowmidMessage_Sushant.setEnabled(true);
                        lowmidMessage_Sushant.setActivated(false);

                        lowmidMessage_Amma.setEnabled(false);
                        lowmidMessage_Mani.setEnabled(false);

                        lowmidMessage_Sushant.setVisibility(View.VISIBLE);
                        lowmidMessage_Amma.setVisibility(View.INVISIBLE);
                        lowmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 2:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowmidMessage_Amma.setEnabled(true);
                        lowmidMessage_Amma.setActivated(true);

                        lowmidMessage_Sushant.setEnabled(false);
                        lowmidMessage_Mani.setEnabled(false);

                        lowmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowmidMessage_Amma.setVisibility(View.VISIBLE);
                        lowmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 3:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowmidMessage_Amma.setEnabled(true);
                        lowmidMessage_Amma.setActivated(false);

                        lowmidMessage_Sushant.setEnabled(false);
                        lowmidMessage_Mani.setEnabled(false);

                        lowmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowmidMessage_Amma.setVisibility(View.VISIBLE);
                        lowmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 4:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowmidMessage_Mani.setEnabled(true);
                        lowmidMessage_Mani.setActivated(true);

                        lowmidMessage_Sushant.setEnabled(false);
                        lowmidMessage_Amma.setEnabled(false);

                        lowmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowmidMessage_Amma.setVisibility(View.INVISIBLE);
                        lowmidMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 5:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowmidMessage_Mani.setEnabled(true);
                        lowmidMessage_Mani.setActivated(false);

                        lowmidMessage_Sushant.setEnabled(false);
                        lowmidMessage_Amma.setEnabled(false);

                        lowmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowmidMessage_Amma.setVisibility(View.INVISIBLE);
                        lowmidMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 6:
            default:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        lowmidMessage_Mani.setEnabled(false);
                        lowmidMessage_Amma.setEnabled(false);
                        lowmidMessage_Sushant.setEnabled(false);

                        lowmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        lowmidMessage_Amma.setVisibility(View.INVISIBLE);
                        lowmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
        }
    }
    public void setMidMessage (int state) {
        switch (state) {
            case 0:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        midMessage_Sushant.setEnabled(true);
                        midMessage_Sushant.setActivated(true);

                        midMessage_Amma.setEnabled(false);
                        midMessage_Mani.setEnabled(false);

                        midMessage_Sushant.setVisibility(View.VISIBLE);
                        midMessage_Amma.setVisibility(View.INVISIBLE);
                        midMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 1:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        midMessage_Sushant.setEnabled(true);
                        midMessage_Sushant.setActivated(false);

                        midMessage_Amma.setEnabled(false);
                        midMessage_Mani.setEnabled(false);

                        midMessage_Sushant.setVisibility(View.VISIBLE);
                        midMessage_Amma.setVisibility(View.INVISIBLE);
                        midMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 2:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        midMessage_Amma.setEnabled(true);
                        midMessage_Amma.setActivated(true);

                        midMessage_Sushant.setEnabled(false);
                        midMessage_Mani.setEnabled(false);

                        midMessage_Sushant.setVisibility(View.INVISIBLE);
                        midMessage_Amma.setVisibility(View.VISIBLE);
                        midMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 3:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        midMessage_Amma.setEnabled(true);
                        midMessage_Amma.setActivated(false);

                        midMessage_Sushant.setEnabled(false);
                        midMessage_Mani.setEnabled(false);

                        midMessage_Sushant.setVisibility(View.INVISIBLE);
                        midMessage_Amma.setVisibility(View.VISIBLE);
                        midMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 4:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        midMessage_Mani.setEnabled(true);
                        midMessage_Mani.setActivated(true);

                        midMessage_Sushant.setEnabled(false);
                        midMessage_Amma.setEnabled(false);

                        midMessage_Sushant.setVisibility(View.INVISIBLE);
                        midMessage_Amma.setVisibility(View.INVISIBLE);
                        midMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 5:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        midMessage_Mani.setEnabled(true);
                        midMessage_Mani.setActivated(false);

                        midMessage_Sushant.setEnabled(false);
                        midMessage_Amma.setEnabled(false);

                        midMessage_Sushant.setVisibility(View.INVISIBLE);
                        midMessage_Amma.setVisibility(View.INVISIBLE);
                        midMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 6:
            default:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        midMessage_Mani.setEnabled(false);
                        midMessage_Amma.setEnabled(false);
                        midMessage_Sushant.setEnabled(false);

                        midMessage_Sushant.setVisibility(View.INVISIBLE);
                        midMessage_Amma.setVisibility(View.INVISIBLE);
                        midMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
        }
    }
    public void setHighMidMessage (int state) {
        switch (state) {
            case 0:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highmidMessage_Sushant.setEnabled(true);
                        highmidMessage_Sushant.setActivated(true);

                        highmidMessage_Amma.setEnabled(false);
                        highmidMessage_Mani.setEnabled(false);

                        highmidMessage_Sushant.setVisibility(View.VISIBLE);
                        highmidMessage_Amma.setVisibility(View.INVISIBLE);
                        highmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 1:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highmidMessage_Sushant.setEnabled(true);
                        highmidMessage_Sushant.setActivated(false);

                        highmidMessage_Amma.setEnabled(false);
                        highmidMessage_Mani.setEnabled(false);

                        highmidMessage_Sushant.setVisibility(View.VISIBLE);
                        highmidMessage_Amma.setVisibility(View.INVISIBLE);
                        highmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 2:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highmidMessage_Amma.setEnabled(true);
                        highmidMessage_Amma.setActivated(true);

                        highmidMessage_Sushant.setEnabled(false);
                        highmidMessage_Mani.setEnabled(false);

                        highmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        highmidMessage_Amma.setVisibility(View.VISIBLE);
                        highmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 3:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highmidMessage_Amma.setEnabled(true);
                        highmidMessage_Amma.setActivated(false);

                        highmidMessage_Sushant.setEnabled(false);
                        highmidMessage_Mani.setEnabled(false);

                        highmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        highmidMessage_Amma.setVisibility(View.VISIBLE);
                        highmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 4:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highmidMessage_Mani.setEnabled(true);
                        highmidMessage_Mani.setActivated(true);

                        highmidMessage_Sushant.setEnabled(false);
                        highmidMessage_Amma.setEnabled(false);

                        highmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        highmidMessage_Amma.setVisibility(View.INVISIBLE);
                        highmidMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 5:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highmidMessage_Mani.setEnabled(true);
                        highmidMessage_Mani.setActivated(false);

                        highmidMessage_Sushant.setEnabled(false);
                        highmidMessage_Amma.setEnabled(false);

                        highmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        highmidMessage_Amma.setVisibility(View.INVISIBLE);
                        highmidMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 6:
            default:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highmidMessage_Mani.setEnabled(false);
                        highmidMessage_Amma.setEnabled(false);
                        highmidMessage_Sushant.setEnabled(false);

                        highmidMessage_Sushant.setVisibility(View.INVISIBLE);
                        highmidMessage_Amma.setVisibility(View.INVISIBLE);
                        highmidMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
        }
    }
    public void setHighMessage (int state) {
        switch (state) {
            case 0:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highMessage_Sushant.setEnabled(true);
                        highMessage_Sushant.setActivated(true);

                        highMessage_Amma.setEnabled(false);
                        highMessage_Mani.setEnabled(false);

                        highMessage_Sushant.setVisibility(View.VISIBLE);
                        highMessage_Amma.setVisibility(View.INVISIBLE);
                        highMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 1:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highMessage_Sushant.setEnabled(true);
                        highMessage_Sushant.setActivated(false);

                        highMessage_Amma.setEnabled(false);
                        highMessage_Mani.setEnabled(false);

                        highMessage_Sushant.setVisibility(View.VISIBLE);
                        highMessage_Amma.setVisibility(View.INVISIBLE);
                        highMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 2:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highMessage_Amma.setEnabled(true);
                        highMessage_Amma.setActivated(true);

                        highMessage_Sushant.setEnabled(false);
                        highMessage_Mani.setEnabled(false);

                        highMessage_Sushant.setVisibility(View.INVISIBLE);
                        highMessage_Amma.setVisibility(View.VISIBLE);
                        highMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 3:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highMessage_Amma.setEnabled(true);
                        highMessage_Amma.setActivated(false);

                        highMessage_Sushant.setEnabled(false);
                        highMessage_Mani.setEnabled(false);

                        highMessage_Sushant.setVisibility(View.INVISIBLE);
                        highMessage_Amma.setVisibility(View.VISIBLE);
                        highMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
            case 4:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highMessage_Mani.setEnabled(true);
                        highMessage_Mani.setActivated(true);

                        highMessage_Sushant.setEnabled(false);
                        highMessage_Amma.setEnabled(false);

                        highMessage_Sushant.setVisibility(View.INVISIBLE);
                        highMessage_Amma.setVisibility(View.INVISIBLE);
                        highMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 5:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highMessage_Mani.setEnabled(true);
                        highMessage_Mani.setActivated(false);

                        highMessage_Sushant.setEnabled(false);
                        highMessage_Amma.setEnabled(false);

                        highMessage_Sushant.setVisibility(View.INVISIBLE);
                        highMessage_Amma.setVisibility(View.INVISIBLE);
                        highMessage_Mani.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case 6:
            default:
                MessengerActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        highMessage_Mani.setEnabled(false);
                        highMessage_Amma.setEnabled(false);
                        highMessage_Sushant.setEnabled(false);

                        highMessage_Sushant.setVisibility(View.INVISIBLE);
                        highMessage_Amma.setVisibility(View.INVISIBLE);
                        highMessage_Mani.setVisibility(View.INVISIBLE);
                    }
                });
                break;
        }
    }
    public void setMessage(int index, int state) {
        switch (index) {
            case 0:
                setHighMessage(state);
                break;
            case 1:
                setHighMidMessage(state);
                break;
            case 2:
                setMidMessage(state);
                break;
            case 3:
                setLowMidMessage(state);
                break;
            case 4:
                setLowMessage(state);
                break;
            default:
                break;
        }
    }

    // Once given access (true return value) you must reset the state yourself
    // using release_system_lock(); This lock is for message access.
    public synchronized boolean get_system_lock () {
        if (message_history_is_being_accessed) {
            return false;
        } else {
            message_history_is_being_accessed = true;
            return true;
        }
    }

    public void release_system_lock () {
        message_history_is_being_accessed = false;
    }

    // Once given access (true return value) you must reset the state yourself
    // using release_drawing_lock(); This lock is for screenshare access and
    // is only respected by MY threads. DrawingView will ignore it.
    public synchronized boolean get_drawing_lock () {
        if (drawing_view_bitmap_is_being_accessed) {
            return false;
        } else {
            drawing_view_bitmap_is_being_accessed = true;
            return true;
        }
    }

    public void release_drawing_lock () {
        drawing_view_bitmap_is_being_accessed = false;
    }

    public void disable_all_message_slots () {
        setLowMessage(6); // Blank
        setLowMidMessage(6); // Blank
        setMidMessage(6); // Blank
        setHighMidMessage(6); // Blank
        setHighMessage(6); // Blank
    }

    // Must be called before other threads are activated, from the UI thread
    // NOT thread safe.
    public void reset_ui_state () {
        drawView.allowDrawing(true); // Mani only; irrelevant for others.
        set_action_state(false);
        setPresenceState(0);
        disable_all_message_slots();
        message_history_is_being_accessed = false;
        drawing_view_bitmap_is_being_accessed = false;
        highMessagePressed = false;
        highmidMessagePressed = false;
        midMessagePressed = false;
        lowmidMessagePressed = false;
        lowMessagePressed = false;
        playbackStarted = false;
        highMessageGotDrawingLock = false;
        highmidMessageGotDrawingLock = false;
        midMessageGotDrawingLock = false;
        lowmidMessageGotDrawingLock = false;
        lowMessageGotDrawingLock = false;
        actionButtonGotDrawingLock = false;        
    }

    public void setup_app_containers() {
        String folder_messages = "Boogie_RevB/messages";
        String folder_current_output = "Boogie_RevB/current_output";
        String folder_queued_output = "Boogie_RevB/queued_output";
        String folder_ongoing_output = "Boogie_RevB/ongoing_output";

        File f;

        f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_messages);
        if (!f.exists()) { f.mkdirs(); }
        f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_current_output);
        if (!f.exists()) { f.mkdirs(); }
        f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_queued_output);
        if (!f.exists()) { f.mkdirs(); }
        f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_ongoing_output);
        if (!f.exists()) { f.mkdirs(); }

        dir_messages = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + folder_messages + "/";
        dir_current_output = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + folder_current_output + "/";
        dir_queued_output = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + folder_queued_output + "/";
        dir_ongoing_output = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + folder_ongoing_output + "/";
            
        for (File c : new File(dir_messages).listFiles()) { if (!c.isDirectory()) { c.delete(); } }
        for (File c : new File(dir_current_output).listFiles()) { if (!c.isDirectory()) { c.delete(); } }
        for (File c : new File(dir_queued_output).listFiles()) { if (!c.isDirectory()) { c.delete(); } }
        for (File c : new File(dir_ongoing_output).listFiles()) { if (!c.isDirectory()) { c.delete(); } }
    }

    // Must be locked before calling
    public void trim_messages () {
        while (message_history.size() > 5) {            
            new File(message_history.get(0).get(4)).delete();
            message_history.remove(0);            
        }
    }

    // Must be locked before calling
    public void update_sidebar () {
        // Assign low, lowmid, mid, highmid, and high
        // to indices 4, 3, 2, 1, 0 (newer to oldest)
        // and set the ImageButton settings according
        // to the source user + read state
        for (int i = 0; i < Math.min(message_history.size(),5); i++) {
            String uname = message_history.get(i).get(2);
            if (message_history.get(i).get(1).equals("read")) {
                if (uname.equals("sushant")) {
                    setMessage(i, 1);
                } else if (uname.equals("sudha")) {
                    setMessage(i, 3);
                } else if (uname.equals("mani")) {
                    setMessage(i, 5);
                }
            } else {
                if (uname.equals("sushant")) {
                    setMessage(i, 0);
                } else if (uname.equals("sudha")) {
                    setMessage(i, 2);
                } else if (uname.equals("mani")) {
                    setMessage(i, 4);
                }
            }
        }
        for (int i = Math.min(message_history.size(), 5); i < 5; i++) {
            setMessage(i,6);
        }
    }

    // Build message history based on files in the dir_messages folder
    public boolean update_message_history () {
        if (!get_system_lock()) {
            return false;
        }

        message_history = new ArrayList<ArrayList<String>>();
        for (File c: new File(dir_messages).listFiles()) {
            if (!c.isDirectory()) {
                message_history.add(new ArrayList<String>());
                ArrayList<String> meta_message = message_history.get(message_history.size() - 1);
                String tokens[] = c.getName().split("_");  // MSSERVERTIMESTAMP_READ/UNREAD_SOURCEUSER.EXTENSION
                meta_message.add(tokens[0].toLowerCase());               // 0 millisecond timestamp from server
                meta_message.add(tokens[1].toLowerCase()); // 1 read, unread
                String userext[] = tokens[2].split("\\.");
                meta_message.add(userext[0].toLowerCase()); // 2 sushant, sudha, mani
                meta_message.add(userext[1].toLowerCase()); // 3 3gp or png
                meta_message.add(c.getAbsolutePath());     // 4 file path (for purging)
            }
        }

        // Sort on integer ms timestamp, with lowest time = furthest back = lowest index
        Collections.sort(message_history, new Comparator<List<String>>() {
            @Override
            public int compare(List<String> o1, List<String> o2) {
                return new Long(o1.get(0)).compareTo(new Long(o2.get(0)));
            }
        });

        trim_messages();
        update_sidebar();

        release_system_lock();
        return true;
    }

    private void test_update_message_history (String filename) {
        FileOutputStream os;
        try {
            os = new FileOutputStream(new File(dir_messages + filename));
            os.write("Hi there!".getBytes());
            os.close();
        } catch (IOException e) {
            Log.d("Boogie", "File write failed: " + e.toString());
        }
    }

    // On startup, scan pending output folder (uniquemonotoneid_me.ext) and get the ids
    // and keep the largest one + 1 in a variable
    public void get_unique_pending_output_id () {
        unique_output_id = 0;
        for (File c: new File(dir_queued_output).listFiles()) {
            if (!c.isDirectory()) {
                String tokens[] = c.getName().split("_");  // UNIQUEID_ME.EXT
                int contender = new Integer(tokens[0]).intValue();
                Log.d("Boogie", "Found queued output " + c.getName());
                if (contender > unique_output_id) {
                    unique_output_id = contender;
                }
            }
        }
        unique_output_id += 1;
    }

    public void start_audio_handlers () {
        myAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        myAudioRecorder = new MediaRecorder();
        myMediaPlayer = new MediaPlayer();
    }

    public void start_message_handler_thread () {
        message_handler = new Thread(new Runnable()
        {
            private long startMillis = System.currentTimeMillis();
            private ConnectivityManager cm = null;

            private void resetTimer() {
                this.startMillis = System.currentTimeMillis();
            }

            @Override
            public void run()
            {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND );
                cm = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                while (!Thread.interrupted()) {
                    if ((System.currentTimeMillis() - this.startMillis) > 200) {
                        resetTimer();
                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        boolean isConnected = (activeNetwork != null) && activeNetwork.isConnected() && (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
                        if (isConnected) {
                            processQueuedOutput();
                            processOutput();
                            getPresence();
                            getNextMessage();
                            update_message_history();
                        } else {
                            setPresenceState(0);
                        }
                    }
                }

                Log.d("BoogieThread", "Message interrupted.");
            }

            private void processQueuedOutput () {
                // Scan dir_current_output to see if it is empty
                File dir = new File(dir_current_output);
                File[] files = dir.listFiles();
                // No internal directories expected & no self/parent directories
                if ((files != null) && (files.length > 0)) {
                    return;
                }

                // Scan dir_queued_output and find the lowest unique_id
                File min_queued_id = null;
                int min_id = unique_output_id; // Max possible (with a slight irrelevant race condition)
                String min_rest_of_filename = null;
                for (File c: new File(dir_queued_output).listFiles()) {
                    if (!c.isDirectory()) {
                        String tokens[] = c.getName().split("_");  // UNIQUEID_USERNAME.EXT
                        int contender = new Integer(tokens[0]).intValue();
                        if (contender < min_id) {
                            min_id = contender;
                            min_rest_of_filename = tokens[1];
                            min_queued_id = c;
                        }
                    }
                }

                if (min_queued_id != null) {
                    // Log the filename for testing (with and without wifi enabled to test queuing
                    Log.d("Boogie", "About to send message: " + min_queued_id.getName() + " as " + password + "_" + min_rest_of_filename + " to output directory.");

                    // Rename the file to password_username.ext in dir_current_output
                    min_queued_id.renameTo(new File(dir_current_output + password + "_" + min_rest_of_filename));
                }
            }

            private void processOutput() {
                File dir = new File(dir_current_output);
                File[] files = dir.listFiles();
                // No internal directories
                if ((files == null) || (files.length == 0)) {
                    return;
                }

                String filename = files[0].getName();

                String url = "http://" + backendServer + ":8080/upload";

                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";

                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1024 * 1024;

                try {
                    URL server = new URL(url);
                    FileInputStream fileInputStream = new FileInputStream(files[0]);
                    DataOutputStream dos = null;
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    ec2.setRequestMethod("POST");
                    ec2.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    ec2.setDoOutput(true);
                    ec2.setDoInput(true);

                    dos = new DataOutputStream(ec2.getOutputStream());
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"myFile\";filename=\""+ filename + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);

                    // create a buffer of  maximum size
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    // send multipart form data necesssary after file data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    // Responses from the server (code and message)
                    int serverResponseCode = ec2.getResponseCode();
                    String serverResponseMessage = ec2.getResponseMessage();

                    Log.i("Boogie", "HTTP Response is : "
                            + serverResponseMessage + ": " + serverResponseCode);

                    //close the streams //
                    fileInputStream.close();
                    dos.flush();
                    dos.close();

                    if (serverResponseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("Boogie", "Deleting file " + files[0].getName() + " from output directory.");
                        files[0].delete();
                    }
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

            private String readStream(InputStream in) {
                BufferedReader reader = null;
                StringBuffer response = new StringBuffer();
                try {
                    reader = new BufferedReader(new InputStreamReader(in));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                } catch (SocketTimeoutException e) {
                    Log.d("Boogie", "SocketTimeout", e);
                } catch (InterruptedIOException e) {
                    Log.d("Boogie", "InterruptedIOException during readStream in Messenger", e);
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return response.toString();
            }

            private void getPresence() {
                String url = "http://" + backendServer + ":8080/presence/?user=" + username + "&password=" + password;
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    String server_response = null;
                    if (ec2.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        server_response = readStream(ec2.getInputStream());
                        if (server_response.equals("0")) {
                            setPresenceState(0);
                        } else if (server_response.equals("1")) {
                            setPresenceState(1);
                        } else if (server_response.equals("2")) {
                            setPresenceState(2);
                        } else if (server_response.equals("3")) {
                            setPresenceState(3);
                        }
                        return;
                    }
                    setPresenceState(0);
                    return;
                } catch (SocketTimeoutException e) {
                    Log.d("Boogie", "SocketTimeout", e);
                } catch (InterruptedIOException e) {
                    Log.d("Boogie", "InterruptedIOException during Presence in Messenger", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    setPresenceState(0);
                    Log.d("Boogie", "Exception during presence polling", e);
                }
            }


            private void copyInputStreamToFile( InputStream in, File file ) {
                try {
                    OutputStream out = new FileOutputStream(file);
                    byte[] buf = new byte[1024];
                    int len;
                    while((len=in.read(buf))>0){
                        out.write(buf,0,len);
                    }
                    out.close();
                    in.close();
                } catch (SocketTimeoutException e) {
                    Log.d("Boogie", "SocketTimeout", e);
                } catch (InterruptedIOException e) {
                    Log.d("Boogie", "InterruptedIOException during copyInputStreamToFile in Messenger", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void getRemoteFile (String remoteName) {
                String url = "http://" + backendServer + ":8080/download/?user=" + username + "&password=" + password + "&fname=" + remoteName;
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    ec2.setRequestMethod("GET");
                    ec2.setDoInput(true);
                    ec2.setDoOutput(true);
                    ec2.connect();
                    if (ec2.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        String tokens[] = remoteName.split("_");
                        if (tokens[1].split("\\.")[0].equals(username)) {
                            copyInputStreamToFile(ec2.getInputStream(), new File(dir_messages + tokens[0] + "_read_" + tokens[1]));
                        } else {
                            copyInputStreamToFile(ec2.getInputStream(), new File(dir_messages + tokens[0] + "_unread_" + tokens[1]));
                        }
                        Log.d("Boogie", "Brought in " + remoteName);
                    }
                } catch (SocketTimeoutException e) {
                    Log.d("Boogie", "SocketTimeout", e);
                } catch (InterruptedIOException e) {
                    Log.d("Boogie", "InterruptedIOException during getRemoteFile in Messenger", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.d("Boogie", "Exception during mail retrieval", e);
                }
            }

            private void getNextMessage () {
                String url = "http://" + backendServer + ":8080/check_mailbox/?user=" + username + "&password=" + password;
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    String server_response = null;
                    if (ec2.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        server_response = readStream(ec2.getInputStream());
                        if (!server_response.equals("empty")) {
                            getRemoteFile(server_response);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    Log.d("Boogie", "SocketTimeout", e);
                } catch (InterruptedIOException e) {
                    Log.d("Boogie", "InterruptedIOException during getNextMessage in Messenger", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.d("Boogie", "Exception during mailbox check", e);
                }
            }
        });

        message_handler.start();
    }

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
                            if (userID == 2) {
                                sampleScreen();
                            } else {
                                setScreen();
                            }
                        }
                    }
                }

                Log.d("BoogieThread", "Screen interrupted.");
            }

            private void setScreen () {
                String url = "http://" + backendServer + ":8080/grabscreen/?user=" + username + "&password=" + password;
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    ec2.setReadTimeout(1000 /* milliseconds */);
                    ec2.setConnectTimeout(1500 /* milliseconds */);
                    ec2.setRequestMethod("GET");                    
                    ec2.setDoInput(true);
                    ec2.connect();
                    if (ec2.getResponseCode() == HttpURLConnection.HTTP_OK) {                                                                                            
                        int dVheight = drawView.canvasBitmap.getHeight();
                        int dVwidth = drawView.canvasBitmap.getWidth();
                        Bitmap newCanvasBM = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(ec2.getInputStream()), dVwidth, dVheight, true);
                        if (get_drawing_lock()) {
                            drawView.setCanvasBitmap(newCanvasBM);
                            release_drawing_lock();
                            MessengerActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    drawView.invalidate();
                                }
                            });
                        }
                    }
                } catch (SocketTimeoutException e) {
                    Log.d("Boogie", "SocketTimeout", e);
                } catch (InterruptedIOException e) {
                    Log.d("Boogie", "InterruptedIOException during setScreen in Screen", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.d("Boogie", "Exception in SetScreen.", e);
                }
            }

            private void sampleScreen() {
                // Make an immutable copy of the Drawing View bitmap, resized to NxN.
                int h = drawView.canvasBitmap.getHeight();
                int w = drawView.canvasBitmap.getWidth();
                int nh = (int) (h * ((N * 1.0) / w));
                if (get_drawing_lock()) {
                    Bitmap scaled = Bitmap.createScaledBitmap(drawView.canvasBitmap, N, nh, true);
                    release_drawing_lock();
                    String url = "http://" + backendServer + ":8080/sharescreen";
                    String lineEnd = "\r\n";
                    String twoHyphens = "--";
                    String boundary = "*****";
                    try {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        scaled.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] imageByte = stream.toByteArray();
                        stream.close();
                        URL server = new URL(url);
                        HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                        ec2.setRequestMethod("POST");
                        ec2.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                        ec2.setDoOutput(true);
                        ec2.setDoInput(true);
                        DataOutputStream dos = new DataOutputStream(ec2.getOutputStream());
                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"myFile\";filename=\""+ password + "_" + username + ".png" + "\"" + lineEnd);
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
                        Log.d("Boogie", "InterruptedIOException during screenshare to backend", e);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        Log.d("Boogie", "Exception during screenshare to backend", e);                        
                    }
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
        Log.d("BoogieThread", "Resuming");
        getUIObjects();
        reset_ui_state();
        setup_app_containers();

        update_message_history();
        get_unique_pending_output_id();
        start_audio_handlers();

        start_message_handler_thread();
        start_screen_share_thread();
    }

    private void stop_audio_handlers() {
        myAudioRecorder.reset();
        myAudioRecorder.release();
        myMediaPlayer.reset();
        myMediaPlayer.release();
        myAudioRecorder = null;
        myMediaPlayer = null;
        myAudioManager = null;
    }

    private void stop_message_handler_thread() {
        while (!message_handler.isInterrupted()) {
            message_handler.interrupt();
        }
        message_handler = null;
    }

    private void stop_screen_share_thread() {
        while (!screen_sharer.isInterrupted()) {
            screen_sharer.interrupt();
        }
        screen_sharer = null;
    }

    @Override
    protected void onPause() {
        Log.d("BoogieThread", "Pausing");
        super.onPause();
        stop_audio_handlers();
        stop_message_handler_thread();
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

    // Must hold display lock before calling this function
    public void displayScreenMessage (String sharedScreen) {
        try {
            int dVheight = drawView.canvasBitmap.getHeight();
            int dVwidth = drawView.canvasBitmap.getWidth();
            Bitmap newCanvasBM = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(sharedScreen), dVwidth, dVheight, true);            
            drawView.setCanvasBitmap(newCanvasBM);                                
            drawView.invalidate();            
        } catch (Exception e) {
            Log.d("Boogie", "Error in screen playback decoding, " + e.toString());
        }
    }

    // Must hold system lock to call this function
    public void showMessage (int index) {
        if ((index + 1) <= message_history.size()) {
            String msgpath = message_history.get(index).get(4);
            String extension = message_history.get(index).get(3);
            if (extension.equals("3gp")) {
                myAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, myAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                try {
                    myMediaPlayer.setDataSource(msgpath);
                    myMediaPlayer.prepare();
                    myMediaPlayer.start();
                    playbackStarted = true;
                } catch (IOException e) {
                    Log.d("Boogie", "Playback failure");
                    e.printStackTrace();
                    playbackStarted = false;
                }
            } else if (extension.equals("png")) {
                // We already know only one button got here, because of the system lock.
                // Here, if we're Mani, drawing, we are competing with a slow POST request that
                // doesn't lock the system very often for drawing. For Amma and Sushant, we are
                // competing with a drawing lock by the screen setter that happens VERY fast,
                // so we often see lockouts. To this end, we introduce a while loop, and we have
                // tested that press-releases happen in order (i.e. all in same thread).
                boolean lock_flag = false;
                while (!lock_flag) {
                    lock_flag = get_drawing_lock();
                }

                switch (index) {
                    case 0:
                        highMessageGotDrawingLock = lock_flag;
                        break;
                    case 1:
                        highmidMessageGotDrawingLock = lock_flag;
                        break;
                    case 2:
                        midMessageGotDrawingLock = lock_flag;
                        break;
                    case 3:
                        lowmidMessageGotDrawingLock = lock_flag;
                        break;
                    case 4:
                        lowMessageGotDrawingLock = lock_flag;
                        break;
                    default:
                        Log.d("Boogie", "Invalid index at screen share message display handler.");
                        release_drawing_lock();
                        return;
                }
                drawView.allowDrawing(false);
                displayScreenMessage(msgpath);
            }
        }
    }

    // Must hold system lock to call this function
    public void cleanupMessage (int index) {
        if ((index + 1) <= message_history.size()) {
            String timestamp = message_history.get(index).get(0);
            String readstate = message_history.get(index).get(1);
            String source = message_history.get(index).get(2);
            String extension = message_history.get(index).get(3);
            String msgpath = message_history.get(index).get(4);
            if (extension.equals("3gp")) {
                if (playbackStarted) {
                    try {
                        myMediaPlayer.stop();
                    } catch (RuntimeException e) {
                        Log.d("Boogie", "RuntimeException at playback stop.");
                        e.printStackTrace();
                    }
                }
                myMediaPlayer.reset();
                playbackStarted = false;
            } else if (extension.equals("png")) {
                Log.d("Boogie", "Got to cleanup already!");
                boolean gotDrawingLock = false;
                switch (index) {
                    case 0:
                        gotDrawingLock = highMessageGotDrawingLock;
                        highMessageGotDrawingLock = false;
                        break;
                    case 1:
                        gotDrawingLock = highmidMessageGotDrawingLock;
                        highmidMessageGotDrawingLock = false;
                        break;
                    case 2:
                        gotDrawingLock = midMessageGotDrawingLock;
                        midMessageGotDrawingLock = false;
                        break;
                    case 3:
                        gotDrawingLock = lowmidMessageGotDrawingLock;
                        lowmidMessageGotDrawingLock = false;
                        break;
                    case 4:
                        gotDrawingLock = lowMessageGotDrawingLock;
                        lowMessageGotDrawingLock = false;
                        break;
                    default:
                        return;
                }
                if (gotDrawingLock) {
                    drawView.allowDrawing(true);
                    drawView.wipeScreen();
                    release_drawing_lock();
                }
            }

            if (readstate.equals("unread")) {
                new File(msgpath).renameTo(new File(dir_messages + timestamp + "_read_" + source + "." + extension));
            }
        }
    }

    // You must have the drawing lock to call this
    public void encodeScreenMessage () {                
        int h = drawView.canvasBitmap.getHeight();
        int w = drawView.canvasBitmap.getWidth();
        int nh = (int) (h * ((N * 1.0) / w));        
        Bitmap scaled = Bitmap.createScaledBitmap(drawView.canvasBitmap, N, nh, true);                   
        drawView.wipeScreen();         
        release_drawing_lock();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] imageByte = stream.toByteArray();
            stream.close();
            FileOutputStream fstream = new FileOutputStream(new File(dir_ongoing_output + username + ".png"));
            fstream.write(imageByte);
            fstream.close();        
            File source = new File(dir_ongoing_output + username + ".png");
            File dest = new File(dir_queued_output + unique_output_id + "_" + username + ".png");
            source.renameTo(dest);
            unique_output_id += 1;
        } catch (Exception e) {
            Log.d("Boogie", "Unable to write screenshare message to queued output, " + e.toString());
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event){
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // BUTTON PRESSED
                action_button.setClickable(false);
                if (view.getId()==R.id.action_button) {
                    set_action_state(true);
                    if (userID != 2) {
                        // Starting recording audio to temporary folder dir_ongoing_output
                        try {
                            String currentRecordingName = username + ".3gp";
                            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                            myAudioRecorder.setOutputFile(dir_ongoing_output + currentRecordingName);
                            myAudioRecorder.prepare();
                            myAudioRecorder.start();
                        } catch (IllegalStateException e) {
                            Log.d("Boogie", "IllegalStateException in Audio Recording.");
                            e.printStackTrace();
                        } catch (IOException e) {
                            Log.d("Boogie", "IOException in Audio Recording.");
                            e.printStackTrace();
                        }
                    } else {
                        actionButtonGotDrawingLock = get_drawing_lock();
                        if (actionButtonGotDrawingLock) {
                            encodeScreenMessage();
                        }
                    }
                } else {
                    switch (view.getId()) {
                        case R.id.sudha_high:
                        case R.id.sushant_high:
                        case R.id.mani_high:
                            Log.d("Boogie", "High Pressed");
                            highMessagePressed = get_system_lock();
                            if (highMessagePressed) {
                                showMessage(0);
                            }
                            break;
                        case R.id.sudha_highmid:
                        case R.id.sushant_highmid:
                        case R.id.mani_highmid:
                            Log.d("Boogie", "High Mid Pressed");
                            highmidMessagePressed = get_system_lock();
                            if (highmidMessagePressed) {
                                showMessage(1);
                            }
                            break;
                        case R.id.sudha_mid:
                        case R.id.sushant_mid:
                        case R.id.mani_mid:
                            Log.d("Boogie", "Mid Pressed");
                            midMessagePressed = get_system_lock();
                            if (midMessagePressed) {
                                showMessage(2);
                            }
                            break;
                        case R.id.sudha_lowmid:
                        case R.id.sushant_lowmid:
                        case R.id.mani_lowmid:
                            Log.d("Boogie", "Low Mid Pressed");
                            lowmidMessagePressed = get_system_lock();
                            if (lowmidMessagePressed) {
                                showMessage(3);
                            }
                            break;
                        case R.id.sudha_low:
                        case R.id.sushant_low:
                        case R.id.mani_low:
                            Log.d("Boogie", "Low Pressed");
                            lowMessagePressed = get_system_lock();
                            if (lowMessagePressed) {
                                showMessage(4);
                            }
                            break;
                        default:
                            break;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                // BUTTON RELEASED
                if (view.getId()==R.id.action_button) {
                    if (userID != 2) {
                        try {
                            myAudioRecorder.stop();
                            File source = new File(dir_ongoing_output + username + ".3gp");
                            File dest = new File(dir_queued_output + unique_output_id + "_" + username + ".3gp");
                            source.renameTo(dest);
                            unique_output_id += 1;
                        } catch (RuntimeException e) {
                            Log.d("Boogie", "RuntimeException due to lack of audio in rapid click.");
                        }
                        myAudioRecorder.reset();
                    }
                    set_action_state(false);
                } else {
                    switch (view.getId()) {
                        case R.id.sudha_high:
                        case R.id.sushant_high:
                        case R.id.mani_high:
                            if (highMessagePressed) {
                                cleanupMessage(0);
                                release_system_lock();
                                highMessagePressed = false;
                            }
                            break;
                        case R.id.sudha_highmid:
                        case R.id.sushant_highmid:
                        case R.id.mani_highmid:
                            if (highmidMessagePressed) {
                                cleanupMessage(1);
                                release_system_lock();
                                highmidMessagePressed = false;
                            }
                            break;
                        case R.id.sudha_mid:
                        case R.id.sushant_mid:
                        case R.id.mani_mid:
                            if (midMessagePressed) {
                                cleanupMessage(2);
                                release_system_lock();
                                midMessagePressed = false;
                            }
                            break;
                        case R.id.sudha_lowmid:
                        case R.id.sushant_lowmid:
                        case R.id.mani_lowmid:
                            if (lowmidMessagePressed) {
                                cleanupMessage(3);
                                release_system_lock();
                                lowmidMessagePressed = false;
                            }
                            break;
                        case R.id.sudha_low:
                        case R.id.sushant_low:
                        case R.id.mani_low:
                            if (lowMessagePressed) {
                                cleanupMessage(4);
                                release_system_lock();
                                lowMessagePressed = false;
                            }
                            break;
                        default:
                            break;
                    }
                }
                action_button.setClickable(true);
                return true;
        }
        return false;
    }
}
