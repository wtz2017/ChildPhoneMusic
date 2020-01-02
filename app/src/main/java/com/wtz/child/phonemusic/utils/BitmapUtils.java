package com.wtz.child.phonemusic.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

public class BitmapUtils {

    public enum RoundCornerType {
        LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM,
        LEFT, TOP, RIGHT, BOTTOM, ALL
    }

    /**
     * 其原理就是：先建立一个与图片大小相同的透明的Bitmap画板
     * 然后在画板上画出一个想要的形状的区域，通过绘制多个区域的并集达到目标形状
     * 最后把源图片贴上
     *
     * @param bitmap  原图
     * @param type    哪个角需要做成圆角
     * @param roundPx 圆角大小
     * @return
     */
    public static Bitmap roundImageCorner(Bitmap bitmap, RoundCornerType type, int roundPx) {
        try {
            final int width = bitmap.getWidth();
            final int height = bitmap.getHeight();

            Bitmap paintingBoard = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(paintingBoard);
            canvas.drawARGB(Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT);

            final Paint paint = new Paint();
            paint.setAntiAlias(true);

            switch (type) {
                case LEFT_TOP:
                    clipLeftTop(canvas, paint, roundPx, width, height);
                    break;
                case RIGHT_TOP:
                    clipRightTop(canvas, paint, roundPx, width, height);
                    break;
                case LEFT_BOTTOM:
                    clipLeftBottom(canvas, paint, roundPx, width, height);
                    break;
                case RIGHT_BOTTOM:
                    clipRightBottom(canvas, paint, roundPx, width, height);
                    break;
                case LEFT:
                    clipLeft(canvas, paint, roundPx, width, height);
                    break;
                case RIGHT:
                    clipRight(canvas, paint, roundPx, width, height);
                    break;
                case TOP:
                    clipTop(canvas, paint, roundPx, width, height);
                    break;
                case BOTTOM:
                    clipBottom(canvas, paint, roundPx, width, height);
                    break;
                case ALL:
                    clipAll(canvas, paint, roundPx, width, height);
                    break;
            }

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            final Rect src = new Rect(0, 0, width, height);
            final Rect dst = src;
            canvas.drawBitmap(bitmap, src, dst, paint);
            return paintingBoard;
        } catch (Exception e) {
            return bitmap;
        }
    }

    private static void clipLeftTop(final Canvas canvas, final Paint paint, int roundPx, int width, int height) {
        final Rect rect1 = new Rect(roundPx, 0, width, height);
        canvas.drawRect(rect1, paint);
        final RectF rect2 = new RectF(0, 0, roundPx * 2, height);
        canvas.drawRoundRect(rect2, roundPx, roundPx, paint);
        final Rect rect3 = new Rect(0, height - roundPx, roundPx, height);
        canvas.drawRect(rect3, paint);
    }

    private static void clipRightTop(final Canvas canvas, final Paint paint, int roundPx, int width, int height) {
        final Rect rect1 = new Rect(0, 0, width - roundPx, height);
        canvas.drawRect(rect1, paint);
        final RectF rect2 = new RectF(width - 2 * roundPx, 0, width, height);
        canvas.drawRoundRect(rect2, roundPx, roundPx, paint);
        final Rect rect3 = new Rect(width - roundPx, height - roundPx, width, height);
        canvas.drawRect(rect3, paint);
    }

    private static void clipLeftBottom(final Canvas canvas, final Paint paint, int roundPx, int width, int height) {
        final Rect rect1 = new Rect(roundPx, 0, width, height);
        canvas.drawRect(rect1, paint);
        final RectF rect2 = new RectF(0, 0, roundPx * 2, height);
        canvas.drawRoundRect(rect2, roundPx, roundPx, paint);
        final Rect rect3 = new Rect(0, 0, roundPx, roundPx);
        canvas.drawRect(rect3, paint);
    }

    private static void clipRightBottom(final Canvas canvas, final Paint paint, int roundPx, int width, int height) {
        final Rect rect1 = new Rect(0, 0, width - roundPx, height);
        canvas.drawRect(rect1, paint);
        final RectF rect2 = new RectF(width - 2 * roundPx, 0, width, height);
        canvas.drawRoundRect(rect2, roundPx, roundPx, paint);
        final Rect rect3 = new Rect(width - roundPx, 0, width, roundPx);
        canvas.drawRect(rect3, paint);
    }

    private static void clipLeft(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final Rect rect1 = new Rect(offset, 0, width, height);
        canvas.drawRect(rect1, paint);
        final RectF rect2 = new RectF(0, 0, offset * 2, height);
        canvas.drawRoundRect(rect2, offset, offset, paint);
    }

    private static void clipRight(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final Rect rect1 = new Rect(0, 0, width - offset, height);
        canvas.drawRect(rect1, paint);
        final RectF rect2 = new RectF(width - offset * 2, 0, width, height);
        canvas.drawRoundRect(rect2, offset, offset, paint);
    }

    private static void clipTop(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final Rect rect1 = new Rect(0, offset, width, height);
        canvas.drawRect(rect1, paint);
        final RectF rect2 = new RectF(0, 0, width, offset * 2);
        canvas.drawRoundRect(rect2, offset, offset, paint);
    }

    private static void clipBottom(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final Rect rect1 = new Rect(0, 0, width, height - offset);
        canvas.drawRect(rect1, paint);
        final RectF rect2 = new RectF(0, height - offset * 2, width, height);
        canvas.drawRoundRect(rect2, offset, offset, paint);
    }

    private static void clipAll(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final RectF rectF = new RectF(0, 0, width, height);
        canvas.drawRoundRect(rectF, offset, offset, paint);
    }
}
