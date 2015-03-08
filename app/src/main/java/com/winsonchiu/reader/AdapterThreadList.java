package com.winsonchiu.reader;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterThreadList extends RecyclerView.Adapter<AdapterThreadList.ViewHolder> {

    private ThreadClickListener listener;

    public AdapterThreadList(ThreadClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_thread, viewGroup));
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {

    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected ImageView imageThreadPreview;
        protected TextView textThreadTitle;
        protected TextView textThreadInfo;
        protected ImageButton buttonComments;

        public ViewHolder(View itemView) {
            super(itemView);
            this.imageThreadPreview = (ImageView) itemView.findViewById(R.id.image_thread_preview);
            this.textThreadTitle = (TextView) itemView.findViewById(R.id.text_thread_title);
            this.textThreadInfo = (TextView) itemView.findViewById(R.id.text_thread_info);
            this.buttonComments = (ImageButton) itemView.findViewById(R.id.button_comments);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onThreadClick();
                }
            });
            this.imageThreadPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onImagePreviewClick();
                }
            });
        }
    }

    public interface ThreadClickListener {

        void onThreadClick();
        void onImagePreviewClick();

    }

}
