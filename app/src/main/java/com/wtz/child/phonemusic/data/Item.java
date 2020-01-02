package com.wtz.child.phonemusic.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Item implements Parcelable, Comparable {

    public static final int TYPE_DIR = 1;
    public static final int TYPE_AUDIO = 2;

    public int type;
    public String name;
    public String path;

    public Item(int type, String name, String path) {
        this.type = type;
        this.name = name;
        this.path = path;
    }

    protected Item(Parcel in) {
        type = in.readInt();
        name = in.readString();
        path = in.readString();
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(type);
        parcel.writeString(name);
        parcel.writeString(path);
    }

    @Override
    public int compareTo(Object o) {
        int ret;
        Item next = (Item) o;
        if (this.type == next.type) {
            ret = this.name.compareTo(next.name);
        } else {
            if (this.type == TYPE_DIR) {
                ret = 1;
            } else {
                ret = -1;
            }
        }
        return ret;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Item[");
        builder.append("type=");
        builder.append(type);
        builder.append(";name=");
        builder.append(name);
        builder.append(";path=");
        builder.append(path);
        builder.append("]");
        return builder.toString();
    }
}
