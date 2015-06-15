package com.winsonchiu.reader;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 6/7/2015.
 */
public class ActivityNewPost extends AppCompatActivity {


    public static final String POST_TYPE = "PostType";
    public static final String LINK = "Link";
    public static final String TEXT = "Text";
    public static final String SUBMIT_TEXT_HTML = "SubmitTextHtml";
    private static final String TAG = ActivityNewPost.class.getCanonicalName();
    private Toolbar toolbar;
    private TextView textSubmit;
    private EditText editTextTitle;
    private EditText editTextBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

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

        String submitTextHtml = getIntent().getStringExtra(SUBMIT_TEXT_HTML);
        Log.d(TAG, "submitTextHtml: " + submitTextHtml);
        textSubmit = (TextView) findViewById(R.id.text_submit);
        if (TextUtils.isEmpty(submitTextHtml) || "null".equals(submitTextHtml)) {
            textSubmit.setVisibility(View.GONE);
        }
        else {
            textSubmit.setText(Reddit.getTrimmedHtml(submitTextHtml));
        }

        editTextTitle = (EditText) findViewById(R.id.edit_title);
        editTextBody = (EditText) findViewById(R.id.edit_body);

        if (LINK.equals(getIntent().getStringExtra(POST_TYPE))) {
            editTextBody.setHint("URL");
        }
        else {
            editTextBody.setHint("Text");
        }
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