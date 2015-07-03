# [Reader](https://play.google.com/store/apps/details?id=com.winsonchiu.reader)

A Reddit client created to practice good UI/UX design

## Licenses

### Open Source License

This repository has no specific license. You are free to use and modify this code however you want, but if your application will compete with Reader for Reddit on the Google Play Store and you have not received permission otherwise, do not publish it there.

### Libraries

#### [Picasso](http://square.github.io/picasso/)

Copyright 2013 Square, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

#### [Volley](https://developer.android.com/training/volley/index.html)

Copyright (C) 2012 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Screenshots
<b>Thread list</b>

<img src="/Assets/Screenshots/thread_list.png" width="500px"/>

<b>Comment thread</b>

<img src="/Assets/Screenshots/comments.png" width="500px"/>

<b>Subreddit list</b>

<img src="/Assets/Screenshots/subreddit_search.png" width="500px"/>

<b>Aww</b>

<img src="/Assets/Screenshots/grid_view_aww.png" width="500px"/>

## Objectives

Universal
  - Navigation Drawer
      - Fancy Material Design background header
      - Account switch
      - ListView entries
      - Home
      - Inbox
      - Profile
      - Search
      - Settings
  - Action bar toolbar
      - Title based on current screen
      - Overflow menu hiding less important actions

Thread List
  - Thread view
    - Thumbnail preview
      - Click to scale up to full size image above thread entry, move post details under image
    - Post details
    - Comment button
  - Smooth translation animation on click to top of screen

Comment Thread
  - Thread view
    - Show expanded view
    - Swipe down gesture to return and animate position back to thread list
    - Swipe sideways to move to previous/next post
  - Comment view
    - Find a way to implement tree, including view for load more
    - Level indicator
    - Comment details
    - Collapsable with possible animation
    - Expandable view for actions
      - Upvote/downvote
      - User profile
      - Reply with circular review into reply entry Fragment
      - Overflow for additional actions
  - Easy parent comment navigation

Data Models
  - API class
  - Models for each necessary data set in the UI
  - Implementing various libraries like Volley/Picasso
  - Networking framework to sync data

## Formatting

This section address how the IDs and variables will be named.

View IDs: view_name_modifier
Example: R.id.recycler_view_thread_list_main

## ~~Documentation~~ (Really, really old and needs to be updated, please ignore)

### Setting Up

So, the first thing we should do when starting a new project like this is the basics. Before we even start delving into Reddit's Application Programming Interface (API), we need to create the Android User Interface (UI). To align with Google's Material Design (MD) guidelines, the first thing we should do is to set up a Toolbar that acts as the app's ActionBar, as well as defining the three basic colorPrimary, colorPrimaryDark, and colorAccent colors inside a new colors.xml file. Android actually doesn't care what the XML file is named, but we'll use "colors" to make it clear what the file does. These colors, along with a default background color, are the ones that will be constantly used inside the app UI.

After those basics, we'll need to go and make our starting UI screen. For Reader, that would be the basic thread list, defaulted to /r/All, along with the navigation drawer on the left side and the Toolbar up top.

### Navigation Drawer

In order for the user to access the different basic screens of Reader, such as Home (their front page), Inbox, Profile, etc., we'll need to have those entries in the Navigation Drawer. For reuse purposes, the drawer is stored in a FragmentNavDrawer, which will inflate the fragment_navigation_drawer.xml layout which includes a header, some UI decoration, and of course the main list of entries. We will follow Material Design guidelines to style the drawer, with a 16:9 image up top supporting rotation of user accounts, and a series of list entries below. Each will redirect to a different Fragment. Our entries will be as follows:

- Home - User's front page, or default /r/all if not logged in
- Profile - /u/username profile page
- Inbox - Message box, defaulted to all messages
- Settings - Application wide settings

### Action Bar

The basics of our action bar shows the subreddit title, along with options to search, sort, and change the RecyclerView interface. We can toggle between the list and grid UIs using this MenuItem, changing the adapters from an AdapterLinkList to an AdapterLinkGrid and vice versa. As we keep the actual data manipulation in controllerLinks, switching UIs is quick and easy, just recreating a few views.

The remaining two actions allowing searching for a subreddit and sorting the current subreddit by Reddit's various options, such as hot, new, and top.

### Data Models

Inorder to actually include Reddit data inside Reader, we'll need some data models to represent the various parts of the Reddit ecosystem, such as threads and comments, along with a wrapper model around each comment. The way we'll do it is just using Java objects, created from a class that handles all of our API calls. /But first, we'll have to add the following permission to our AndroidManifest.xml, to access the internet:

```
    <uses-permission android:name="android.permission.INTERNET"/>
```

To access Reddit's APIs, we'll need to use OAuth 2.0, which requires registering an application on Reddit and getting back a client_id. This ID can then be used to to access the API calls for the application. We'll add support for logging in as a user eventually, but for now, we'll just use general API calls.

Since we don't have a login system yet, we'll need to request a temporary token for the app to use, checking to refresh this token if we get a network error, or if the time has expired. We'll store the token and the check values inside a SharedPreferences instance global to our app, retrieved from PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).

We will be using Google's Volley library, wrapped in a static singleton class Reddit, with helper methods to make API calls. There are also some basic headers we need to send with our Reddit requests, such as a User-Agent and Authorization, which for now will be our application access token.

#### Links

For links, which make up the front of each subreddit, our adapter will make an API call to load 30 entries at once, loading more as we reach the bottom of the list. The resulting JSON from the API call is parsed manually through the Listing class, which in this case, creates a Listing of Link objects. I have chosen to manually parse the JSON to make sure that it follows the Reddit data model. Using a library such as Gson or Jackson would have been simpler, but Android handles JSON very well, and it's worth having full control over the class structure, especially since it's not all that difficult to set up.

