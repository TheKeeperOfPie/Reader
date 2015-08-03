/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.utils.UtilsJson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/8/2015.
 */
public class Listing {

    private static final String TAG = Listing.class.getCanonicalName();
    private String before;
    private String after;
    private String modHash;
    private List<Thing> children;

    public Listing() {
        super();
        children = new ArrayList<>();
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

    public static Listing fromJson(JsonNode nodeRoot) {

        long start = System.currentTimeMillis();

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


            switch (UtilsJson.getString(node.get("kind"))) {

                // TODO: Add cases for all ID36s and fix adding Comments

                case "more":
                    things.add(Comment.fromJson(node, 0));
                    break;
                case "t1":
                    Comment.addAllFromJson(things, node, 0);
                    break;
                case "t3":
                    things.add(Link.fromJson(node));
                    break;
                case "t4":
                    things.add(Message.fromJson(node));
                    break;
                case "t5":
                    things.add(Subreddit.fromJson(node));
                    break;

            }
        }

        listing.setChildren(things);

        Log.d(TAG, Arrays.toString(Thread.currentThread().getStackTrace()));
        Log.d(TAG, "fromJson end: " + (System.currentTimeMillis() - start));

        return listing;
    }
}
