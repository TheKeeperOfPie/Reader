/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data;

/**
 * Created by TheKeeperOfPie on 7/26/2015.
 */
public class Page {

    private String page;
    private String text;

    public Page(String page, String text) {
        this.page = page;
        this.text = text;
    }

    public String getPage() {
        return page;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Page page1 = (Page) o;

        return !(getPage() != null ? !getPage().equals(page1.getPage()) : page1.getPage() != null);

    }

    @Override
    public int hashCode() {
        return getPage() != null ? getPage().hashCode() : 0;
    }
}
