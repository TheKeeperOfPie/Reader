package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Reddit;

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
    private static final String TAG = ActivityNewPost.class.getCanonicalName();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        setSupportActionBar(toolbar);

        textInfo.setText(getString(R.string.submitting_to) + " /r/" + getIntent().getStringExtra(SUBREDDIT) + " " + getString(R.string.as) + " /u/" + getIntent().getStringExtra(USER));

        String submitTextHtml = getIntent().getStringExtra(SUBMIT_TEXT_HTML);
        Log.d(TAG, "submitTextHtml: " + submitTextHtml);
        if (TextUtils.isEmpty(submitTextHtml) || "null".equals(submitTextHtml)) {
            textSubmit.setVisibility(View.GONE);
        }
        else {
            textSubmit.setText(Reddit.getTrimmedHtml(submitTextHtml));
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

                if (TextUtils.isEmpty(editTextTitle.getText().toString())) {
                    Toast.makeText(ActivityNewPost.this, getString(R.string.empty_title), Toast.LENGTH_LONG).show();
                    return;
                }
                else if (Reddit.POST_TYPE_LINK.equals(getIntent().getStringExtra(POST_TYPE)) && !URLUtil.isValidUrl(editTextBody.getText().toString())) {
                    Toast.makeText(ActivityNewPost.this, getString(R.string.invalid_url), Toast.LENGTH_LONG).show();
                    return;
                }

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
                    params.put("url", editTextBody.getText().toString());
                }
                else {
                    params.put("text", TextUtils.htmlEncode(editTextBody.getText()
                            .toString()));
                }
                if (layoutCaptcha.isShown()) {
                    params.put("iden", captchaId);
                    params.put("captcha", editCaptcha.getText().toString());
                }

                reddit.loadPost(Reddit.OAUTH_URL + "/api/submit", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }, params, 0);

                setResult(Activity.RESULT_OK);
                finish();
            }
        });

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

        reddit.loadGet(Reddit.OAUTH_URL + "/api/needs_captcha", new Response.Listener<String>() {
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

    private void loadCaptcha() {

        Map<String, String> params = new HashMap<>();
        params.put("api_type", "json");

        reddit.loadPost(Reddit.OAUTH_URL + "/api/new_captcha", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                captchaId = response;
                Picasso.with(ActivityNewPost.this).load(Reddit.OAUTH_URL + "/captcha/" + response).into(imageCaptcha);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

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