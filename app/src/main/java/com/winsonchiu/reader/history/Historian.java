/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.data.reddit.Link;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TheKeeperOfPie on 7/8/2015.
 */
public class Historian {

    private static final String FILE_NAME = "history";
    private static final String TAG = Historian.class.getCanonicalName();
    private static Historian historian;
    private Map<String, HistoryEntry> mapHistory;
    private HistoryEntry first;
    private int size;

    public static Historian getInstance(Context context) {
        if (historian == null) {
            historian = new Historian();
            try {
                FileInputStream fileInputStream = context.openFileInput(FILE_NAME);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    builder.append(line);
                }
                bufferedReader.close();
                fileInputStream.close();

                historian.fromJsonArray(new JSONArray(builder.toString()));

                Log.d(TAG, "Create Historian: " + historian.toString());
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return historian;
    }

    public Historian() {
        this.mapHistory = new HashMap<>();
    }

    /**
     *
     * @param link
     * @return true if new entry, false if old entry updated
     */
    public void add(Link link) {

        HistoryEntry entry = mapHistory.get(link.getName());
        if (entry == null) {
            entry = new HistoryEntry(link);
            entry.setNext(first);
            if (first != null) {
                first.setPrevious(entry);
            }
            first = entry;
            size++;
        }
        else {
            if (entry != first) {
                if (entry.getNext() != null) {
                    entry.getNext().setPrevious(entry.getPrevious());
                }
                if (entry.getPrevious() != null) {
                    entry.getPrevious().setNext(entry.getNext());
                }
                entry.setNext(first);
                entry.setPrevious(null);
                first.setPrevious(entry);
                first = entry;
            }
        }
        first.setTimestamp(System.currentTimeMillis());
        first.setRemoved(false);

        mapHistory.put(link.getName(), entry);
    }

    public void fromJsonArray(JSONArray jsonArray) {

        List<HistoryEntry> entries = new ArrayList<>(jsonArray.length());

        try {
            for (int index = 0; index < jsonArray.length(); index++) {
                entries.add(HistoryEntry.fromJson(jsonArray.getJSONObject(index)));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        buildFromList(entries);

    }

    public JSONArray toJsonArray(int max) {

        JSONArray jsonArray = new JSONArray();

        int num = 0;

        HistoryEntry currentEntry = first;
        while (currentEntry != null && num++ < max) {
            if (!currentEntry.isRemoved()) {
                jsonArray.put(currentEntry.toJson());
            }
            currentEntry = currentEntry.getNext();
        }

        return jsonArray;
    }

    @Override
    public String toString() {

        HistoryEntry currentEntry = first;

        StringBuilder builder = new StringBuilder();

        while (currentEntry != null) {
            builder.append(currentEntry);
            builder.append(", ");
            currentEntry = currentEntry.getNext();
        }

        return builder.toString();
    }

    public void buildFromList(List<HistoryEntry> entries) {

        first = null;

        if (entries.isEmpty()) {
            return;
        }

        first = entries.get(0);
        mapHistory.put(first.getName(), first);

        HistoryEntry currentEntry = first;
        size = entries.size();

        for (int index = 1; index < entries.size(); index++) {
            HistoryEntry entry = entries.get(index);

            entry.setPrevious(currentEntry);
            currentEntry.setNext(entry);
            currentEntry = entry;

            mapHistory.put(entry.getName(), entry);
        }

    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public HistoryEntry getFirst() {
        return first;
    }

    public static void saveToFile(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int max;
        try {
            max = Integer.parseInt(preferences.getString(AppSettings.PREF_HISTORY_SIZE, "5000"));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            max = 500;
        }

        try {
            String data = getInstance(context).toJsonArray(max).toString();

            Log.d(TAG, "Save Historian: " + data);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean contains(String name) {
        return mapHistory.keySet().contains(name);
    }

    public static void clear(Context context) {
        getInstance(context).mapHistory.clear();
        HistoryEntry currentEntry = historian.first;

        while (currentEntry != null) {
            if (currentEntry.getPrevious() != null) {
                currentEntry.getPrevious().setNext(null);
            }
            currentEntry.setPrevious(null);
            currentEntry = currentEntry.getNext();
        }

        historian.first = null;

        System.gc();

        saveToFile(context);
    }

    public HistoryEntry getEntry(Link link) {
        return mapHistory.get(link.getName());
    }
}