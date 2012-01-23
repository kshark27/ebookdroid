package org.ebookdroid.core;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.text.TextPaint;

public enum PagePaint {
    DAY(Color.BLACK, Color.WHITE),

    NIGHT(Color.WHITE, Color.BLACK);

    public final Paint bitmapPaint;
    public final Paint nightBitmapPaint;
    public final TextPaint textPaint = new TextPaint();
    public final Paint fillPaint = new Paint();
    public final Paint decodingPaint = new Paint();
    public final Paint strokePaint = new Paint();

    private PagePaint(final int textColor, final int fillColor) {
        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);

        nightBitmapPaint = new Paint();
        nightBitmapPaint.setFilterBitmap(true);
        nightBitmapPaint.setXfermode(new PixelXorXfermode(-1));

        textPaint.setColor(textColor);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);

        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);

        decodingPaint.setColor(Color.GRAY);
        decodingPaint.setStyle(Paint.Style.FILL);

        strokePaint.setColor(textColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
    }
}