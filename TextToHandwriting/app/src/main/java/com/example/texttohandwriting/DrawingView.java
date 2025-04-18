package com.example.texttohandwriting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Custom view component for drawing handwriting.
 * Handles touch events to capture drawing paths and renders them onto a bitmap.
 * Provides functionality for capturing the drawn content as a bitmap,
 * clearing the canvas, and adjusting stroke width.
 */
public class DrawingView extends View {

    private Paint paint;
    private Path path;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;

    /**
     * Constructor for creating a DrawingView programmatically or from XML layout.
     * 
     * @param context The context in which the view is running
     * @param attrs The attributes of the XML tag that is inflating the view
     */
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initializes the drawing tools (paint and path).
     * Sets up default paint properties such as color, stroke width, and style.
     */
    private void init() {
        path = new Path();
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        // Default marker (stroke) width; you can update dynamically.
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * Called when the size of the view has changed.
     * Creates a new bitmap and canvas with the new dimensions.
     * 
     * @param w The new width of the view
     * @param h The new height of the view
     * @param oldw The old width of the view
     * @param oldh The old height of the view
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Create a bitmap based on the view dimensions
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        bitmapCanvas = new Canvas(bitmap);
    }

    /**
     * Renders the current drawing state to the canvas.
     * Draws both the persistent bitmap and the current path being drawn.
     * 
     * @param canvas The canvas on which the view is drawn
     */
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.drawPath(path, paint);
    }

    /**
     * Handles touch events to capture drawing gestures.
     * Tracks finger movement and converts it to drawing paths.
     * 
     * @param event The motion event containing touch information
     * @return True if the event was handled, false otherwise
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                bitmapCanvas.drawPath(path, paint);
                path.reset();
                break;
        }
        invalidate();
        return true;
    }

    /**
     * Returns the bitmap drawn so far.
     * Can be used to save or process the drawn content.
     * 
     * @return The current drawing as a bitmap
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * Clears the drawing canvas.
     * Erases all drawn content and resets to a white background.
     */
    public void clear() {
        bitmap.eraseColor(Color.WHITE);
        invalidate();
    }

    /**
     * Sets the marker (draw) size by adjusting the stroke width of the paint.
     * Controls the thickness of the drawing line.
     *
     * @param size The new stroke width in pixels.
     */
    public void setMarkerSize(float size) {
        paint.setStrokeWidth(size);
    }
}