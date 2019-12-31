package com.wtz.child.phonemusic.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

public class MusicIcon {

    private static final String ICON_BASE_URL = "https://www.gravatar.com/avatar/";
    private static final String ICON_REQUEST_PARA = "?d=identicon&s=80";// 80 表示大小 80x80

    public static String getRandomIconUrl(String musicName) {
        String md5 = Md5Util.getStringMD5(musicName).toLowerCase();
        String ret = ICON_BASE_URL + md5 + ICON_REQUEST_PARA;
        return ret;
    }

    public static Bitmap getArtworkFromFile(String filePath, int targetWidth, int targetHeight) {
        if (filePath == null) {
            return null;
        }
        Bitmap bitmap = null;
        byte[] tempByteArray;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            tempByteArray = retriever.getEmbeddedPicture();
            if (tempByteArray != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(tempByteArray, 0, tempByteArray.length, options);
                if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
                    return null;
                }
                options.inJustDecodeBounds = false;
                if (options.outWidth > targetWidth || options.outHeight > targetHeight) {
                    int heightSampleRate = (int) Math.ceil(((double)options.outHeight/(double)targetHeight));
                    int widthSampleRate = (int) Math.ceil((double)options.outWidth/(double)targetWidth);
                    options.inSampleSize = heightSampleRate > widthSampleRate ? heightSampleRate : widthSampleRate;
                }
                bitmap = BitmapFactory.decodeByteArray(tempByteArray, 0, tempByteArray.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

}
