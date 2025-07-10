package com.notificationdetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class StrokeTextView extends AppCompatTextView {
    private Paint strokePaint = new Paint();

    public StrokeTextView(Context context) {
        super(context);
        init();
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4);
        strokePaint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw stroke
        int currentTextColor = getCurrentTextColor();
        float strokeWidth = 4f;
        strokePaint.setTextSize(getTextSize());
        strokePaint.setTypeface(getTypeface());
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.STROKE);
        // Draw the stroke text
        String text = getText() != null ? getText().toString() : "";
        int x = getPaddingLeft();
        int y = getBaseline();
        canvas.drawText(text, x, y, strokePaint);
        // Draw the fill text
        setTextColor(currentTextColor);
        strokePaint.setStyle(Paint.Style.FILL);
        super.onDraw(canvas);
    }
}
