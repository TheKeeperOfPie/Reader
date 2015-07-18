/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.reddit.Reddit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FragmentNewMessage extends FragmentBase {

    public static final String TAG = FragmentNewMessage.class.getCanonicalName();
    private Reddit reddit;

    private Toolbar toolbar;
    private EditText editTextRecipient;
    private EditText editTextSubject;
    private EditText editTextMessage;
    private FloatingActionButton floatingActionButton;
    private static final long DURATION_SUBMIT = 400;
    private static final long DURATION_SUBMIT_DELAY = 125;

    private RelativeLayout layoutCaptcha;
    private String captchaId;
    private ImageView imageCaptcha;
    private EditText editCaptcha;
    private ImageButton buttonCaptchaRefresh;
    private ProgressBar progressSubmit;

    private FragmentListenerBase mListener;
    private Activity activity;

    public static FragmentNewMessage newInstance() {
        FragmentNewMessage fragment = new FragmentNewMessage();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FragmentNewMessage() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_new_message, container, false);

        reddit = Reddit.getInstance(activity);

        editTextRecipient = (EditText) view.findViewById(R.id.edit_recipient);
        editTextSubject = (EditText) view.findViewById(R.id.edit_subject);
        editTextMessage = (EditText) view.findViewById(R.id.edit_message);

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

        floatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab_submit_post);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitMessage();
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

        return view;
    }


    private void submitMessage() {

        String recipient = editTextRecipient.getText().toString().replaceAll("\\s", "");

        if (TextUtils.isEmpty(recipient)) {
            Toast.makeText(activity, getString(R.string.empty_recipient), Toast.LENGTH_SHORT).show();
            return;
        }

        if (recipient.startsWith("/u/")) {
            recipient = recipient.substring(3);
        }

        progressSubmit.setIndeterminate(true);
        progressSubmit.setVisibility(View.VISIBLE);

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");
        params.put("subject", editTextSubject.getText().toString());
        params.put("text", editTextMessage.getText().toString());
        params.put("to", recipient);
        if (!TextUtils.isEmpty(captchaId)) {
            params.put("iden", captchaId);
            params.put("captcha", editCaptcha.getText().toString());
        }

        reddit.loadPost(Reddit.OAUTH_URL + "/api/compose", new Response.Listener<String>() {
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
                Toast.makeText(activity, getString(R.string.error_sending_message), Toast.LENGTH_LONG)
                        .show();
            }
        }, params, 0);
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
                    Picasso.with(activity).load(Reddit.BASE_URL + "/captcha/" + captchaId + ".png").resize(getResources().getDisplayMetrics().widthPixels, 0).into(
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        try {
            mListener = (FragmentListenerBase) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
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
