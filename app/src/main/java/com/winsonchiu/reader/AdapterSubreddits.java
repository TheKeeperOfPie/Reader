package com.winsonchiu.reader;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Subreddit;

/**
 * Created by TheKeeperOfPie on 5/17/2015.
 */
public class AdapterSubreddits extends RecyclerView.Adapter<AdapterSubreddits.ViewHolder>
        implements ControllerSubreddits.ListenerCallback {

    private static final String TAG = AdapterSubreddits.class.getCanonicalName();
    private RecyclerView.LayoutManager layoutManager;
    private Activity activity;
    private ControllerSubreddits controllerSubreddits;
    private ControllerSubreddits.SubredditListener listener;

    public AdapterSubreddits(Activity activity, ControllerSubreddits controllerSubreddits, ControllerSubreddits.SubredditListener subredditListener) {
        this.activity = activity;
        this.controllerSubreddits = controllerSubreddits;
        this.listener = subredditListener;
        this.layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_subreddit, parent, false), this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        holder.onBind(controllerSubreddits.getSubreddit(position));

    }

    @Override
    public int getItemCount() {
        return controllerSubreddits.getItemCount();
    }

    @Override
    public ControllerSubreddits.SubredditListener getListener() {
        return listener;
    }

    @Override
    public ControllerSubreddits getController() {
        return controllerSubreddits;
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected TextView textName;
        protected TextView textTitle;
        protected TextView textDescription;
        protected TextView textInfo;
        protected ImageButton buttonOpen;
        private ControllerSubreddits.ListenerCallback callback;

        public ViewHolder(View itemView, final ControllerSubreddits.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

            textName = (TextView) itemView.findViewById(R.id.text_name);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textDescription = (TextView) itemView.findViewById(R.id.text_description);
            textDescription.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            buttonOpen = (ImageButton) itemView.findViewById(R.id.button_open);

            buttonOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.getListener().onClickSubreddit(callback.getController().getSubreddit(getAdapterPosition()));
                }
            });

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    AnimationUtils.animateExpand(textDescription, 1.0f);
                    AnimationUtils.animateExpand(textInfo, 1.0f);
                }
            };

            itemView.setOnClickListener(onClickListener);

        }

        public void onBind(Subreddit subreddit) {

            textDescription.setVisibility(View.GONE);
            textInfo.setVisibility(View.GONE);

            textName.setText("/r/" + subreddit.getDisplayName());
            textTitle.setText(subreddit.getTitle());

            if ("null".equals(subreddit.getPublicDescriptionHtml())) {
                textDescription.setText("");
            }
            else {
                // TODO: Move all instances to Reddit class
                String html = subreddit.getPublicDescriptionHtml();
                html = Html.fromHtml(html.trim())
                        .toString();

                CharSequence sequence = Html.fromHtml(html);

                // Trims leading and trailing whitespace
                int start = 0;
                int end = sequence.length();
                while (start < end && Character.isWhitespace(sequence.charAt(start))) {
                    start++;
                }
                while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
                    end--;
                }
                sequence = sequence.subSequence(start, end);

                textDescription.setText(sequence);
            }

            textInfo.setText(subreddit.getSubscribers() + " subscribers");

        }
    }

}
