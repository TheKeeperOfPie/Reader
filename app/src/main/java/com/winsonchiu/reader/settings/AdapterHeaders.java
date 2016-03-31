/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.utils.ViewHolderBase;

/**
 * Created by TheKeeperOfPie on 7/2/2015.
 */
public class AdapterHeaders extends RecyclerView.Adapter<AdapterHeaders.ViewHolder> {

    private PorterDuffColorFilter colorFilterIcon;
    private EventListener eventListener;
    private Header[] headers;

    public AdapterHeaders(Activity activity, EventListener eventListener) {
        super();
        this.eventListener = eventListener;
        headers = Header.values();

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(new int[] {R.attr.colorIconFilter});
        int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
        typedArray.recycle();

        colorFilterIcon = new PorterDuffColorFilter(colorIconFilter, PorterDuff.Mode.MULTIPLY);

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_pref_header, parent, false), eventListener, colorFilterIcon);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
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

        public ViewHolder(View itemView, EventListener listener, PorterDuffColorFilter colorFilterIcon) {
            super(itemView);
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
