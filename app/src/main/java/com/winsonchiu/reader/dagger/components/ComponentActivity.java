/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.components;

import com.winsonchiu.reader.FragmentNewMessage;
import com.winsonchiu.reader.FragmentNewPost;
import com.winsonchiu.reader.MainActivity;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.comments.FragmentComments;
import com.winsonchiu.reader.comments.FragmentReply;
import com.winsonchiu.reader.dagger.ScopeActivity;
import com.winsonchiu.reader.dagger.modules.ModuleReddit;
import com.winsonchiu.reader.history.FragmentHistory;
import com.winsonchiu.reader.inbox.FragmentInbox;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.FragmentThreadList;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.profile.FragmentProfile;
import com.winsonchiu.reader.search.FragmentSearch;

import dagger.Subcomponent;

/**
 * Created by TheKeeperOfPie on 12/13/2015.
 */
@ScopeActivity
@Subcomponent(
        modules = {
                ModuleReddit.class
        }
)
public interface ComponentActivity {
    void inject(MainActivity target);
    void inject(FragmentSearch target);
    void inject(FragmentNewPost target);
    void inject(FragmentReply target);
    void inject(FragmentThreadList target);
    void inject(FragmentProfile target);
    void inject(ControllerProfile target);
    void inject(AdapterLink target);
    void inject(FragmentNewMessage target);
    void inject(AdapterCommentList target);
    void inject(FragmentComments target);
    void inject(FragmentInbox target);
    void inject(FragmentHistory target);
}
