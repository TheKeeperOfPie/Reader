/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by TheKeeperOfPie on 1/2/2016.
 */
public abstract class RecyclerFragmentPagerAdapter<FragmentType extends Fragment> extends PagerAdapter {

    private static final String TAG = "FragmentPagerAdapter";
    private static final boolean DEBUG = false;

    private final FragmentManager fragmentManager;
    private Stack<FragmentType> stackFragments = new Stack<>();
    private List<FragmentType> removalBuffer = new ArrayList<>();
    private FragmentTransaction currentTransaction = null;
    private Fragment currentItem = null;

    private ArrayList<Fragment.SavedState> savedStates = new ArrayList<>();
    private ArrayList<FragmentType> fragments = new ArrayList<>();

    public RecyclerFragmentPagerAdapter(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public abstract FragmentType createFragment();

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
        if (fragments.size() > position) {
            Fragment f = fragments.get(position);

            if (f != null) {
                return f;
            }
        }

        if (currentTransaction == null) {
            currentTransaction = fragmentManager.beginTransaction();
        }

        FragmentType fragment;

        if (stackFragments.isEmpty()) {
            fragment = createFragment();
        }
        else {
            fragment = stackFragments.pop();
        }

        if (DEBUG) Log.v(TAG, "Adding item #" + position + ": f=" + fragment);
        if (savedStates.size() > position) {
            Fragment.SavedState fss = savedStates.get(position);
            if (fss != null) {
                fragment.setInitialSavedState(fss);
            }
        }
        while (fragments.size() <= position) {
            fragments.add(null);
        }
        FragmentCompat.setMenuVisibility(fragment, false);
        FragmentCompat.setUserVisibleHint(fragment, false);
        fragments.set(position, fragment);
        currentTransaction.add(container.getId(), fragment);

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;

        if (currentTransaction == null) {
            currentTransaction = fragmentManager.beginTransaction();
        }
        if (DEBUG) Log.v(TAG, "Removing item #" + position + ": f=" + object
                + " v=" + ((Fragment)object).getView());
        while (savedStates.size() <= position) {
            savedStates.add(null);
        }
        savedStates.set(position, fragmentManager.saveFragmentInstanceState(fragment));
        fragments.set(position, null);

        currentTransaction.remove(fragment);

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

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (savedStates.size() > 0) {
            state = new Bundle();
            Fragment.SavedState[] fss = new Fragment.SavedState[savedStates.size()];
            savedStates.toArray(fss);
            state.putParcelableArray("states", fss);
        }
        for (int i=0; i<fragments.size(); i++) {
            Fragment f = fragments.get(i);
            if (f != null && f.isAdded()) {
                if (state == null) {
                    state = new Bundle();
                }
                String key = "f" + i;
                fragmentManager.putFragment(state, key, f);
            }
        }
        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state != null) {
            Bundle bundle = (Bundle)state;
            bundle.setClassLoader(loader);
            Parcelable[] fss = bundle.getParcelableArray("states");
            savedStates.clear();
            fragments.clear();
            if (fss != null) {
                for (Parcelable fs : fss) {
                    savedStates.add((Fragment.SavedState) fs);
                }
            }
            Iterable<String> keys = bundle.keySet();
            for (String key: keys) {
                if (key.startsWith("f")) {
                    int index = Integer.parseInt(key.substring(1));
                    FragmentType f = (FragmentType) fragmentManager.getFragment(bundle, key);
                    if (f != null) {
                        while (fragments.size() <= index) {
                            fragments.add(null);
                        }
                        FragmentCompat.setMenuVisibility(f, false);
                        fragments.set(index, f);
                    } else {
                        Log.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
        }
    }

    public List<FragmentType> getFragments() {
        return fragments;
    }
}
