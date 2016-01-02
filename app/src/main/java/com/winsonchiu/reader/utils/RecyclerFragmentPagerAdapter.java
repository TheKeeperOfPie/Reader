/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Parcelable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Created by TheKeeperOfPie on 1/2/2016.
 */
public abstract class RecyclerFragmentPagerAdapter<FragmentType extends Fragment> extends PagerAdapter {

    private static final String TAG = "FragmentPagerAdapter";
    private static final boolean DEBUG = false;

    private final FragmentManager fragmentManager;
    private Set<FragmentType> fragmentsCreated = new HashSet<>();
    private Stack<FragmentType> stackFragments = new Stack<>();
    private List<FragmentType> removalBuffer = new ArrayList<>();
    private FragmentTransaction currentTransaction = null;
    private Fragment currentItem = null;

    public RecyclerFragmentPagerAdapter(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public abstract FragmentType createFragment();

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (currentTransaction == null) {
            currentTransaction = fragmentManager.beginTransaction();
        }

        final long itemId = getItemId(position);

        FragmentType fragment;

        if (stackFragments.isEmpty()) {
            fragment = createFragment();
            fragmentsCreated.add(fragment);
        }
        else {
            fragment = stackFragments.pop();
        }

        bindFragment(fragment, position);

        if (DEBUG) Log.v(TAG, "Adding item #" + itemId + ": f=" + fragment);
        currentTransaction.add(container.getId(), fragment,
                makeFragmentName(container.getId(), itemId));

        return fragment;
    }

    protected abstract void bindFragment(FragmentType fragment, int position);

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (currentTransaction == null) {
            currentTransaction = fragmentManager.beginTransaction();
        }
        if (DEBUG) Log.v(TAG, "Detaching item #" + getItemId(position) + ": f=" + object
                + " v=" + ((Fragment)object).getView());
        currentTransaction.remove((Fragment)object);

        removalBuffer.add((FragmentType) object);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        if (fragment != currentItem) {
            if (currentItem != null) {
                FragmentCompat.setMenuVisibility(currentItem, false);
                FragmentCompat.setUserVisibleHint(currentItem, false);
            }
            if (fragment != null) {
                FragmentCompat.setMenuVisibility(fragment, true);
                FragmentCompat.setUserVisibleHint(fragment, true);
            }
            currentItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (currentTransaction != null) {
            currentTransaction.commitAllowingStateLoss();
            currentTransaction = null;
            fragmentManager.executePendingTransactions();

            for (FragmentType fragment : removalBuffer) {
                stackFragments.push(fragment);
            }
        }

        removalBuffer.clear();
    }

    public Collection<FragmentType> getFragments() {
        return fragmentsCreated;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }

    /**
     * Return a unique identifier for the item at the given position.
     *
     * <p>The default implementation returns the given position.
     * Subclasses should override this method if the positions of items can change.</p>
     *
     * @param position Position within this adapter
     * @return Unique identifier for the item at position
     */
    public long getItemId(int position) {
        return position;
    }

    private static String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }
}
