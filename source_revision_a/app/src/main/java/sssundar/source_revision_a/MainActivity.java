package sssundar.source_revision_a;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.view.View.OnClickListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import android.text.TextUtils;

public class MainActivity extends Activity implements OnClickListener {

    private DrawingView drawView;

    private ImageButton eraseBtn;

    private Thread sampler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawView = (DrawingView)findViewById(R.id.drawing);

        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view){
        if(view.getId()==R.id.erase_btn){
            drawView.wipeScreen();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        sampler.interrupt();
        sampler = null;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        sampler = new Thread(new Runnable()
        {

            private int[] bmPixels = new int[40000];

            @Override
            public void run()
            {
                while (!Thread.interrupted())
                    try
                    {
                        Thread.sleep(1000);
                        sampleScreen();
                    } catch (InterruptedException e) {
                        Log.d("Boogie", "Encoding thread was interrupted");
                    }
            }

            private void sampleScreen() {
                // Make an immutable copy of the Drawing View bitmap, resized to 200x200.
                // ToDo There's no way the image on the Nexus screen is 200x200, right?
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

                Log.d("Boogie", "Length of RLE is " + String.valueOf(csdata.length()) + " characters");

                // HTTP post this string to the Amazon EC2 webserver associated with this app
                String url = "http://ec2-54-67-127-196.us-west-1.compute.amazonaws.com/";
                try {
                    URL server = new URL(url);
                    HttpURLConnection ec2 = (HttpURLConnection) server.openConnection();
                    ec2.setDoOutput(true);
                    ec2.setDoInput(true);
                    ec2.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    ec2.setRequestProperty("Accept", "application/json");
                    try {
                        ec2.setRequestMethod("POST");

                        JSONObject data = new JSONObject();
                        try {
                            data.put("run_length_encoding",csdata);
                            try {
                                OutputStream os = ec2.getOutputStream();
                                os.write(data.toString().getBytes("UTF-8"));
                                os.close();

                                int HttpResult = ec2.getResponseCode();
                                if (HttpResult == HttpURLConnection.HTTP_OK) {
                                    Log.d("Boogie", "RLE POST Successful!");
                                } else {
                                    Log.d("Boogie", "RLE POST Failed with Response Code: " + String.valueOf(HttpResult));
                                }
                            } catch (IOException o) {
                                Log.d("Boogie", "IO Exception!");
                            }
                        } catch (JSONException j) {
                            Log.d("Boogie", "JSONException!");
                        }
                    } catch (ProtocolException p) {
                        Log.d("Boogie", "ProtocolException!");
                    }
                } catch (IOException m) {
                    Log.d("Boogie", "MalformedURLException or other IO Exception!");
                }
            }
        });

        sampler.start();

    }

}
