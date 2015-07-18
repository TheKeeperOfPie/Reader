/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FragmentNewPost extends FragmentBase {

    public static final String USER = "User";
    public static final String SUBREDDIT = "Subreddit";
    public static final String POST_TYPE = "PostType";
    public static final String SUBMIT_TEXT_HTML = "SubmitTextHtml";
    public static final String IS_EDIT = "isEdit";
    public static final String EDIT_ID = "editId";
    public static final String TAG = FragmentNewPost.class.getCanonicalName();
    private static final long DURATION_SUBMIT = 400;
    private static final long DURATION_SUBMIT_DELAY = 125;
    private Toolbar toolbar;
    private TextView textInfo;
    private TextView textSubmit;
    private EditText editTextTitle;
    private EditText editTextBody;
    private FloatingActionButton floatingActionButton;
    private Reddit reddit;

    private RelativeLayout layoutCaptcha;
    private String captchaId;
    private ImageView imageCaptcha;
    private EditText editCaptcha;
    private ImageButton buttonCaptchaRefresh;
    private ProgressBar progressSubmit;

    private FragmentListenerBase mListener;
    private Activity activity;

    public static FragmentNewPost newInstance(String user, String subredditUrl, String postType, String submitTextHtml) {
        FragmentNewPost fragment = new FragmentNewPost();
        Bundle args = new Bundle();
        args.putString(USER, user);
        args.putString(SUBREDDIT, subredditUrl);
        args.putString(POST_TYPE, postType);
        args.putString(SUBMIT_TEXT_HTML, submitTextHtml);
        fragment.setArguments(args);
        return fragment;
    }

    public static FragmentNewPost newInstanceEdit(String user, Link link) {
        FragmentNewPost fragment = new FragmentNewPost();
        Bundle args = new Bundle();
        args.putString(USER, user);
        args.putString(SUBREDDIT, "/r/" + link.getSubreddit());
        args.putBoolean(IS_EDIT, true);
        args.putString(EDIT_ID, link.getName());
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentNewPost() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_post, container, false);

        reddit = Reddit.getInstance(activity);

        textInfo = (TextView) view.findViewById(R.id.text_info);
        textSubmit = (TextView) view.findViewById(R.id.text_submit);
        editTextTitle = (EditText) view.findViewById(R.id.edit_title);
        editTextBody = (EditText) view.findViewById(R.id.edit_body);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.new_post));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNavigationBackClick();
            }
        });

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorIconFilter});
        int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
        typedArray.recycle();

        PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(colorIconFilter,
                PorterDuff.Mode.MULTIPLY);
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilter);

        textInfo.setText(getString(R.string.submitting_to) + " " + getArguments()
                .getString(SUBREDDIT) + " " + getString(R.string.as) + " /u/" + getArguments()
                .getString(USER));

        String submitTextHtml = getArguments().getString(SUBMIT_TEXT_HTML);
        Log.d(TAG, "submitTextHtml: " + submitTextHtml);
        if (TextUtils.isEmpty(submitTextHtml) || "null".equals(submitTextHtml)) {
            textSubmit.setVisibility(View.GONE);
        }
        else {
            textSubmit.setText(Reddit.getFormattedHtml(submitTextHtml));
        }
        textSubmit.setMovementMethod(LinkMovementMethod.getInstance());

        if (Reddit.POST_TYPE_LINK.equals(getArguments().getString(POST_TYPE))) {
            editTextBody.setHint("URL");
        }
        else {
            editTextBody.setHint("Text");
        }

        floatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab_submit_post);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getArguments().getBoolean(IS_EDIT, false)) {
                    submitEdit();
                }
                else {
                    submitNew();
                }
            }
        });

        progressSubmit = (ProgressBar) view.findViewById(R.id.progress_submit);

        layoutCaptcha = (RelativeLayout) view.findViewById(R.id.layout_captcha);
        imageCaptcha = (ImageView) view.findViewById(R.id.image_captcha);
        editCaptcha = (EditText) view.findViewById(R.id.edit_captcha);
        buttonCaptchaRefresh = (ImageButton) view.findViewById(R.id.button_captcha_refresh);
        buttonCaptchaRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCaptcha();
            }
        });

        if (getArguments().getBoolean(IS_EDIT, false)) {
            loadEditValues();
        }
        else {
            reddit.loadGet(Reddit.OAUTH_URL + "/api/needs_captcha",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if ("true".equalsIgnoreCase(response)) {
                                layoutCaptcha.setVisibility(View.VISIBLE);
                                loadCaptcha();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }, 0);
        }

        return view;
    }

    private void loadCaptcha() {

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");

        reddit.loadPost(Reddit.OAUTH_URL + "/api/new_captcha", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    captchaId = jsonObject.getJSONObject("json").getJSONObject("data").getString(
                            "iden");
                    Log.d(TAG, "captchaId: " + captchaId);
                    Picasso.with(activity)
                            .load(Reddit.BASE_URL + "/captcha/" + captchaId + ".png")
                            .resize(getResources().getDisplayMetrics().widthPixels, 0).into(
                            imageCaptcha);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }, params, 0);
    }

    private void loadEditValues() {
        reddit.loadGet(Reddit.OAUTH_URL + "/api/info?id=" + getArguments().getString(EDIT_ID),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Listing listing = Listing.fromJson(new JSONObject(response));
                            Link link = (Link) listing.getChildren().get(0);
                            editTextTitle.setText(link.getTitle());
                            editTextTitle.setClickable(false);
                            editTextTitle.setFocusable(false);
                            editTextTitle.setFocusableInTouchMode(false);
                            editTextTitle.setEnabled(false);

                            editTextBody.setText(link.getSelfText());
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }, 0);
    }

    private void submitEdit() {

        progressSubmit.setIndeterminate(true);
        progressSubmit.setVisibility(View.VISIBLE);

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");
        params.put("text", editTextBody.getText().toString());
        params.put("thing_id", getArguments().getString(EDIT_ID));

        reddit.loadPost(Reddit.OAUTH_URL + "/api/editusertext", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                progressSubmit.setProgress(0);
                progressSubmit.setIndeterminate(false);

                Animation animation = new Animation() {
                    @Override
                    protected void applyTransformation(final float interpolatedTime,
                            Transformation t) {
                        super.applyTransformation(interpolatedTime, t);
                        progressSubmit.setProgress((int) (interpolatedTime * 100));
                        Log.d(TAG, "progress: " + ((int) (interpolatedTime * 100)));
                    }
                };
                animation.setDuration(DURATION_SUBMIT);
                animation.setInterpolator(new FastOutSlowInInterpolator());
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        progressSubmit.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mListener.getControllerLinks().reloadAllLinks(false);
                                mListener.onNavigationBackClick();
                            }
                        }, DURATION_SUBMIT_DELAY);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                progressSubmit.startAnimation(animation);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, getString(R.string.error_submitting_post),
                        Toast.LENGTH_LONG)
                        .show();
                progressSubmit.setVisibility(View.GONE);
            }
        }, params, 0);
    }

    private void submitNew() {
        String text = editTextBody.getText().toString();

        if (TextUtils.isEmpty(editTextTitle.getText().toString())) {
            Toast.makeText(activity, getString(R.string.empty_title), Toast.LENGTH_LONG)
                    .show();
            return;
        }
        else if (Reddit.POST_TYPE_LINK.equals(getArguments().getString(POST_TYPE)) && !URLUtil
                .isValidUrl(text)) {

            text = "http://" + text;

            if (!URLUtil.isValidUrl(text)) {
                Toast.makeText(activity, getString(R.string.invalid_url),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        progressSubmit.setIndeterminate(true);
        progressSubmit.setVisibility(View.VISIBLE);

        Map<String, String> params = new HashMap<>();
        params.put("kind", getArguments().getString(POST_TYPE).toLowerCase());
        params.put("api_type", "json");
        params.put("resubmit", "true");
        params.put("sendreplies", "true");
        params.put("then", "comments");
        params.put("extension", "json");
        params.put("sr", getArguments().getString(SUBREDDIT));
        params.put("title", editTextTitle.getText().toString());
        if (Reddit.POST_TYPE_LINK.equalsIgnoreCase(getArguments().getString(POST_TYPE))) {
            params.put("url", text);
        }
        else {
            params.put("text", text);
        }
        if (layoutCaptcha.isShown()) {
            params.put("iden", captchaId);
            params.put("captcha", editCaptcha.getText().toString());
        }

        reddit.loadPost(Reddit.OAUTH_URL + "/api/submit", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Submit new response: " + response);

                try {
                    JSONObject jsonObject = new JSONObject(response).getJSONObject("json");
                    String error = jsonObject.getJSONArray("errors").optString(
                            0);
                    if (!TextUtils.isEmpty(error)) {

                        String captcha = jsonObject.optString("captcha");

                        if (!TextUtils.isEmpty(captcha)) {
                            captchaId = captcha;
                            editCaptcha.setText("");
                            Picasso.with(activity)
                                    .load(Reddit.BASE_URL + "/captcha/" + captchaId + ".png")
                                    .resize(getResources().getDisplayMetrics().widthPixels, 0).into(
                                    imageCaptcha);
                        }

                        Toast.makeText(activity, getString(R.string.error) + ": " + error, Toast.LENGTH_LONG)
                                .show();
                        progressSubmit.setVisibility(View.GONE);
                        return;
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

                progressSubmit.setProgress(0);
                progressSubmit.setIndeterminate(false);

                Animation animation = new Animation() {
                    @Override
                    protected void applyTransformation(final float interpolatedTime, Transformation t) {
                        super.applyTransformation(interpolatedTime, t);
                        progressSubmit.setProgress((int) (interpolatedTime * 100));
                        Log.d(TAG, "progress: " + ((int) (interpolatedTime * 100)));
                    }
                };
                animation.setDuration(DURATION_SUBMIT);
                animation.setInterpolator(new FastOutSlowInInterpolator());
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        progressSubmit.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mListener.getControllerLinks().reloadAllLinks(false);
                                mListener.onNavigationBackClick();
                            }
                        }, DURATION_SUBMIT_DELAY);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                progressSubmit.startAnimation(animation);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(activity, getString(R.string.error_submitting_post), Toast.LENGTH_LONG)
                        .show();
                progressSubmit.setVisibility(View.GONE);
            }
        }, params, 0);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FragmentListenerBase");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
        mListener = null;
    }

    @Override
    public boolean navigateBack() {
        return true;
    }

}
