/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by TheKeeperOfPie on 3/8/2015.
 */
public class Thing implements Parcelable {

    private static final String TAG = Thing.class.getCanonicalName();

    private String id;
    private String name;
    private String kind;

    public Thing() {

    }

    public Thing(String id, String name, String kind) {
        this.id = id;
        this.name = name;
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        return "Thing{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", kind='" + kind + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Thing thing = (Thing) o;

        return getId().equals(thing.getId());

    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.kind);
    }

    protected Thing(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.kind = in.readString();
    }

    public static final Creator<Thing> CREATOR = new Creator<Thing>() {
        public Thing createFromParcel(Parcel source) {
            return new Thing(source);
        }

        public Thing[] newArray(int size) {
            return new Thing[size];
        }
    };
}
