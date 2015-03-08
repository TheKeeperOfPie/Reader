# Reader

A Reddit client created to practice good UI/UX design

## Screenshots
<b>Starting view and thread list</b>

<img src="/Assets/Screenshots/Expanded Item.png" width="400px"/>

<b>Navigation drawer</b>

<img src="/Assets/Screenshots/Nav Drawer.png" width="400px"/>

<b>Comment thread</b>

<img src="/Assets/Screenshots/Comment Thread.png" width="400px"/>

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

## Documentation

### Setting Up

So, the first thing we should do when starting a new project like this is the basics. Before we even start delving into Reddit's Application Programming Interface (API), we need to create the Android User Interface (UI). To align with Google's Material Design (MD) guidelines, the first thing we should do is to set up a Toolbar that acts as the app's ActionBar, as well as defining the three basic colorPrimary, colorPrimaryDark, and colorAccent colors inside a new colors.xml file. Android actually doesn't care what the XML file is named, but we'll use "colors" to make it clear what the file does. These colors, along with a default background color, are the ones that will be constantly used inside the app UI.

After those basics, we'll need to go and make our starting UI screen. For Reader, that would be the basic thread list, defaulted to /r/All, along with the navigation drawer on the left side and the Toolbar up top.

### Data Models

Inorder to actually include Reddit data inside Reader, we'll need some data models to represent the various parts of the Reddit ecosystem, such as threads and comments, along with a wrapper model around each comment. The way we'll do it is just using Java objects, created from a class that handles all of our API calls. /But first, we'll have to add the following permission to our AndroidManifest.xml, to access the internet:

```
    <uses-permission android:name="android.permission.INTERNET"/>
```

To access Reddit's APIs, we'll need to use OAuth 2.0, which requires registering an application on Reddit and getting back a client_id. This ID can then be used to to access the API calls for the application. We'll add support for logging in as a user eventually, but for now, we'll just use general API calls. We'll create a class called Reddit that will house all of the relevant API calls, as well as creating several data models that hold the information returned from those calls, which will all be made using Volley Requests.

### Thread List

So, to start off with the thread list, we'll be using Google's new RecyclerView, which offers maximum control in editing the list entries and customizing the look and behavior. It's more complicated overall to use than a normal ListView, but the benefits will be worth it eventually.

First off, we'll need to import RecyclerView, as it's not part of the default Android SDK, using:

```
    compile 'com.android.support:recyclerview-v7:21.0.3'
```

Next, we need to create a new Fragment for our thread list, which we'll name FragmentThreadList, and then place an android.support.v7.widget.RecyclerView inside the fragment_thread_list.xml layout file, giving it an ID so we can reference it in code. Next, we'll do some boilerplate like setting hasFixedSize(true) and giving it a default LinearLayoutManager, as we'll just be using a vertical list. Next comes creating the actual RecyclerView.Adapter<VH> that will be used to pass data into the RecyclerView. Along with it, we'll create an inner ViewHolder class that will store references to our views, so that we can have instant access to them.

To add rows to our RecyclerView list, we'll create a row_thread.xml layout file, representing a single thread or row in the list. The thread row will contain a preview image, the thread title, some information about the thread including poster and points, and a button to access the comment thread. Since RecyclerView doesn't come with its own OnClickListener implementation, we'll have to add our own class and pass it into the adapter.