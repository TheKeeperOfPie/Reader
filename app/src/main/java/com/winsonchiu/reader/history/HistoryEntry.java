/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import com.winsonchiu.reader.data.reddit.Link;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implementation of LinkedList entry for use in history list, allowing instant access by reference
 *
 * Created by TheKeeperOfPie on 7/8/2015.
 */
public class HistoryEntry {

    private static final String KEY_NAME = "name";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TIMESTAMP = "timestamp";

    private String name;
    private String title;
    private long timestamp;
    private boolean removed;

    private HistoryEntry next;
    private HistoryEntry previous;

    public HistoryEntry(Link link) {
        this(link, System.currentTimeMillis());
    }

    public HistoryEntry(Link link, long timestamp) {
        this.name = link.getName();
        this.title = link.getTitle();
        this.timestamp = timestamp;
    }

    public HistoryEntry(String name, String title, long timestamp) {
        this.name = name;
        this.title = title;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public HistoryEntry getNext() {
        return next;
    }

    public void setNext(HistoryEntry next) {
        this.next = next;
    }

    public HistoryEntry getPrevious() {
        return previous;
    }

    public void setPrevious(HistoryEntry previous) {
        this.previous = previous;
    }

    @Override
    public String toString() {
        return "[" + name + ", " + timestamp + "]";
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_NAME, name);
            jsonObject.put(KEY_TITLE, title);
            jsonObject.put(KEY_TIMESTAMP, timestamp);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public static HistoryEntry fromJson(JSONObject jsonObject) {
        return new HistoryEntry(jsonObject.optString(KEY_NAME), jsonObject.optString(KEY_TITLE), jsonObject.optLong(KEY_TIMESTAMP));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }
}