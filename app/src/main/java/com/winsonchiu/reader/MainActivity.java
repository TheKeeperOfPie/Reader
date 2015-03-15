package com.winsonchiu.reader;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;


public class MainActivity extends ActionBarActivity
        implements FragmentNavDrawer.NavigationDrawerCallbacks,
        FragmentThreadList.OnFragmentInteractionListener,
        FragmentWeb.OnFragmentInteractionListener,
        FragmentComments.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getCanonicalName();
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private FragmentNavDrawer mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private int oldPosition = -1;
    private Toolbar toolbar;
    private TextView textTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (FragmentNavDrawer)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuItem itemSearch = toolbar.getMenu().findItem(R.id.item_search);
                if (itemSearch != null) {
                    itemSearch.expandActionView();
                    ((SearchView) itemSearch.getActionView()).setQuery(toolbar.getTitle().toString().replaceAll("/r/", ""), false);
                }
            }
        });
        setSupportActionBar(toolbar);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                toolbar);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // TODO: update the main content by replacing fragments
        if (oldPosition != position) {
            getFragmentManager().beginTransaction().replace(R.id.frame_fragment, FragmentThreadList.newInstance("", "")).commit();
        }
        oldPosition = position;
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        setToolbarTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            FragmentWeb fragmentWeb = (FragmentWeb) getFragmentManager().findFragmentByTag("fragmentWeb");

            if (fragmentWeb != null && fragmentWeb.navigateBack()) {
                return;
            }

            getFragmentManager().popBackStack();
            Log.d(TAG, "popBackStack");
        }
        else {
            super.onBackPressed();
        }

    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        mTitle = title;
        toolbar.setTitle(mTitle);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