To allow separate UIs for viewing the various sets of data, we are going to hide the implementation of the data model inside a ControllerLinks class, where different adapters can access a shared data pool, separating the visual front-end and the Link data model backend. The ControllerLinks class is where we will create a private Listing instance to represent the links and where API calls will be made to fetch the relevant data.

The class will hold a listener reference, which will be supplied by the adapter/Fragment to allow callbacks to occur from the data, such as notifyDataSetChanged() and notifyItemRangeInserted(). Our primary UI adapter, AdapterLinkList will show a list entry based UI, while AdapterLinkGrid will show a staggered grid layout. They will create unique ViewHolders representing the different UIs, but will bind their data through the controllerLinks reference they hold.

In the adapter's onBindViewHolder, we use Volley to load the link's thumbnail, or full image for the grid, but we have to make sure to check that the Bitmap isn't null in the ImageListener, as Volley calls the onResponse method twice if the image doesn't exist in the cache. The first time, the bitmap will be null, while the second time will actual contain the bitmap pulled from the network.

Once a link's preview thumbnail is clicked inside either UI, we want to be able to expand the image/text, or go to the link if a browser is needed. We pass the touch event to the ViewHolder, which either loads the image if the UI calls for such implementation, or passes the event upwards to the controllerLinks so that a FragmentWeb, for example, could handle it. If it's an image file, we make the imageFull ImageView visible and load the full URL into the ImageView. If it's a link, we use our we send the event up and load a FragmentWeb with the URL.

Implementing Imgur and Gfycat's APIs, we can check the link URL for Imgur albums, Imgur .gifv files, and Gfycat links, parsing them for their IDs. We can then use these IDs in an HTTP GET call to retrieve information such as width and height, as well as loading the properly formatted URL, passing into loadGifv, loadImgurAlbum, loading a raw image link into our WebView, or loading the FragmentWeb as appropriate.

We use a WebView to display images to leverage the built in zoom/pan gestures and large image support, although it takes some adjustment to work properly. We need to set an OnTouchListener to each WebView which calls requestDisallowInterceptTouchEvent() on our recyclerThreadList during MOTION_DOWN and MOTION_UP events to enable/disable the RecyclerView's scrolling as necessary, otherwise it's impossible to pan upwards or downwards without moving the entire list. This solution is far from optimal, but if we want to support zooming, it's a necessary sacrifice for now, until a better solution is found.

#### Comments

To represent a link's comment tree, we'll need to create a Comment object, which contains a List<Comment> object which contains all of its children comments, filled recursively through a static method. Each comment is assigned a int indent, which determines how far the level indicator is moved to the right, to show the parent and child relationship between comments.

The FragmentComments created to actually show the list of comments consists of a RecyclerView with a header, implemented by using 2 view types, giving position 0 a separate type and a ViewHolderHeader rather than the ViewHolderComment given to all other positions. This allows us to place a row_thread as a header view to show the parent link to the comment list, and have it scroll past as the user scrolls further down the comments. And like the links, we'll have options for the comments, expanded when a comment is clicked.

The actual comment data is held much like the link data, through a ControllerComments, to handle network requests and data access. As we need to hold multiple Comment objects, our controllerComments actually holds a link with the comments as children, so we can access both the parent link and the child comments. For the actual visible comments, we'll be using a separate list, holding all visible comments. This requires holding separate instances of our comments, but it's easier for the adapter to map the positions correctly, rather than checking each comment to see if it's visible and then incrementing the index as required.

Inside our adapter's onBindViewHolder, we setup the text and info for each comment, as well as taking the comment's level and multiplying it by an indentWidth, which is 8dp wide, in order to shift child comments to the right and make our list look like a tree model. In order to collapse and expand comments, all we have to is find the parent, and then iterate the list until we hit the next comment with the same level, or the end of the list. Then we can remove all of the found comments from the visible list. For expansion, we just go to the original listing, iterate again to find the child comments, and then add them back to the visible list.

### Thread List

So, to start off with the thread list, we'll be using Google's new RecyclerView, which offers maximum control in editing the list entries and customizing the look and behavior. It's more complicated overall to use than a normal ListView, but the benefits will be worth it eventually.

First off, we'll need to import RecyclerView, as it's not part of the default Android SDK, using:

```
    compile 'com.android.support:recyclerview-v7:21.0.3'
```

Next, we need to create a new Fragment for our thread list, which we'll name FragmentThreadList, and then place an android.support.v7.widget.RecyclerView inside the fragment_thread_list.xml layout file, giving it an ID so we can reference it in code. Next, we'll do some boilerplate like setting hasFixedSize(true) and giving it a default LinearLayoutManager, as we'll just be using a vertical list. Next comes creating the actual RecyclerView.Adapter<VH> that will be used to pass data into the RecyclerView. Along with it, we'll create an inner ViewHolder class that will store references to our views, so that we can have instant access to them.

To add rows to our RecyclerView list, we'll create a row_thread.xml layout file, representing a single thread or row in the list. The thread row will contain a preview image, the thread title, some information about the thread including poster and points, and buttons to upvote and downvote the link. Since RecyclerView doesn't come with its own OnClickListener implementation, we'll have to add our own class and pass it into the adapter.

This adapter will make network requests and load images into the views in onBindViewHolder. We will also create a SearchView inside our Toolbar for changing the subreddit displayed by typing it into the search bar. The adapter will handle this change through a method call which will set the subreddit and sort parameters and then force a reload of the listingLinks which represents our RecyclerView's list of items.
