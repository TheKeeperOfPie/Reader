package com.winsonchiu.reader;

/**
 * Created by TheKeeperOfPie on 3/16/2015.
 */

/*
    Class used to hide implementation of networking interface, to allow quick changes
    in library or protocol while still in development.
 */
public abstract class ResponseReddit {
    abstract void onCompleted();
    abstract void onError();
}
