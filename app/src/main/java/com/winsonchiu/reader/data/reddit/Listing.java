/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.utils.UtilsJson;
import com.winsonchiu.reader.utils.UtilsReddit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/8/2015.
 */
public class Listing implements Parcelable {

    private static final String TAG = Listing.class.getCanonicalName();
    private String before;
    private String after;
    private String modHash;
    private List<Thing> children = new LinkedList<>();

    public Listing() {

    }

    public Listing(List<Thing> things) {
        children = things;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getModHash() {
        return modHash;
    }

    public void setModHash(String modHash) {
        this.modHash = modHash;
    }

    public List<Thing> getChildren() {
        return children;
    }

    public void setChildren(List<Thing> children) {
        this.children = children;
    }

    public void addChildren(List<Thing> children) {
        this.children.addAll(children);
        checkChildren();
    }

    public void checkChildren() {
        LinkedHashSet<Thing> linkedHashSet = new LinkedHashSet<>(this.children);
        this.children.clear();
        this.children.addAll(linkedHashSet);
    }

    public static Listing fromJson(JsonNode nodeRoot) throws JsonProcessingException {
        Listing listing = new Listing();
        JsonNode nodeData = nodeRoot.get("data");

        if (nodeData == null) {
             return listing;
        }

        listing.setBefore(UtilsJson.getString(nodeData.get("before")));
        listing.setAfter(UtilsJson.getString(nodeData.get("after")));
        listing.setModHash(UtilsJson.getString(nodeData.get("modhash")));

        ArrayList<Thing> things = new ArrayList<>();

        for (JsonNode node : nodeData.get("children")) {
            things.addAll(UtilsReddit.parseJson(node));
        }

        listing.setChildren(things);

        return listing;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.before);
        dest.writeString(this.after);
        dest.writeString(this.modHash);
        dest.writeList(this.children);
    }

    protected Listing(Parcel in) {
        this.before = in.readString();
        this.after = in.readString();
        this.modHash = in.readString();
        this.children = new ArrayList<Thing>();
        in.readList(this.children, List.class.getClassLoader());
    }

    public static final Creator<Listing> CREATOR = new Creator<Listing>() {
        public Listing createFromParcel(Parcel source) {
            return new Listing(source);
        }

        public Listing[] newArray(int size) {
            return new Listing[size];
        }
    };
}
