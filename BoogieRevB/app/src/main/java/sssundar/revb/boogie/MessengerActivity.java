package sssundar.revb.boogie;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sush on 10/9/16.
 */
public class MessengerActivity extends Activity implements View.OnTouchListener {

    // Credentials
    // 0 = sushant, password0
    // 1 = sudha, password1
    // 2 = mani, password2
    public static int userID = 2;
    public static String password = "password2";
    public static String username = "mani";

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

    public void getUIObjects () {
        drawView = (DrawingView) findViewById(R.id.doodler);

        presence_button = (ImageButton) findViewById(R.id.presence_button);
        action_button = (ImageButton) findViewById(R.id.action_button);
        action_button.setOnTouchListener(this);

        highMessage_Amma = (ImageButton) findViewById(R.id.sudha_high);
        highMessage_Amma.setOnTouchListener(this);
        highmidMessage_Amma = (ImageButton) findViewById(R.id.sudha_highmid);
        highmidMessage_Amma.setOnTouchListener(this);
        midMessage_Amma = (ImageButton) findViewById(R.id.sudha_mid);
        midMessage_Amma.setOnTouchListener(this);
        lowmidMessage_Amma = (ImageButton) findViewById(R.id.sudha_lowmid);
        lowmidMessage_Amma.setOnTouchListener(this);
        lowMessage_Amma = (ImageButton) findViewById(R.id.sudha_low);
        lowMessage_Amma.setOnTouchListener(this);

        highMessage_Sushant = (ImageButton) findViewById(R.id.sushant_high);
        highMessage_Sushant.setOnTouchListener(this);
        highmidMessage_Sushant = (ImageButton) findViewById(R.id.sushant_highmid);
        highmidMessage_Sushant.setOnTouchListener(this);
        midMessage_Sushant = (ImageButton) findViewById(R.id.sushant_mid);
        midMessage_Sushant.setOnTouchListener(this);
        lowmidMessage_Sushant = (ImageButton) findViewById(R.id.sushant_lowmid);
        lowmidMessage_Sushant.setOnTouchListener(this);
        lowMessage_Sushant = (ImageButton) findViewById(R.id.sushant_low);
        lowMessage_Sushant.setOnTouchListener(this);

        highMessage_Mani = (ImageButton) findViewById(R.id.mani_high);
        highMessage_Mani.setOnTouchListener(this);
        highmidMessage_Mani = (ImageButton) findViewById(R.id.mani_highmid);
        highmidMessage_Mani.setOnTouchListener(this);
        midMessage_Mani = (ImageButton) findViewById(R.id.mani_mid);
        midMessage_Mani.setOnTouchListener(this);
        lowmidMessage_Mani = (ImageButton) findViewById(R.id.mani_lowmid);
        lowmidMessage_Mani.setOnTouchListener(this);
        lowMessage_Mani = (ImageButton) findViewById(R.id.mani_low);
        lowMessage_Mani.setOnTouchListener(this);
    }

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
    // using release_system_lock();
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

    public void disable_all_message_slots () {
        setLowMessage(6); // Blank
        setLowMidMessage(6); // Blank
        setMidMessage(6); // Blank
        setHighMidMessage(6); // Blank
        setHighMessage(6); // Blank
    }

    // Must be called before other threads are activated, from the UI thread
    public void reset_ui_state () {
        drawView.allowDrawing(true); // Mani only; irrelevant for others.
        set_action_state(false);
        setPresenceState(0);
        disable_all_message_slots();
        message_history_is_being_accessed = false;
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

        // Clean up partial files from any aborted recordings
        for (File c : new File(dir_ongoing_output).listFiles()) {
            if (!c.isDirectory()) {
                c.delete();
            }
        }
    }

    // Must be locked before calling
    public void trim_messages () {
        while (message_history.size() > 5) {
            if (message_history.get(0).get(1).equals("unread")) {
                return;
            } else {
                new File(message_history.get(0).get(4)).delete();
                message_history.remove(0);
            }
        }
    }

    // Must be locked before calling
    public void update_sidebar () {
        // Assign low, lowmid, mid, highmid, and high
        // to indices 4, 3, 2, 1, 0 (newer to oldest)
        // and set the ImageButton settings according
        // to the source user + read state
        disable_all_message_slots();
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
                meta_message.add(userext[1].toLowerCase()); // 3 3gp or txt
                meta_message.add(c.getAbsolutePath());     // 4 file path (for purging)
            }
        }

        // Sort on integer ms timestamp, with lowest time = furthest back = lowest index
        Collections.sort(message_history, new Comparator<List<String>>() {
            @Override
            public int compare(List<String> o1, List<String> o2) {
                return new Integer(o1.get(0)).compareTo(new Integer(o2.get(0)));
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_messenger);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getUIObjects();
        reset_ui_state();
        setup_app_containers();

        test_update_message_history("0_unread_sushant.3gp");
        test_update_message_history("1_read_sushant.3gp");
        test_update_message_history("2_unread_sudha.3gp");
        test_update_message_history("3_unread_mani.3gp");
        test_update_message_history("4_read_sushant.3gp");

        update_message_history();
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
    public boolean onTouch(View view, MotionEvent event){
        return true;
    }
}
