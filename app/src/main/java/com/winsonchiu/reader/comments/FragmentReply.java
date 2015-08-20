/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.rjeschke.txtmark.Processor;
import com.winsonchiu.reader.FragmentBase;
import com.winsonchiu.reader.FragmentListenerBase;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.history.FragmentHistory;
import com.winsonchiu.reader.inbox.FragmentInbox;
import com.winsonchiu.reader.links.FragmentThreadList;
import com.winsonchiu.reader.profile.FragmentProfile;
import com.winsonchiu.reader.search.FragmentSearch;
import com.winsonchiu.reader.utils.UtilsColor;

public class FragmentReply extends FragmentBase implements Toolbar.OnMenuItemClickListener {

    public static final String ARG_NAME_PARENT = "nameParent";
    public static final String ARG_TEXT = "text";
    public static final String ARG_TEXT_PARENT = "textParent";
    public static final String ARG_IS_EDIT = "isEdit";
    public static final String ARG_COMMENT_LEVEL = "commentLevel";

    private static final int PAGE_PARENT = 0;
    private static final int PAGE_REPLY = 1;
    private static final int PAGE_PREVIEW = 2;
    public static final String TAG = FragmentReply.class.getCanonicalName();

    private FragmentListenerBase mListener;
    private Activity activity;

    private CoordinatorLayout layoutCoordinator;
    private AppBarLayout layoutAppBar;
    private NestedScrollView scrollText;
    private TextView textAuthor;
    private TextView textParent;
    private EditText editReply;
    private TextView textPreview;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private Toolbar toolbarActions;
    private View viewDivider;
    private Menu menu;

    private String nameParent;

    private int editMarginDefault;
    private int editMarginWithActions;
    private boolean collapsed;
    private ColorFilter colorFilterPrimary;
    private ColorFilter colorFilterIcon;

    public static FragmentReply newInstance(Replyable replyable) {
        FragmentReply fragment = new FragmentReply();
        Bundle args = new Bundle();
        args.putString(ARG_NAME_PARENT, replyable.getName());
        args.putCharSequence(ARG_TEXT, replyable.getReplyText());
        args.putCharSequence(ARG_TEXT_PARENT, replyable.getParentHtml());

        if (replyable instanceof Comment) {
            args.putBoolean(ARG_IS_EDIT, ((Comment) replyable).isEditMode());
            args.putInt(ARG_COMMENT_LEVEL, ((Comment) replyable).getLevel());
        }

        fragment.setArguments(args);
        return fragment;
    }

    public FragmentReply() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nameParent = getArguments().getString(ARG_NAME_PARENT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view =  inflater.inflate(R.layout.fragment_reply, container, false);

        layoutCoordinator = (CoordinatorLayout) view.findViewById(R.id.layout_coordinator);
        layoutAppBar = (AppBarLayout) view.findViewById(R.id.layout_app_bar);

        TypedArray typedArray = activity.getTheme().obtainStyledAttributes(
                new int[] {R.attr.colorPrimary, R.attr.colorIconFilter});
        final int colorPrimary = typedArray.getColor(0, getResources().getColor(R.color.colorPrimary));
        int colorIcon = typedArray.getColor(1, getResources().getColor(R.color.darkThemeIconFilter));
        typedArray.recycle();

        int colorResourcePrimary = UtilsColor.computeContrast(colorPrimary, Color.WHITE) > 3f ? R.color.darkThemeIconFilter : R.color.lightThemeIconFilter;

        colorFilterPrimary = new PorterDuffColorFilter(getResources().getColor(colorResourcePrimary), PorterDuff.Mode.MULTIPLY);
        colorFilterIcon = new PorterDuffColorFilter(colorIcon, PorterDuff.Mode.MULTIPLY);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(colorResourcePrimary));
        setUpToolbar();

        textAuthor = (TextView) view.findViewById(R.id.text_author);
        textAuthor.setText(getString(R.string.replying_from) + " " + mListener.getControllerUser().getUser().getName());

        scrollText = (NestedScrollView) view.findViewById(R.id.scroll_text);

        textParent = (TextView) view.findViewById(R.id.text_parent);
        textParent.setText(getArguments().getCharSequence(ARG_TEXT_PARENT, ""));

