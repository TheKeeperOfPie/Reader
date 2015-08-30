/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.utils.UtilsColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterInboxPage extends BaseAdapter {

    private final int colorTextMenu;
    private final int colorTextPrimary;
    private List<Page> pages;
    private int colorPrimary;

    public AdapterInboxPage(Activity activity) {
        super();

        pages = new ArrayList<>();
        pages.add(new Page(ControllerInbox.INBOX, activity.getString(R.string.inbox_page_inbox)));
        pages.add(new Page(ControllerInbox.UNREAD, activity.getString(R.string.inbox_page_unread)));
        pages.add(new Page(ControllerInbox.SENT, activity.getString(R.string.inbox_page_sent)));
        pages.add(new Page(ControllerInbox.COMMENTS, activity.getString(R.string.inbox_page_comments)));
        pages.add(new Page(ControllerInbox.SELF_REPLY, activity.getString(R.string.inbox_page_self_reply)));
        pages.add(new Page(ControllerInbox.MENTIONS, activity.getString(R.string.inbox_page_mentions)));
        pages.add(new Page(ControllerInbox.MODERATOR, activity.getString(R.string.inbox_page_moderator)));
        pages.add(new Page(ControllerInbox.MODERATOR_UNREAD, activity.getString(R.string.inbox_page_moderator_unread)));

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary});
        colorPrimary = typedArray.getColor(0, activity.getResources().getColor(R.color.colorPrimary));
        colorTextMenu = typedArray.getColor(1, Color.WHITE);
        typedArray.recycle();

        colorTextPrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? Color.WHITE : Color.BLACK;
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public Page getItem(int position) {
        return pages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_page, parent, false);
//            convertView.setBackgroundColor(colorPrimary);
        }

        TextView textPage = (TextView) convertView.findViewById(R.id.text_page);
        textPage.setText(pages.get(position).getText());
        textPage.setTextColor(colorTextMenu);

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_page, parent, false);
//            convertView.setBackgroundColor(colorPrimary);
        }

        TextView textPage = (TextView) convertView.findViewById(R.id.text_page);
        textPage.setText(pages.get(position).getText());
        textPage.setTextColor(colorTextPrimary);

        return convertView;
    }

    public List<Page> getPages() {
        return pages;
    }
}
