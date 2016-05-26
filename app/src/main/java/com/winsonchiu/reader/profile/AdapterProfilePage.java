/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.profile;

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
public class AdapterProfilePage extends BaseAdapter {

    private Themer themer;
    private List<Page> pages;
    private int colorPrimary;
    private boolean isUser;

    public AdapterProfilePage(Activity activity) {
        super();

        pages = new ArrayList<>();
        pages.add(new Page(ControllerProfile.PAGE_OVERVIEW, activity.getString(R.string.profile_page_overview)));
        pages.add(new Page(ControllerProfile.PAGE_SUBMITTED, activity.getString(R.string.profile_page_submitted)));
        pages.add(new Page(ControllerProfile.PAGE_COMMENTS, activity.getString(R.string.profile_page_comments)));
        pages.add(new Page(ControllerProfile.PAGE_GILDED, activity.getString(R.string.profile_page_gilded)));
        pages.add(new Page(ControllerProfile.PAGE_UPVOTED, activity.getString(R.string.profile_page_upvoted)));
        pages.add(new Page(ControllerProfile.PAGE_DOWNVOTED, activity.getString(R.string.profile_page_downvoted)));
        pages.add(new Page(ControllerProfile.PAGE_HIDDEN, activity.getString(R.string.profile_page_hidden)));
        pages.add(new Page(ControllerProfile.PAGE_SAVED, activity.getString(R.string.profile_page_saved)));

        themer = new Themer(activity);
    }

    @Override
    public int getCount() {
        return isUser ? pages.size() : 4;
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
        textPage.setTextColor(themer.getColorFilterPrimary().getColor());

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

    public boolean isUser() {
        return isUser;
    }

    public void setIsUser(boolean isUser) {
        this.isUser = isUser;
        notifyDataSetChanged();
    }

    public List<Page> getPages() {
        return pages;
    }
}
