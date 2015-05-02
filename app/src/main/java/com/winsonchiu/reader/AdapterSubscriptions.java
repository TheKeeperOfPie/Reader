package com.winsonchiu.reader;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.winsonchiu.reader.data.Subreddit;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 5/1/2015.
 */
public class AdapterSubscriptions extends RecyclerView.Adapter<AdapterSubscriptions.ViewHolder> {

    private List<Subreddit> subreddits;

    public AdapterSubscriptions() {
        subreddits = new ArrayList<>();
    }

    public void setSubreddits(List<Subreddit> subreddits) {
        this.subreddits = subreddits;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_subreddit, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Subreddit subreddit = subreddits.get(position);
        holder.textSubredditTitle.setText(subreddit.getDisplayName());
    }

    @Override
    public int getItemCount() {
        return subreddits.size();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        protected TextView textSubredditTitle;
        protected TextView textSubredditInfo;

        public ViewHolder(View itemView) {
            super(itemView);
            textSubredditTitle = (TextView) itemView.findViewById(R.id.text_subreddit_title);
            textSubredditInfo = (TextView) itemView.findViewById(R.id.text_subreddit_info);
        }
    }

}
