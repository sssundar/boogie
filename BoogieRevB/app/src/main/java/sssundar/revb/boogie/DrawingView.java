package sssundar.revb.boogie;

import android.graphics.Color;
import android.view.View;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.graphics.PorterDuff;

public class DrawingView extends View {

    private Path drawPath;
    private Paint drawPaint, canvasPaint;

    private int paintColor = 0xFF000000; // black

    private int userID;

    private Canvas drawCanvas;
    public volatile Bitmap canvasBitmap;
    public volatile boolean isDrawingAllowed = false;

    public DrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        setupDrawing();
    }

    public void allowDrawing(boolean enable) {
        isDrawingAllowed = enable && (userID == 2);
    }

    private void setupDrawing(){
        drawPath = new Path();

        drawPaint = new Paint();

        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(false);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);

        userID = MessengerActivity.userID;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    public void setCanvasBitmap (Bitmap b) {
        canvasBitmap = b;
        drawCanvas = new Canvas(b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isDrawingAllowed) {
            float touchX = event.getX();
            float touchY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    drawPath.moveTo(touchX, touchY);
                    drawCanvas.drawCircle(touchX, touchY, 2, drawPaint);
                    break;
                case MotionEvent.ACTION_MOVE:
                    drawPath.lineTo(touchX, touchY);
                    break;
                case MotionEvent.ACTION_UP:
                    drawCanvas.drawPath(drawPath, drawPaint);
                    drawPath.reset();
                    break;
                default:
                    return false;
            }

            invalidate();
        }
        return true;
    }

    public void wipeScreen(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

}
