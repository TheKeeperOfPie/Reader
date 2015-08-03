/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.history;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 7/9/2015.
 */
public class ThreadHistorySearch extends Thread {

    private HistoryEntry entry;
    private String query;
    private Callback callback;
    private List<String> names;

    public ThreadHistorySearch(HistoryEntry entry, String query,  Callback callback) {
        this.entry = entry;
        this.callback = callback;
        this.query = query;
        names = new ArrayList<>();
    }

    @Override
    public void run() {
        List<String> namesStartsWith = new ArrayList<>();
        List<String> namesContains = new ArrayList<>();

        while (entry != null) {
            if (isInterrupted()) {
                return;
            }

            int index = entry.getTitle().toLowerCase().indexOf(query);
            if (index == 0) {
                namesStartsWith.add(entry.getName());
            }
            else if (index > 0) {
                namesContains.add(entry.getName());
            }
            entry = entry.getNext();
        }

        if (!isInterrupted()) {
            names.addAll(namesStartsWith);
            names.addAll(namesContains);

            callback.onFinished(names);
        }
    }

    public interface Callback {
        void onFinished(List<String> names);
    }

}