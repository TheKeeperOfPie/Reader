/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.components;

import com.winsonchiu.reader.FragmentData;
import com.winsonchiu.reader.FragmentNewPost;
import com.winsonchiu.reader.MainActivity;
import com.winsonchiu.reader.comments.FragmentReply;
import com.winsonchiu.reader.dagger.ActivityScope;
import com.winsonchiu.reader.dagger.modules.ModuleReddit;
import com.winsonchiu.reader.links.FragmentThreadList;
import com.winsonchiu.reader.profile.FragmentProfile;
import com.winsonchiu.reader.search.FragmentSearch;

import dagger.Subcomponent;

/**
 * Created by TheKeeperOfPie on 12/13/2015.
 */
@ActivityScope
@Subcomponent(
        modules = {
                ModuleReddit.class
        }
)
public interface ComponentActivity {
    void inject(MainActivity target);
    void inject(FragmentData target);
    void inject(FragmentSearch target);
    void inject(FragmentNewPost target);
    void inject(FragmentReply target);
    void inject(FragmentThreadList target);
    void inject(FragmentProfile target);
}
