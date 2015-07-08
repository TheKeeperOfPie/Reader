/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

    public static Listing fromJson(JSONObject rootJsonObject) throws JSONException {

        Listing listing = new Listing();
        JSONObject jsonObject = rootJsonObject.getJSONObject("data");

        listing.setBefore(jsonObject.optString("before"));
        listing.setAfter(jsonObject.optString("after"));
        listing.setModHash(jsonObject.optString("modhash"));

        JSONArray jsonArray = jsonObject.getJSONArray("children");

        ArrayList<Thing> things = new ArrayList<>(jsonArray.length());

        for (int index = 0; index < jsonArray.length(); index++) {

            JSONObject thing = jsonArray.getJSONObject(index);

            switch (thing.optString("kind")) {

                // TODO: Add cases for all ID36s and fix adding Comments

                case "more":
                    things.add(Comment.fromJson(thing, 0));
                    break;
                case "t1":
                    List<Comment> comments = new ArrayList<>();
                    Comment.addAllFromJson(comments, thing, 0);
                    things.addAll(comments);
                    break;
                case "t3":
                    things.add(Link.fromJson(thing));
                    break;
                case "t4":
                    things.add(Message.fromJson(thing));
                    break;
                case "t5":
                    things.add(Subreddit.fromJson(thing));
                    break;

            }

        }

        listing.setChildren(things);

        return listing;
    }
}
