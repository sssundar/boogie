package sssundar.revb.voicesnippetdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;

public class VoiceActivity extends Activity implements View.OnTouchListener {

    public Button recordButton, playButton;
    private MediaRecorder myAudioRecorder = null;
    private MediaPlayer myMediaPlayer = null;
    private AudioManager myAudioManager = null;
    private String outputFile = null;

    public void setup_button_listeners () {
        recordButton = (Button)findViewById(R.id.record_button);
        recordButton.setOnTouchListener(this);
        playButton = (Button)findViewById(R.id.playback_button);
        playButton.setOnTouchListener(this);
    }

    public void setup_app_persistent_storage () {
        String folder_main = "Boogie_Voice_Demo";
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_main);
        if (!f.exists()) {
            f.mkdirs();
        }
        outputFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + folder_main + "/" + "temp.3gp";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);
        setup_button_listeners();
        setup_app_persistent_storage();

        myAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, myAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event){
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // PRESSED
                if (view.getId()==R.id.record_button) {
                    // Starting recording to temp file
                    try {
                        myAudioRecorder = new MediaRecorder();
                        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                        myAudioRecorder.setOutputFile(outputFile);
                        myAudioRecorder.prepare();
                        myAudioRecorder.start();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (view.getId()==R.id.playback_button) {
                    // Start playing back from temp file
                    try {
                        myMediaPlayer = new MediaPlayer();
                        myMediaPlayer.setDataSource(outputFile);
                        myMediaPlayer.prepare();
                        myMediaPlayer.start();
                    }

                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                // RELEASED
                if (view.getId()==R.id.record_button) {
                    // Stop recording to temp file
                    if (myAudioRecorder != null) {
                        myAudioRecorder.stop();
                        myAudioRecorder.release();
                    }
                    myAudioRecorder = null;
                } else if (view.getId()==R.id.playback_button) {
                    // Stop playback from temp file
                    if (myMediaPlayer != null) {
                        myMediaPlayer.stop();
                        myMediaPlayer.release();
                    }
                    myMediaPlayer = null;
                }
                return true;
        }
        return false;
    }
}

