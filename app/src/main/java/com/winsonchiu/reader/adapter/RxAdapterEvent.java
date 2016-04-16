/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.adapter;

import android.support.annotation.NonNull;

/**
 * Created by TheKeeperOfPie on 4/16/2016.
 */
public class RxAdapterEvent<Data> {

    private Data data;
    private Type type = Type.RESET;
    private int positionStart;
    private int size = 1;
    private Object payload;

    public RxAdapterEvent(Data data, @NonNull Type type, int positionStart, int size, Object payload) {
        this.data = data;
        this.type = type;
        this.positionStart = positionStart;
        this.size = size;
        this.payload = payload;
    }

    public RxAdapterEvent(Data data, @NonNull Type type, int positionStart, Object payload) {
        this.data = data;
        this.type = type;
        this.positionStart = positionStart;
        this.payload = payload;
    }

    public RxAdapterEvent(Data data, @NonNull Type type, int positionStart, int size) {
        this.data = data;
        this.type = type;
        this.positionStart = positionStart;
        this.size = size;
    }

    public RxAdapterEvent(Data data, @NonNull Type type, int positionStart) {
        this.data = data;
        this.type = type;
        this.positionStart = positionStart;
    }

    public RxAdapterEvent(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public int getPositionStart() {
        return positionStart;
    }

    public int getSize() {
        return size;
    }

    public Object getPayload() {
        return payload;
    }

    public enum Type {
        CHANGE,
        INSERT,
        REMOVE,
        RESET
    }
}
