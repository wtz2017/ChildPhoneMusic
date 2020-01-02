package com.wtz.child.phonemusic.utils;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;

public class PicassoRoundTransformation implements Transformation {

    private int roundPx;

    public PicassoRoundTransformation(int roundPx) {
        this.roundPx = roundPx;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap result = BitmapUtils.roundImageCorner(source, BitmapUtils.RoundCornerType.ALL, roundPx);
        if (result != source) {
            source.recycle();
        }
        return result;
    }

    @Override
    public String key() {
        return "PicassoRoundTransformation";
    }

}
