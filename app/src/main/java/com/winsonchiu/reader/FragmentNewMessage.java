/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.github.rjeschke.txtmark.Processor;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.utils.UtilsColor;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FragmentNewMessage extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = FragmentNewMessage.class.getCanonicalName();
    public static final String ARG_RECIPIENT = "recipient";
    public static final String ARG_SUBJECT = "subject";
    public static final String ARG_MESSAGE = "message";

    private Reddit reddit;

    private Toolbar toolbar;
    private TextView textAuthor;
    private EditText editTextRecipient;
    private EditText editTextSubject;
    private EditText editTextMessage;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private NestedScrollView scrollText;
    private TextView textPreview;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Toolbar toolbarActions;
    private View viewDivider;
    private static final int PAGE_BODY = 0;
    private static final int PAGE_PREVIEW = 1;

    private int editMarginDefault;
    private int editMarginWithActions;

    private RelativeLayout layoutCaptcha;
    private String captchaId;
    private ImageView imageCaptcha;
    private EditText editCaptcha;
    private ImageButton buttonCaptchaRefresh;

    private FragmentListenerBase mListener;
    private Activity activity;
    private MenuItem itemHideActions;
    private Menu menu;
    private ColorFilter colorFilterPrimary;
    private ColorFilter colorFilterIcon;

    public static FragmentNewMessage newInstance() {
        FragmentNewMessage fragment = new FragmentNewMessage();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public static FragmentNewMessage newInstance(String recipient, String subject, String message) {
        FragmentNewMessage fragment = new FragmentNewMessage();
        Bundle args = new Bundle();
        args.putString(ARG_RECIPIENT, recipient);
        args.putString(ARG_SUBJECT, subject);
        args.putString(ARG_MESSAGE, message);
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
        final View view =  inflater.inflate(R.layout.fragment_new_message, container, false);

        reddit = Reddit.getInstance(activity);

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);
        scrollText = (NestedScrollView) view.findViewById(R.id.scroll_text);

        textAuthor = (TextView) view.findViewById(R.id.text_author);
        textAuthor.setText(getString(R.string.sending_from) + " " + mListener.getControllerUser().getUser().getName());

        editTextRecipient = (EditText) view.findViewById(R.id.edit_recipient);
        editTextSubject = (EditText) view.findViewById(R.id.edit_subject);
        editTextMessage = (EditText) view.findViewById(R.id.edit_message);

        Bundle arguments = getArguments();

        editTextRecipient.setText(arguments.getString(ARG_RECIPIENT));
        editTextSubject.setText(arguments.getString(ARG_SUBJECT));
        editTextMessage.setText(arguments.getString(ARG_MESSAGE));

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorIconFilter});
        final int colorPrimary = typedArray.getColor(0,
                getResources().getColor(R.color.colorPrimary));
        int colorIcon = typedArray.getColor(1, getResources().getColor(R.color.darkThemeIconFilter));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;
        int colorResourceTextMuted = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeTextColorMuted : R.color.lightThemeTextColorMuted;

        colorFilterPrimary = new PorterDuffColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterIcon = new PorterDuffColorFilter(colorIcon, PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.message));
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(editTextMessage.getWindowToken(), 0);
                mListener.onNavigationBackClick();
            }
        });
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);
        setUpOptionsMenu();

        View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    AppBarLayout.Behavior behaviorAppBar = (AppBarLayout.Behavior) ((CoordinatorLayout.LayoutParams) layoutAppBar
                            .getLayoutParams()).getBehavior();
                    behaviorAppBar
                            .onNestedFling(layoutCoordinator, layoutAppBar, null, 0, 1000, true);
                }
            }
        };

        editTextRecipient.setOnFocusChangeListener(onFocusChangeListener);
        editTextSubject.setOnFocusChangeListener(onFocusChangeListener);
        editTextMessage.setOnFocusChangeListener(onFocusChangeListener);

        editMarginDefault = (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        editMarginWithActions = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56,
                getResources().getDisplayMetrics());

        textPreview = (TextView) view.findViewById(R.id.text_preview);
        viewDivider = view.findViewById(R.id.view_divider);

        toolbarActions = (Toolbar) view.findViewById(R.id.toolbar_actions);
        toolbarActions.inflateMenu(R.menu.menu_editor_actions);
        toolbarActions.setOnMenuItemClickListener(this);

        tabLayout = (TabLayout) view.findViewById(R.id.layout_tab);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabTextColors(getResources().getColor(colorResourceTextMuted),
                getResources().getColor(colorResourcePrimary));

        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new PagerAdapter() {

            @Override
            public CharSequence getPageTitle(int position) {

                switch (position) {
                    case PAGE_BODY:
                        return getString(R.string.page_message);
                    case PAGE_PREVIEW:
                        return getString(R.string.page_preview);
                }

                return super.getPageTitle(position);
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                return viewPager.getChildAt(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {

            }

            @Override
            public int getCount() {
                return viewPager.getChildCount();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position,
                    float positionOffset,
                    int positionOffsetPixels) {

                if (position == PAGE_BODY && toolbarActions.getVisibility() == View.VISIBLE) {
                    float translationY = positionOffset * (toolbarActions.getHeight() + viewDivider
                            .getHeight());
                    viewDivider.setTranslationY(translationY);
                    toolbarActions.setTranslationY(translationY);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == PAGE_PREVIEW) {
                    if (editTextMessage.length() == 0) {
                        textPreview.setText(R.string.empty_reply_preview);
                    }
                    else {
                        textPreview.setText(
                                Html.fromHtml(
                                        Processor.process(editTextMessage.getText().toString())));
                    }
                }
                itemHideActions.setVisible(position == PAGE_BODY);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

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

        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Menu menu = toolbarActions.getMenu();

                        int maxNum = (int) (view.getWidth() / TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 48,
                                getResources().getDisplayMetrics()));
                        int numShown = 0;

                        for (int index = 0; index < menu.size(); index++) {

                            MenuItem menuItem = menu.getItem(index);
                            menuItem.getIcon().setColorFilter(colorFilterIcon);

                            if (numShown++ < maxNum - 1) {
                                menuItem
                                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                            }
                            else {
                                menuItem
                                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                            }
                        }

                        // Toggle visibility to fix weird bug causing tabs to not be added
                        tabLayout.setVisibility(View.GONE);
                        tabLayout.setVisibility(View.VISIBLE);
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        return view;
    }

    private void setUpOptionsMenu() {
        toolbar.inflateMenu(R.menu.menu_new_message);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();
        itemHideActions = menu.findItem(R.id.item_hide_actions);

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
        }
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
                            Reddit.loadPicasso(activity)
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
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(editTextMessage.getWindowToken(), 0);
                mListener.onNavigationBackClick();

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
                if (!isAdded()) {
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    captchaId = jsonObject.getJSONObject("json").getJSONObject("data").getString(
                            "iden");
                    Log.d(TAG, "captchaId: " + captchaId);
                    Reddit.loadPicasso(activity).load(Reddit.BASE_URL + "/captcha/" + captchaId + ".png")
                            .resize(imageCaptcha.getWidth(), 0).into(
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

    @Override
    public boolean onMenuItemClick(MenuItem item) {


        switch (item.getItemId()) {
            case R.id.item_send_message:
                submitMessage();
                break;
            case R.id.item_hide_actions:
                toggleActions();
                break;
        }

        Reddit.onMenuItemClickEditor(editTextMessage, item, getResources());

        return true;
    }

    private void toggleActions() {

        final int margin;
        float translationY = toolbarActions.getHeight() + viewDivider
                .getHeight();
        if (toolbarActions.isShown()) {
            margin = editMarginDefault;
            viewDivider.animate().translationY(translationY);
            toolbarActions.animate().translationY(translationY).withEndAction(new Runnable() {
                @Override
                public void run() {

                    toolbarActions.setVisibility(View.GONE);
                    viewDivider.setVisibility(View.GONE);

                    ((RelativeLayout.LayoutParams) scrollText.getLayoutParams()).bottomMargin = margin;
                    scrollText.requestLayout();
                }
            });
        }
        else {
            margin = editMarginWithActions;
            toolbarActions.setVisibility(View.VISIBLE);
            viewDivider.setVisibility(View.VISIBLE);
            viewDivider.animate().translationY(0);
            toolbarActions.animate().translationY(0).withEndAction(new Runnable() {
                @Override
                public void run() {
                    ((RelativeLayout.LayoutParams) scrollText.getLayoutParams()).bottomMargin = margin;
                    scrollText.requestLayout();
                }
            });
        }
    }
}
