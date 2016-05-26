/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.Page;
import com.winsonchiu.reader.theme.Themer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterInboxPage extends BaseAdapter {

    private Themer themer;
    private List<Page> pages;

    public AdapterInboxPage(Activity activity) {
        pages = new ArrayList<>();
        pages.add(new Page(ControllerInbox.INBOX, activity.getString(R.string.inbox_page_inbox)));
        pages.add(new Page(ControllerInbox.UNREAD, activity.getString(R.string.inbox_page_unread)));
        pages.add(new Page(ControllerInbox.SENT, activity.getString(R.string.inbox_page_sent)));
        pages.add(new Page(ControllerInbox.COMMENTS, activity.getString(R.string.inbox_page_comments)));
        pages.add(new Page(ControllerInbox.SELF_REPLY, activity.getString(R.string.inbox_page_self_reply)));
        pages.add(new Page(ControllerInbox.MENTIONS, activity.getString(R.string.inbox_page_mentions)));
        pages.add(new Page(ControllerInbox.MODERATOR, activity.getString(R.string.inbox_page_moderator)));
        pages.add(new Page(ControllerInbox.MODERATOR_UNREAD, activity.getString(R.string.inbox_page_moderator_unread)));

        themer = new Themer(activity);
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
        textPage.setTextColor(themer.getColorFilterIcon().getColor());

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
        textPage.setTextColor(themer.getColorFilterPrimary().getColor());

        return convertView;
    }

    public List<Page> getPages() {
        return pages;
    }
}
