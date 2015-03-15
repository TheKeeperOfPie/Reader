package com.winsonchiu.reader;

/**
 * Created by TheKeeperOfPie on 3/13/2015.
 */
public class NavItem {

    private int drawable;
    private String title;

    public NavItem(int drawable, String title) {
        this.drawable = drawable;
        this.title = title;
    }

    public int getDrawable() {
        return drawable;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
