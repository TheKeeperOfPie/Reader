/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.User;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import rx.Observer;

/**
 * Created by TheKeeperOfPie on 6/24/2015.
 */
public class ControllerUser {

    private static final String TAG = ControllerUser.class.getCanonicalName();
    private User user;
    private Account account;
    private Set<Listener> listeners = new HashSet<>();

    @Inject AccountManager accountManager;
    @Inject Reddit reddit;

    public ControllerUser() {
        CustomApplication.getComponentMain().inject(this);
        user = new User();
    }

    public User getUser() {
        return user;
    }

    public void reloadUser() {
        reddit.me()
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String response) {
                        try {
                            user = User.fromJson(ComponentStatic.getObjectMapper().readValue(response, JsonNode.class));
                            for (Listener listener : listeners) {
                                listener.onUserLoaded(user);
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public boolean hasUser() {
        return account != null;
    }

    public void clearAccount() {
        account = null;
        user = new User();
        for (Listener listener : listeners) {
            listener.onUserLoaded(null);
        }
    }

    public void setAccount(Account accountUser) {
        boolean accountFound = false;
        Account[] accounts = accountManager.getAccountsByType(Reddit.ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(accountUser.name)) {
                this.account = account;
                accountFound = true;
                reloadUser();
                break;
            }
        }

        user = new User();

        if (!accountFound) {
            account = null;
        }
        else {
            user.setName(account.name);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void onUserLoaded(@Nullable User user);
    }
}
