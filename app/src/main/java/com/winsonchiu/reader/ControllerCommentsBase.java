package com.winsonchiu.reader;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Thing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public interface ControllerCommentsBase extends ControllerLinksBase {

    void insertComments(Comment moreComment, Listing listing);
    void insertComment(int commentIndex, Comment comment);
    void removeComment(int commentIndex);

    /**
     * Toggles children of comment
     *
     * @param position
     * @return true if comment is now expanded, false if collapsed
     */
    boolean toggleComment(int position);
    void expandComment(int position);
    void collapseComment(int position);
    Comment get(int position);
    boolean voteComment(final AdapterCommentList.ViewHolderComment viewHolder, final int vote);
    int getIndentWidth(Comment comment);
    void loadMoreComments(final Comment moreComment);
    boolean isCommentExpanded(int position);
    Link getMainLink();
}