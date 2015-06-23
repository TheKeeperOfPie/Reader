package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.winsonchiu.reader.data.Link;

/**
 * Created by TheKeeperOfPie on 6/21/2015.
 */
public class AdapterSearchLinkList extends AdapterLinkList {

    public AdapterSearchLinkList(Activity activity,
            ControllerLinksBase controllerLinks,
            ControllerLinks.LinkClickListener listener) {
        super(activity, controllerLinks, listener);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return super.onCreateViewHolder(viewGroup, viewType);
        }

        ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false), this) {
            @Override
            public void onClickThumbnail() {
                InputMethodManager inputManager = (InputMethodManager) activity
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(itemView.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                super.onClickThumbnail();
            }
        };
        viewHolders.add(viewHolder);
        return viewHolder;
    }
}