        editMarginDefault = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                getResources().getDisplayMetrics());
        editMarginWithActions = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56,
                getResources().getDisplayMetrics());

        editReply = (EditText) view.findViewById(R.id.edit_reply);
        editReply.setText(getArguments().getString(ARG_TEXT));
        editReply.setSelection(editReply.length());

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

        editReply.setOnFocusChangeListener(onFocusChangeListener);

        textPreview = (TextView) view.findViewById(R.id.text_preview);
        viewDivider = view.findViewById(R.id.view_divider);

        toolbarActions = (Toolbar) view.findViewById(R.id.toolbar_actions);
        toolbarActions.inflateMenu(R.menu.menu_editor_actions);
        toolbarActions.setOnMenuItemClickListener(this);

        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new PagerAdapter() {

            @Override
            public CharSequence getPageTitle(int position) {

                switch (position) {
                    case PAGE_PARENT:
                        return getString(R.string.page_parent);
                    case PAGE_REPLY:
                        return getString(R.string.page_reply);
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

                if (toolbarActions.getVisibility() == View.VISIBLE) {
                    float translationY = toolbarActions.getTranslationY();
                    if (position == PAGE_REPLY) {
                        translationY = positionOffset * (toolbarActions.getHeight() + viewDivider
                                .getHeight());
                    }
                    else if (position == PAGE_PARENT) {
                        translationY = (1f - positionOffset) * (toolbarActions
                                .getHeight() + viewDivider
                                .getHeight());
                    }
                    viewDivider.setTranslationY(translationY);
                    toolbarActions.setTranslationY(translationY);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == PAGE_PREVIEW) {
                    if (editReply.length() == 0) {
                        textPreview.setText(R.string.empty_reply_preview);
                    }
                    else {
                        textPreview.setText(
                                Html.fromHtml(Processor.process(editReply.getText().toString())));
                    }
                }
                menu.findItem(R.id.item_hide_actions).setVisible(position == PAGE_REPLY);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setOffscreenPageLimit(viewPager.getChildCount() - 1);
        viewPager.setCurrentItem(PAGE_REPLY);

        tabLayout = (TabLayout) view.findViewById(R.id.layout_tab);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

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

                        editReply.requestFocus();
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TEXT, editReply.getText().toString());
    }

    private void setUpToolbar() {

        toolbar.setTitle(getString(R.string.item_reply));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(editReply.getWindowToken(), 0);
                mListener.onNavigationBackClick();
            }
        });
        toolbar.getNavigationIcon().mutate().setColorFilter(colorFilterPrimary);

        toolbar.inflateMenu(R.menu.menu_reply);
        toolbar.setOnMenuItemClickListener(this);
        menu = toolbar.getMenu();

        for (int index = 0; index < menu.size(); index++) {
            menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterPrimary);
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

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_send_reply:
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(editReply.getWindowToken(), 0);

                if (getArguments().getBoolean(ARG_IS_EDIT)) {
                    mListener.getEventListenerComment().editComment(nameParent, getArguments().getInt(ARG_COMMENT_LEVEL), editReply.getText().toString());
                }
                else {
                    mListener.getEventListenerBase().sendComment(nameParent,
                            editReply.getText().toString());
                }

                collapsed = true;
                mListener.onNavigationBackClick();
                return true;
            case R.id.item_hide_actions:
                toggleActions();
                return true;
        }

        Reddit.onMenuItemClickEditor(editReply, item, getResources());

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

                    ((RelativeLayout.LayoutParams) scrollText.getLayoutParams()).bottomMargin = margin;
                    scrollText.requestLayout();
                }
            });
        }
        else {
            margin = editMarginWithActions;
            viewDivider.animate().translationY(0);
            toolbarActions.setVisibility(View.VISIBLE);
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
    public boolean navigateBack() {
        String text = editReply.getText().toString();

        // TODO: This is far too expensive to set the reply text
        if (getFragmentManager().findFragmentByTag(FragmentThreadList.TAG) != null) {
            mListener.getControllerLinks().setReplyText(nameParent, text, collapsed);
        }
        if (getFragmentManager().findFragmentByTag(FragmentComments.TAG) != null) {
            mListener.getControllerComments().setReplyText(nameParent, text, collapsed);
        }
        if (getFragmentManager().findFragmentByTag(FragmentProfile.TAG) != null) {
            mListener.getControllerProfile().setReplyText(nameParent, text, collapsed);
        }
        if (getFragmentManager().findFragmentByTag(FragmentInbox.TAG) != null) {
            mListener.getControllerInbox().setReplyText(nameParent, text, collapsed);
        }
        if (getFragmentManager().findFragmentByTag(FragmentHistory.TAG) != null) {
            mListener.getControllerHistory().setReplyText(nameParent, text, collapsed);
        }
        if (getFragmentManager().findFragmentByTag(FragmentSearch.TAG) != null) {
            mListener.getControllerSearch().setReplyTextLinks(nameParent, text, collapsed);
            mListener.getControllerSearch().setReplyTextLinksSubreddit(nameParent, text, collapsed);
        }

        return true;
    }
}
