package com.wtz.child.phonemusic.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wtz.child.phonemusic.data.Item;
import com.wtz.child.phonemusic.R;
import com.wtz.child.phonemusic.utils.LogUtils;
import com.wtz.child.phonemusic.utils.MusicIcon;

import java.util.ArrayList;
import java.util.List;


public class MusicGridAdapter extends BaseAdapter {
    private final static String TAG = MusicGridAdapter.class.getSimpleName();

    private Context mContext;
    private List<Item> mDataList = new ArrayList<>();
    private int mItemWidth;
    private int mItemHeight;
    private AbsListView.LayoutParams mItemLayoutParams;
    private Handler mHandler;

    public MusicGridAdapter(Context context, List<Item> dataList, int itemWidth, int itemHeight, Handler handler) {
        mContext = context;
        mDataList.addAll(dataList);
        mItemWidth = itemWidth;
        mItemHeight = itemHeight;
        mHandler = handler;
    }

    public void updateData(List<Item> dataList) {
        mDataList.clear();
        mDataList.addAll(dataList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return (mDataList == null) ? 0 : mDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return (mDataList == null) ? null : mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        final Item item = (Item) getItem(position);
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_music_gridview, parent, false);
            if (mItemLayoutParams == null) {
                mItemLayoutParams = (AbsListView.LayoutParams) convertView.getLayoutParams();
                mItemLayoutParams.width = mItemWidth;
                mItemLayoutParams.height = mItemHeight;
            }
            convertView.setLayoutParams(mItemLayoutParams);
            holder.id = item.path;
            holder.imageView = (ImageView) convertView.findViewById(R.id.iv_img);
            holder.name = convertView.findViewById(R.id.tv_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.name.setText(item.name);
        if (item.type == Item.TYPE_DIR) {
            holder.imageView.setImageResource(R.drawable.icon_folder);
        } else if (item.type == Item.TYPE_AUDIO) {
            holder.imageView.setImageResource(R.drawable.icon_music);
            holder.id = item.path;
            final ViewHolder finalHolder = holder;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap bitmap = MusicIcon.getArtworkFromFile(item.path, mItemLayoutParams.width, mItemLayoutParams.height);
                    LogUtils.d(TAG, "getArtworkFromFile bitmap=" + bitmap);
                    if (!item.path.equals(finalHolder.id)) {
                        LogUtils.d(TAG, "after getArtworkFromFile path != finalHolder.id");
                        return;
                    }
                    if (bitmap != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                finalHolder.imageView.setImageBitmap(bitmap);
                            }
                        });
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Picasso call should happen from the main thread
                                Picasso.get()
                                        .load(MusicIcon.getRandomIconUrl(item.name))
                                        // 解决 OOM 问题
                                        .resize(mItemLayoutParams.width, mItemLayoutParams.height)
                                        .centerCrop()// 需要先调用fit或resize设置目标大小，否则会报错：Center crop requires calling resize with positive width and height
                                        .placeholder(R.drawable.icon_music)
                                        .noFade()
                                        .into(finalHolder.imageView);
                            }
                        });
                    }
                }
            }).start();
        }

        return convertView;
    }

    class ViewHolder {
        String id;
        ImageView imageView;
        TextView name;
    }

}
