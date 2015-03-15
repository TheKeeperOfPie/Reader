package com.winsonchiu.reader;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/13/2015.
 */
public class AdapterNavDrawer extends RecyclerView.Adapter<AdapterNavDrawer.ViewHolder> {

    private List<NavItem> listNavItem;
    private OnEntryClickListener listener;

    public AdapterNavDrawer(OnEntryClickListener listener) {
        this.listener = listener;
        listNavItem = new ArrayList<>();
    }

    public void addItem(NavItem item) {
        listNavItem.add(item);
        notifyItemInserted(listNavItem.size() - 1);
    }

    @Override
    public AdapterNavDrawer.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_nav, parent, false));
    }

    @Override
    public void onBindViewHolder(AdapterNavDrawer.ViewHolder holder, int position) {
        NavItem navItem = listNavItem.get(position);
        holder.imageIcon.setImageResource(navItem.getDrawable());
        holder.textTitle.setText(navItem.getTitle());
    }

    @Override
    public int getItemCount() {
        return listNavItem.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        protected ImageView imageIcon;
        protected TextView textTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            this.imageIcon = (ImageView) itemView.findViewById(R.id.image_icon);
            this.textTitle = (TextView) itemView.findViewById(R.id.text_title);
            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(listNavItem.get(getPosition()), getPosition());
                }
            });
        }
    }

    public interface OnEntryClickListener {
        void onClick(NavItem navItem, int position);
    }

}
