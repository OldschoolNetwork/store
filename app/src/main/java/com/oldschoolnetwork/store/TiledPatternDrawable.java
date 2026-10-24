package com.oldschoolnetwork.store;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

public class TiledPatternDrawable extends Drawable {
    private final Drawable mVectorDrawable;
    private final int mSize;
    private final int mSpacing;

    public TiledPatternDrawable(Context context, int resId, int sizeDp, int spacingDp) {
        mVectorDrawable = ContextCompat.getDrawable(context, resId);
        float density = context.getResources().getDisplayMetrics().density;
        mSize = (int) (sizeDp * density);
        mSpacing = (int) (spacingDp * density);
    }

    @Override
    public void draw(Canvas canvas) {
        // Берем границы конкретно нашей кнопки, а не всего экрана
        Rect bounds = getBounds();
        int width = bounds.width();
        int height = bounds.height();

        if (width <= 0 || height <= 0) return;

        canvas.save();

        // Смещаем холст к началу координат нашей кнопки
        canvas.translate(bounds.left, bounds.top);

        // Поворачиваем строго вокруг центра КНОПКИ
        canvas.rotate(-45, width / 2f, height / 2f);

        int step = mSize + mSpacing;
        int maxDim = Math.max(width, height);

        // Заполняем сетку с запасом во все стороны из-за угла поворота
        for (int x = -maxDim; x < width + maxDim; x += step) {
            for (int y = -maxDim; y < height + maxDim; y += step) {
                if (mVectorDrawable != null) {
                    mVectorDrawable.setBounds(x, y, x + mSize, y + mSize);
                    mVectorDrawable.draw(canvas);
                }
            }
        }
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        if (mVectorDrawable != null) {
            mVectorDrawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (mVectorDrawable != null) {
            mVectorDrawable.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}