package sssundar.reva.boogie;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.view.View.OnClickListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

public class DoodlerActivity extends Activity implements OnClickListener {

    private DrawingView drawView;
    private ImageButton eraseBtn, callIssueBtn, hangUpBtn;
    private ImageButton callAmmaBtn, callBaluBtn, callSushBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doodler);

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

        callAmmaBtn.setEnabled(true);
        callBaluBtn.setEnabled(true);
        callSushBtn.setEnabled(true);

        callIssueBtn.setEnabled(false);
        hangUpBtn.setEnabled(true);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
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
    }

}
