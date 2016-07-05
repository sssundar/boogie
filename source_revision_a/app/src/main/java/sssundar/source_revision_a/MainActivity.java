package sssundar.source_revision_a;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

    private DrawingView drawView;

    private ImageButton eraseBtn;

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
}
