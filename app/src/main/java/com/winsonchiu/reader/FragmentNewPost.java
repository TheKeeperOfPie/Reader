/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.rjeschke.txtmark.Processor;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Listing;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.links.ControllerLinks;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.utils.UtilsColor;

import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

import rx.Observer;

public class FragmentNewPost extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String USER = "User";
    public static final String SUBREDDIT = "Subreddit";
    public static final String POST_TYPE = "PostType";
    public static final String SUBMIT_TEXT_HTML = "SubmitTextHtml";
    public static final String IS_EDIT = "isEdit";
    public static final String EDIT_ID = "editId";
    public static final String TAG = FragmentNewPost.class.getCanonicalName();
    private static final int PAGE_POST = 0;
    private static final int PAGE_PREVIEW = 1;

    private Toolbar toolbar;
    private TextView textInfo;
    private TextView textSubmit;
    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private NestedScrollView scrollText;
    private EditText editTextTitle;
    private EditText editTextBody;
    private TextView textPreview;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Toolbar toolbarActions;
    private View viewDivider;

    @Inject Reddit reddit;
    @Inject Picasso picasso;
    @Inject ControllerLinks controllerLinks;

    private int editMarginDefault;
    private int editMarginWithActions;

    private RelativeLayout layoutCaptcha;
    private String captcha;
    private ImageView imageCaptcha;
    private EditText editCaptcha;
    private ImageButton buttonCaptchaRefresh;
    private Reddit.PostType postType;

    private FragmentListenerBase mListener;
    private Activity activity;
    private Menu menu;
    private MenuItem itemHideActions;
    private ColorFilter colorFilterPrimary;
    private ColorFilter colorFilterIcon;

    public static FragmentNewPost newInstance(String user, String subredditUrl, Reddit.PostType postType, String submitTextHtml) {
        FragmentNewPost fragment = new FragmentNewPost();
        Bundle args = new Bundle();
        args.putString(USER, user);
        args.putString(SUBREDDIT, subredditUrl);
        args.putString(SUBMIT_TEXT_HTML, submitTextHtml);
        args.putSerializable(POST_TYPE, postType);
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
        postType = (Reddit.PostType) getArguments().getSerializable(POST_TYPE);
    }

    @Override
    protected void inject() {
        ((ActivityMain) activity).getComponentActivity().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_new_post, container, false);

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);
        scrollText = (NestedScrollView) view.findViewById(R.id.scroll_text);

        textInfo = (TextView) view.findViewById(R.id.text_info);
        textSubmit = (TextView) view.findViewById(R.id.text_submit);
        editTextTitle = (EditText) view.findViewById(R.id.edit_title);
        editTextBody = (EditText) view.findViewById(R.id.edit_body);

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.colorPrimary, R.attr.colorIconFilter});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        int colorIcon = typedArray.getColor(1, getResources().getColor(R.color.darkThemeIconFilter));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.showOnWhite(colorPrimary) ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;
        int colorResourceTextMuted = UtilsColor.showOnWhite(colorPrimary) ? R.color.darkThemeTextColorMuted : R.color.lightThemeTextColorMuted;

        colorFilterPrimary = new PorterDuffColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterIcon = new PorterDuffColorFilter(colorIcon, PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.new_post));
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(editTextBody.getWindowToken(), 0);
                mListener.onNavigationBackClick();
            }
        });
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);
        setUpOptionsMenu();

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

        if (Reddit.PostType.LINK == postType) {
            editTextBody.setHint("URL");
        }
        else {
            editTextBody.setHint("Text");
        }

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

        editTextTitle.setOnFocusChangeListener(onFocusChangeListener);
        editTextBody.setOnFocusChangeListener(onFocusChangeListener);

        editMarginDefault = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                getResources().getDisplayMetrics());
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
                    case PAGE_POST:
                        return getString(R.string.page_post);
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
                if (Reddit.PostType.LINK == postType) {
                    tabLayout.setVisibility(View.GONE);
                    toolbarActions.setVisibility(View.GONE);
                    viewDivider.setVisibility(View.GONE);
                    itemHideActions.setVisible(false);
                    return 1;
                }
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

                if (position == PAGE_POST && toolbarActions.getVisibility() == View.VISIBLE) {
                    float translationY = positionOffset * (toolbarActions.getHeight() + viewDivider
                            .getHeight());
                    viewDivider.setTranslationY(translationY);
                    toolbarActions.setTranslationY(translationY);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == PAGE_PREVIEW) {
                    if (editTextBody.length() == 0) {
                        textPreview.setText(R.string.empty_reply_preview);
                    }
                    else {
                        textPreview.setText(
                                Html.fromHtml(
                                        Processor.process(editTextBody.getText().toString())));
                    }
                }
                if (Reddit.PostType.SELF == postType) {
                    itemHideActions.setVisible(position == PAGE_POST);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

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
            reddit.needsCaptcha()
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
                            if ("true".equalsIgnoreCase(response)) {
                                layoutCaptcha.setVisibility(View.VISIBLE);
                                loadCaptcha();
                            }
                        }
                    });
        }

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
        toolbar.inflateMenu(R.menu.menu_new_post);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();
        itemHideActions = menu.findItem(R.id.item_hide_actions);

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
        }
    }

    private void loadCaptcha() {
        reddit.newCaptcha()
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
                        if (!isAdded()) {
                            return;
                        }

                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            captcha = jsonObject.getJSONObject("json").getJSONObject("data").getString(
                                    "iden");
                            Log.d(TAG, "captcha: " + captcha);
                            picasso.load(Reddit.BASE_URL + "/captcha/" + captcha + ".png")
                                    .resize(getResources().getDisplayMetrics().widthPixels, 0).into(
                                    imageCaptcha);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void loadEditValues() {
        reddit.info(getArguments().getString(EDIT_ID))
                .flatMap(Listing.FLAT_MAP)
                .subscribe(new Observer<Listing>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(activity, R.string.error_loading, Toast.LENGTH_LONG);
                    }

                    @Override
                    public void onNext(Listing listing) {
                        Link link = (Link) listing.getChildren().get(0);
                        editTextTitle.setText(link.getTitle());
                        editTextTitle.setClickable(false);
                        editTextTitle.setFocusable(false);
                        editTextTitle.setFocusableInTouchMode(false);
                        editTextTitle.setEnabled(false);

                        editTextBody.setText(link.getSelfText());
                    }
                });
    }

    private void submitEdit() {
        String id = getArguments().getString(EDIT_ID);
        String text = editTextBody.getText().toString();

        reddit.editUserText(id, text)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(activity, getString(R.string.error_submitting_post),
                                Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onNext(String s) {
                        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(editTextBody.getWindowToken(), 0);
                        controllerLinks.reloadAllLinks(false)
                                .subscribe(new FinalizingSubscriber<Listing>() {
                                    @Override
                                    public void error(Throwable e) {
                                        Toast.makeText(activity, getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                });
                        mListener.onNavigationBackClick();
                    }
                });
    }

    private void submitNew() {
        String subreddit = getArguments().getString(SUBREDDIT);
        String title = editTextTitle.getText().toString();
        String text = editTextBody.getText().toString();
        String captchaId = TextUtils.isEmpty(captcha) ? null : captcha;
        String captchaText = TextUtils.isEmpty(captcha) ? null : editCaptcha.getText().toString();

        if (TextUtils.isEmpty(editTextTitle.getText().toString())) {
            Toast.makeText(activity, getString(R.string.empty_title), Toast.LENGTH_LONG)
                    .show();
            return;
        }
        else if (Reddit.PostType.LINK == postType && !URLUtil
                .isNetworkUrl(text)) {

            text = "http://" + text;

            if (!URLUtil.isNetworkUrl(text)) {
                Toast.makeText(activity, getString(R.string.invalid_url),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        reddit.submit(postType,
                subreddit,
                title,
                text,
                captchaId,
                captchaText)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(activity, getString(R.string.error_submitting_post), Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onNext(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response).getJSONObject("json");
                            String error = jsonObject.getJSONArray("errors").optString(
                                    0);
                            if (!TextUtils.isEmpty(error)) {

                                String captcha = jsonObject.optString("captcha");

                                if (!TextUtils.isEmpty(captcha)) {
                                    FragmentNewPost.this.captcha = captcha;
                                    editCaptcha.setText("");
                                    picasso.load(Reddit.BASE_URL + "/captcha/" + FragmentNewPost.this.captcha + ".png")
                                            .resize(getResources().getDisplayMetrics().widthPixels, 0).into(
                                            imageCaptcha);
                                }

                                Toast.makeText(activity, getString(R.string.error) + ": " + error, Toast.LENGTH_LONG)
                                        .show();
                                return;
                            }
                        }
                        catch (JSONException e) {
                            onError(e);
                            return;
                        }

                        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(editTextBody.getWindowToken(), 0);
                        controllerLinks.reloadAllLinks(false)
                                .subscribe(new FinalizingSubscriber<Listing>() {
                                    @Override
                                    public void error(Throwable e) {
                                        Toast.makeText(activity, getString(R.string.error_loading_links), Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                });
                        mListener.onNavigationBackClick();
                    }
                });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_submit_post:
                if (getArguments().getBoolean(IS_EDIT, false)) {
                    submitEdit();
                }
                else {
                    submitNew();
                }
                break;
            case R.id.item_hide_actions:
                toggleActions();
                break;
        }
        Reddit.onMenuItemClickEditor(editTextBody, item, getResources());

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
        else if (Reddit.PostType.SELF == postType) {
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

}
