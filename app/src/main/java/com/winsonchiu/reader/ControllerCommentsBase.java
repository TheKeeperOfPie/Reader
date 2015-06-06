package com.winsonchiu.reader;

import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;

/**
 * Created by TheKeeperOfPie on 3/21/2015.
 */
public interface ControllerCommentsBase extends ControllerLinksBase {

    void insertComments(Comment moreComment, Listing listing);
    void insertComment(Comment comment);
    void deleteComment(Comment comment);

    /**
     * Toggles children of comment
     *
     * @param position
     * @return true if comment is now expanded, false if collapsed
     */
    boolean toggleComment(int position);
    void expandComment(int position);
    void collapseComment(int position);
    Comment getComment(int position);
    boolean voteComment(final AdapterCommentList.ViewHolderComment viewHolder, final int vote);
    int getIndentWidth(Comment comment);
    void loadMoreComments(final Comment moreComment);
    boolean isCommentExpanded(int position);
    Link getMainLink();
}