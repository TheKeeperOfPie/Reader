/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.TagHandlerReddit;
import com.winsonchiu.reader.data.reddit.Thing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by TheKeeperOfPie on 2/6/2016.
 */
public class UtilsReddit {

    private static final String TAG = UtilsReddit.class.getCanonicalName();

    public static Uri getUserAuthUri(UUID state) {
        Uri uri = new Uri.Builder()
                .scheme("https")
                .authority("www.reddit.com")
                .path("/api/v1/authorize.compact")
                .appendQueryParameter("client_id", ApiKeys.REDDIT_CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("state", String.valueOf(state))
                .appendQueryParameter("redirect_uri", "com.winsonchiu.reader://reddit")
                .appendQueryParameter("duration", "permanent")
                .appendQueryParameter("scope", Reddit.AUTH_SCOPES)
                .build();
        Log.d(TAG, "getUserAuthUri() called with: " + "uri = [" + uri + "]");
        return uri;
    }

    @Nullable
    public static String parseRawSubredditString(@Nullable String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        input = input.replaceAll("\\s", "");

        if (input.startsWith("/r/")) {
            input = input.substring(3);
        }
        else if (input.startsWith("r/")) {
            input = input.substring(2);
        }
        else if (input.startsWith("/")) {
            input = input.substring(1);
        }

        if (!input.matches("^[A-z]+$")) {
            return null;
        }

        return input;
    }

    @Nullable
    public static String parseSubredditUrlPart(String input) {
        String subreddit = parseRawSubredditString(input);
        if (TextUtils.isEmpty(subreddit)) {
            return null;
        }
        else {
            return "/r/" + subreddit + "/";
        }
    }

    public static boolean isAll(Subreddit subreddit) {
        return "/r/all/".equalsIgnoreCase(subreddit.getUrl());
    }

    public static boolean isMultiple(Subreddit subreddit) {
        return subreddit.getUrl().contains("+");
    }

    public static CharSequence getFormattedHtml(String html) {

        if (TextUtils.isEmpty(html)) {
            return new SpannedString("");
        }

        html = html.replaceAll("\n", "<br>");

        CharSequence sequence = Html.fromHtml(Html.fromHtml(html).toString(), null, new TagHandlerReddit());

        // Trims leading and trailing whitespace
        int start = 0;
        int end = sequence.length();
        while (start < end && Character.isWhitespace(sequence.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
            end--;
        }


        return sequence.subSequence(start, end);
    }

    public static Intent getShareIntentLinkSource(Link link) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
        intent.putExtra(Intent.EXTRA_TEXT, link.getUrl());
        return intent;
    }

    public static Intent getShareIntentLinkComments(Link link) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, link.getTitle());
        intent.putExtra(Intent.EXTRA_TEXT, Reddit.BASE_URL + link.getPermalink());
        return intent;
    }

    private static void launchRedditPage(Context context, String url) {
        Intent intent = new Intent(context, ActivityMain.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(ActivityMain.REDDIT_PAGE, url);
        context.startActivity(intent);
    }

    public static void launchScreenProfile(Context context, Link link) {
        launchRedditPage(context, "https://reddit.com/user/" + link.getAuthor());
    }

    public static void launchScreenSubreddit(Context context, Link link) {
        launchRedditPage(context, "https://reddit.com/r/" + link.getSubreddit());
    }

    public static List<Thing> parseJson(JsonNode node) {
        switch (UtilsJson.getString(node.get("kind"))) {

            // TODO: Add cases for all ID36s and fix adding Comments

            case "more":
                return UtilsList.of(Comment.fromJson(node, 0));
            case "t1":
                List<Thing> list = new ArrayList<>();
                Comment.addAllFromJson(list, node, 0);
                return list;
            case "t3":
                return UtilsList.of(Link.fromJson(node));
            case "t4":
                return UtilsList.of(Message.fromJson(node));
            case "t5":
                return UtilsList.of(Subreddit.fromJson(node));

        }

        return new ArrayList<>();
    }
}
