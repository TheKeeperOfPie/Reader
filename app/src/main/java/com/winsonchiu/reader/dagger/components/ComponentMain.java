/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.dagger.components;

import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.FragmentNewMessage;
import com.winsonchiu.reader.auth.ActivityLogin;
import com.winsonchiu.reader.comments.ControllerComments;
import com.winsonchiu.reader.dagger.modules.ModuleApi;
import com.winsonchiu.reader.dagger.modules.ModuleContext;
import com.winsonchiu.reader.dagger.modules.ModuleHistory;
import com.winsonchiu.reader.dagger.modules.ModuleReddit;
import com.winsonchiu.reader.history.ControllerHistory;
import com.winsonchiu.reader.inbox.ControllerInbox;
import com.winsonchiu.reader.inbox.Receiver;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.search.ControllerSearch;
import com.winsonchiu.reader.settings.FragmentBehavior;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by TheKeeperOfPie on 12/12/2015.
 */
@Singleton
@Component(
        modules = {
                ModuleContext.class,
                ModuleApi.class,
                ModuleHistory.class,
        }
)
public interface ComponentMain {
    void inject(ActivityLogin target);
    void inject(AdapterLink.ViewHolderBase target);
    void inject(FragmentBehavior target);
    void inject(FragmentNewMessage target);
    void inject(ControllerComments target);
    void inject(ControllerHistory target);
    void inject(ControllerInbox target);
    void inject(ControllerLinks target);
    void inject(ControllerProfile target);
    void inject(ControllerSearch target);
    void inject(ControllerUser target);
    void inject(Receiver target);

    ComponentActivity plus(ModuleReddit module);
}