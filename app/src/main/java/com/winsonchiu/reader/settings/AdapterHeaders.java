/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.app.Activity;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterBase;
import com.winsonchiu.reader.adapter.AdapterCallback;
import com.winsonchiu.reader.theme.Themer;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 7/2/2015.
 */
public class AdapterHeaders extends AdapterBase<AdapterHeaders.ViewHolder> {

    private Themer themer;
    private EventListener eventListener;
    private Header[] headers;

    public AdapterHeaders(Activity activity, EventListener eventListener) {
        super();
        this.eventListener = eventListener;
        headers = Header.values();
        themer = new Themer(activity);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_pref_header, parent, false),
                adapterCallback,
                eventListener,
                themer.getColorFilterIcon());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.onBind(headers[position]);
    }

    @Override
    public int getItemCount() {
        return headers.length;
    }

    public static class ViewHolder extends ViewHolderBase {

        protected EventListener eventListener;
        protected ImageView imageIcon;
        protected TextView textTitle;
        protected TextView textSummary;
        private Header header;

        public ViewHolder(View itemView, AdapterCallback adapterCallback, EventListener listener, PorterDuffColorFilter colorFilterIcon) {
            super(itemView, adapterCallback);
            this.eventListener = listener;

            imageIcon = (ImageView) itemView.findViewById(R.id.image_icon);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textSummary = (TextView) itemView.findViewById(R.id.text_summary);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventListener.onClickHeader(header);
                }
            });

            imageIcon.setColorFilter(colorFilterIcon);

        }

        public void onBind(Header header) {

            this.header = header;

            imageIcon.setImageResource(header.getIconResourceId());
            textTitle.setText(header.getTitleResourceId());
            textSummary.setText(header.getSummaryResourceId());

        }

    }

    public interface EventListener {
        void onClickHeader(Header header);
    }

}
