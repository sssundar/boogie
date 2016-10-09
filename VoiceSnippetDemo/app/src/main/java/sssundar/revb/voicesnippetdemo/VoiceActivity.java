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
import android.os.Process;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;


public class VoiceActivity extends Activity implements View.OnTouchListener {

    public Button recordButton, playButton;
    private MediaRecorder myAudioRecorder = null;
    private MediaPlayer myMediaPlayer = null;
    private AudioManager myAudioManager = null;
    private String outputDir = null;
    private String inputDir = null;
    private String tempOutputDir = null;
    private String currentRecordingName = null;
    private int sendCount = 0;
    public Thread voice_messenger;

    public void setup_button_listeners () {
        recordButton = (Button)findViewById(R.id.record_button);
        recordButton.setOnTouchListener(this);
        playButton = (Button)findViewById(R.id.playback_button);
        playButton.setOnTouchListener(this);
    }

    public void setup_app_persistent_storage () {
        String folder_input = "Boogie_Voice_Demo/input";
        String folder_output = "Boogie_Voice_Demo/output";
        String folder_temp_output = "Boogie_Voice_Demo/temp_output";
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_input);
        if (!f.exists()) {
            f.mkdirs();
        }
        f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_output);
        if (!f.exists()) {
            f.mkdirs();
        }
        f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_temp_output);
        if (!f.exists()) {
            f.mkdirs();
        }

        inputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + folder_input + "/";
        outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + folder_output + "/";
        tempOutputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + folder_temp_output + "/";
    }

    public void start_messenger_thread () {
        voice_messenger = new Thread(new Runnable()
        {
            private String latestMessageIn = null;

            @Override
            public void run()
            {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                while (!Thread.interrupted()) {
                    processOutput(); // if output dir is not empty, send it out, then delete it.
                    processInput(); // if input dir is empty, get next filename and then the file, and update global state for next touch
                }
            }

            private void processOutput() {
                File dir = new File(outputDir);
                File[] files = dir.listFiles();
                // No internal directories
                if (files.length == 0) {
                    return;
                }

                String filename = files[0].getName();

                String url = "http://ec2-54-183-116-184.us-west-1.compute.amazonaws.com:8080/upload";

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

                } catch (Exception e) {
                    Log.d("Boogie", "Exception during voice upload", e);
                }

                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void processInput() {
                InputStream is = null;
                String url = "http://ec2-54-183-116-184.us-west-1.compute.amazonaws.com:8080/check_mailbox";
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    int responseCode = ec2.getResponseCode();

                    String server_response = null;
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        server_response = readStream(ec2.getInputStream());
                    } else {
                        return;
                    }

                    // Get the file, if it's new!
                    if ((latestMessageIn == null) || (!server_response.equals(latestMessageIn))) {
                        latestMessageIn = server_response;
                        url = "http://ec2-54-183-116-184.us-west-1.compute.amazonaws.com:8080/download/?name=" + latestMessageIn;
                        server = new URL(url);
                        ec2 = (HttpURLConnection) server.openConnection();
                        ec2.setRequestMethod("GET");
                        ec2.setDoInput(true);
                        ec2.setDoOutput(true);
                        ec2.connect();
                        is = ec2.getInputStream();
                        copyInputStreamToFile(is, new File(inputDir + latestMessageIn));
                        Log.d("Boogie", "Brought in " + latestMessageIn);
                    }

                } catch (Exception e) {
                    Log.d("Boogie", "Exception during message polling", e);
                }

            }

        });

        voice_messenger.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);
        setup_button_listeners();
        setup_app_persistent_storage();
        start_messenger_thread();

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
                        currentRecordingName = "test" + String.valueOf(sendCount) + ".3gp";
                        myAudioRecorder = new MediaRecorder();
                        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                        myAudioRecorder.setOutputFile(tempOutputDir + currentRecordingName);
                        sendCount = sendCount + 1;
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
                        File dir = new File(inputDir);
                        File[] files = dir.listFiles();
                        // No internal directories
                        if (files.length > 0) {
                            myMediaPlayer = new MediaPlayer();
                            myMediaPlayer.setDataSource(files[0].getAbsolutePath());
                            myMediaPlayer.prepare();
                            myMediaPlayer.start();
                        }
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

                    // Move temporary file to send folder
                    File source = new File(tempOutputDir + currentRecordingName);
                    File dest = new File(outputDir + currentRecordingName);
                    source.renameTo(dest);
                } else if (view.getId()==R.id.playback_button) {
                    // Stop playback from temp file
                    if (myMediaPlayer != null) {
                        myMediaPlayer.stop();
                        myMediaPlayer.release();
                        // delete the input file
                        File dir = new File(inputDir);
                        File[] files = dir.listFiles();
                        Log.d("Boogie", "Deleting " + files[0].getName());
                        files[0].delete();
                    }
                    myMediaPlayer = null;
                }
                return true;
        }
        return false;
    }
}

