/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.search;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.utils.AnimationUtils;
import com.winsonchiu.reader.utils.CustomColorFilter;

import java.util.Date;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class AdapterSearchSubreddits extends RecyclerView.Adapter<AdapterSearchSubreddits.ViewHolder> {

    private static final String TAG = AdapterSearchSubreddits.class.getCanonicalName();
    private RecyclerView.LayoutManager layoutManager;
    private ControllerSearchBase controllerSearchBase;
    private ViewHolder.EventListener eventListener;

    public AdapterSearchSubreddits(Activity activity,
            ControllerSearchBase controllerSearchBase,
            ViewHolder.EventListener eventListener) {
        this.controllerSearchBase = controllerSearchBase;
        this.eventListener = eventListener;
        this.layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_subreddit, parent, false), eventListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.onBind(controllerSearchBase.getSubreddit(position));
    }

    @Override
    public int getItemCount() {
        return controllerSearchBase.getSubredditCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected EventListener eventListener;
        protected ImageButton buttonReorder;
        protected TextView textName;
        protected TextView textTitle;
        protected TextView textDescription;
        protected TextView textInfo;
        protected RelativeLayout layoutContainerExpand;
        private Subreddit subreddit;

        public ViewHolder(View itemView, final EventListener eventListener) {
            super(itemView);
            this.eventListener = eventListener;

            buttonReorder = (ImageButton) itemView.findViewById(R.id.button_reorder);
            textName = (TextView) itemView.findViewById(R.id.text_name);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDescription = (TextView) itemView.findViewById(R.id.text_description);
            textDescription.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(R.id.layout_container_expand);

            buttonReorder.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        eventListener.onStartDrag(ViewHolder.this);
                    }
                    return false;
                }
            });

            textDescription.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    MotionEvent newEvent = MotionEvent.obtain(event);
                    newEvent.offsetLocation(v.getLeft(), v.getTop());
                    ViewHolder.this.itemView.onTouchEvent(newEvent);
                    newEvent.recycle();
                    return false;
                }
            });
            textName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder.this.eventListener.onClickSubreddit(subreddit);
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AnimationUtils.animateExpand(layoutContainerExpand, 1f, null);
                }
            });

            if (eventListener.supportsDrag()) {
                TypedArray typedArray = itemView.getContext().getTheme().obtainStyledAttributes(
                        new int[] {R.attr.colorIconFilter});
                CustomColorFilter colorFilterIcon = new CustomColorFilter(typedArray.getColor(0, 0xFFFFFFFF), PorterDuff.Mode.MULTIPLY);
                typedArray.recycle();
                buttonReorder.setColorFilter(colorFilterIcon);
                buttonReorder.setVisibility(View.VISIBLE);
            }

        }

        public void onBind(Subreddit subreddit) {
            this.subreddit = subreddit;

            layoutContainerExpand.setVisibility(View.GONE);

            textName.setText(subreddit.getDisplayName());
            textTitle.setText(subreddit.getTitle());

            if ("null".equals(subreddit.getPublicDescriptionHtml())) {
                textDescription.setVisibility(View.GONE);
            }
            else {
                textDescription.setText(subreddit.getPublicDescriptionHtml());
            }

            textInfo.setText(subreddit.getSubscribers() + " subscribers\n" +
                    "created " + new Date(subreddit.getCreatedUtc()));

        }

        public interface EventListener {
            void onClickSubreddit(Subreddit subreddit);
            boolean supportsDrag();
            void onStartDrag(ViewHolder viewHolder);
        }

    }

}
