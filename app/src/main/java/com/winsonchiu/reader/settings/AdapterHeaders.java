/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.winsonchiu.reader.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 7/2/2015.
 */
public class AdapterHeaders extends RecyclerView.Adapter<AdapterHeaders.ViewHolder> {

    private EventListener eventListener;
    private List<Header> headers;

    public AdapterHeaders(EventListener eventListener) {
        super();
        this.eventListener = eventListener;
        headers = new ArrayList<>();
        headers.add(new Header(R.drawable.ic_palette_white_24dp, R.string.prefs_category_display, R.string.prefs_category_display_summary));
        headers.add(new Header(R.drawable.ic_build_white_24dp, R.string.prefs_category_behavior, R.string.prefs_category_behavior_summary));
        headers.add(new Header(R.drawable.ic_mail_white_24dp, R.string.prefs_category_mail, R.string.prefs_category_mail_summary));
        headers.add(new Header(R.drawable.ic_help_outline_white_24dp, R.string.prefs_category_about, R.string.prefs_category_about_summary));
        headers.add(new Header(R.drawable.ic_exit_to_app_white_24dp, R.string.logout, R.string.logout_summary));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_pref_header, parent, false), eventListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.onBind(headers.get(position));
    }

    @Override
    public int getItemCount() {
        return headers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected EventListener eventListener;
        protected ImageView imageIcon;
        protected TextView textTitle;
        protected TextView textSummary;
        private Header header;

        public ViewHolder(View itemView, EventListener listener) {
            super(itemView);
            this.eventListener = listener;

            imageIcon = (ImageView) itemView.findViewById(R.id.image_icon);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textSummary = (TextView) itemView.findViewById(R.id.text_summary);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventListener.onClickHeader(getAdapterPosition());
                }
            });

        }

        public void onBind(Header header) {

            this.header = header;

            imageIcon.setImageResource(header.getIconResourceId());
            textTitle.setText(header.getTitleResourceId());
            textSummary.setText(header.getSummaryResourceId());

        }

    }

    public interface EventListener {
        void onClickHeader(int position);
    }

}
