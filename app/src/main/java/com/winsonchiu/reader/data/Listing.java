package com.winsonchiu.reader.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
    }

    public static Listing fromJson(JSONObject rootJsonObject) throws JSONException {

        Listing listing = new Listing();
        JSONObject jsonObject = rootJsonObject.getJSONObject("data");

        listing.setBefore(jsonObject.getString("before"));
        listing.setAfter(jsonObject.getString("after"));
        listing.setModHash(jsonObject.getString("modhash"));

        JSONArray jsonArray = jsonObject.getJSONArray("children");

        ArrayList<Thing> things = new ArrayList<>(jsonArray.length());

        for (int index = 0; index < jsonArray.length(); index++) {

            JSONObject thing = jsonArray.getJSONObject(index);

            switch (thing.getString("kind")) {

                case "t3":
                    things.add(Link.fromJson(thing));
                    break;

            }

        }

        listing.setChildren(things);

        return listing;
    }
}
