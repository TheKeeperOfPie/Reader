/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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

/**
 * Created by TheKeeperOfPie on 6/7/2015.
 */
public class ActivityNewPost extends AppCompatActivity {


    public static final int REQUEST_CODE = 0;
    public static final String USER = "User";
    public static final String SUBREDDIT = "Subreddit";
    public static final String POST_TYPE = "PostType";
    public static final String SUBMIT_TEXT_HTML = "SubmitTextHtml";
    public static final String IS_EDIT = "isEdit";
    public static final String EDIT_ID = "editId";
    private static final String TAG = ActivityNewPost.class.getCanonicalName();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        switch (sharedPreferences.getString(AppSettings.PREF_THEME, "Dark")) {
            case AppSettings.THEME_DARK:
                setTheme(R.style.AppDarkTheme);
                break;
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.AppLightTheme);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.AppBlackTheme);
                break;
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_post);

        reddit = Reddit.getInstance(this);

        textInfo = (TextView) findViewById(R.id.text_info);
        textSubmit = (TextView) findViewById(R.id.text_submit);
        editTextTitle = (EditText) findViewById(R.id.edit_title);
        editTextBody = (EditText) findViewById(R.id.edit_body);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.new_post));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TypedArray typedArray = getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorIconFilter});
        int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
        typedArray.recycle();

        PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(colorIconFilter,
                PorterDuff.Mode.MULTIPLY);
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilter);

        setSupportActionBar(toolbar);

        textInfo.setText(getString(R.string.submitting_to) + " " + getIntent()
                .getStringExtra(SUBREDDIT) + " " + getString(R.string.as) + " /u/" + getIntent()
                .getStringExtra(USER));

        String submitTextHtml = getIntent().getStringExtra(SUBMIT_TEXT_HTML);
        Log.d(TAG, "submitTextHtml: " + submitTextHtml);
        if (TextUtils.isEmpty(submitTextHtml) || "null".equals(submitTextHtml)) {
            textSubmit.setVisibility(View.GONE);
        }
        else {
            textSubmit.setText(Reddit.getFormattedHtml(submitTextHtml));
        }

        if (Reddit.POST_TYPE_LINK.equals(getIntent().getStringExtra(POST_TYPE))) {
            editTextBody.setHint("URL");
        }
        else {
            editTextBody.setHint("Text");
        }

        floatingActionButton = (FloatingActionButton) findViewById(R.id.fab_submit_post);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getIntent().getBooleanExtra(IS_EDIT, false)) {
                    submitEdit();
                }
                else {
                    submitNew();
                }
            }
        });

        progressSubmit = (ProgressBar) findViewById(R.id.progress_submit);

        layoutCaptcha = (RelativeLayout) findViewById(R.id.layout_captcha);
        imageCaptcha = (ImageView) findViewById(R.id.image_captcha);
        editCaptcha = (EditText) findViewById(R.id.edit_captcha);
        buttonCaptchaRefresh = (ImageButton) findViewById(R.id.button_captcha_refresh);
        buttonCaptchaRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCaptcha();
            }
        });

        if (getIntent().getBooleanExtra(IS_EDIT, false)) {
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
                    Picasso.with(ActivityNewPost.this)
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
        reddit.loadGet(Reddit.OAUTH_URL + "/api/info?id=" + getIntent().getStringExtra(EDIT_ID),
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
        params.put("thing_id", getIntent().getStringExtra(EDIT_ID));

        reddit.loadPost(Reddit.OAUTH_URL + "/api/editusertext", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

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
                                setResult(Activity.RESULT_OK);
                                finish();
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
                Toast.makeText(ActivityNewPost.this, getString(R.string.error_submitting_post), Toast.LENGTH_LONG)
                        .show();
                progressSubmit.setVisibility(View.GONE);
            }
        }, params, 0);
    }

    private void submitNew() {
        String text = editTextBody.getText().toString();

        if (TextUtils.isEmpty(editTextTitle.getText().toString())) {
            Toast.makeText(ActivityNewPost.this, getString(R.string.empty_title), Toast.LENGTH_LONG)
                    .show();
            return;
        }
        else if (Reddit.POST_TYPE_LINK.equals(getIntent().getStringExtra(POST_TYPE)) && !URLUtil
                .isValidUrl(text)) {

            text = "http://" + text;

            if (!URLUtil.isValidUrl(text)) {
                Toast.makeText(ActivityNewPost.this, getString(R.string.invalid_url),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        progressSubmit.setIndeterminate(true);
        progressSubmit.setVisibility(View.VISIBLE);

        Map<String, String> params = new HashMap<>();
        params.put("kind", getIntent().getStringExtra(POST_TYPE).toLowerCase());
        params.put("api_type", "json");
        params.put("resubmit", "true");
        params.put("sendreplies", "true");
        params.put("then", "comments");
        params.put("extension", "json");
        params.put("sr", getIntent().getStringExtra(SUBREDDIT));
        params.put("title", editTextTitle.getText().toString());
        if (Reddit.POST_TYPE_LINK.equalsIgnoreCase(getIntent().getStringExtra(POST_TYPE))) {
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
                            Picasso.with(ActivityNewPost.this)
                                    .load(Reddit.BASE_URL + "/captcha/" + captchaId + ".png")
                                    .resize(getResources().getDisplayMetrics().widthPixels, 0).into(
                                    imageCaptcha);
                        }

                        Toast.makeText(ActivityNewPost.this, getString(R.string.error) + ": " + error, Toast.LENGTH_LONG)
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
                                setResult(Activity.RESULT_OK);
                                finish();
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
                Toast.makeText(ActivityNewPost.this, getString(R.string.error_submitting_post), Toast.LENGTH_LONG)
                        .show();
                progressSubmit.setVisibility(View.GONE);
            }
        }, params, 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}