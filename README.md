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

To access Reddit's APIs, we'll need to use OAuth 2.0, which requires registering an application on Reddit and getting back a client_id. This ID can then be used to to access the API calls for the application. We'll add support for logging in as a user eventually, but for now, we'll just use general API calls.

Since we don't have a login system yet, we'll need to request a temporary token for the app to use, checking to refresh this token if we get a network error, or if the time has expired. We'll store the token and the check values inside a SharedPreferences instance global to our app, retrieved from PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).

We'll be using the Ion library for basic POST/GET requests and for image loading, as it offers several easy to use features and image caching. There are some basic headers we need to send with our Reddit requests, such as a User-Agent and Authorization, which for now will be our application access token.

#### Links

For links, which make up the front of each subreddit, our adapter will make an API call to load 30 entries at once, loading more as we reach the bottom of the list. The resulting JSON from the API call is parsed manually through the Listing class, which in this case, creates a Listing of Link objects. I have chosen to manually parse the JSON to make sure that it follows the Reddit data model. Using a library such as Gson or Jackson would have been simpler, but Android handles JSON very well, and it's worth having full control over the class structure, especially since it's not all that difficult to set up.

Once our links are loaded, they're held inside a private Listing instance, and we call notifyDataSetChanged() or notifyItemRangeInserted() to get our adapter to update itself. It creates each row view inside onCreateViewHolder, giving us access to all the views contained in the row through a custom ViewHolder. Then, we use Ion and the Listing instance to load up the data for each row, syncing them with the Listing's children.

Once a link's preview thumbnail is clicked, we want to be able to expand the image/text, or go to the link if a browser is needed. We pass a custom ThreadClickListener to the adapter from our FragmentThreadList so we can be notified when a user decides to click one of the links. Inside the ViewHolder itself, we decide whether or not the selected link is an image or not, and we load it appropriately. If it's an image file, we make the imageFull ImageView visible and have Ion load the full URL into the ImageView. If it's a link, we use our ThreadClickListener to have our FragmentThreadList add a new FragmentWeb to the application's back stack, showing a fullscreen WebView.

#### Comments

To represent a link's comment tree, we'll need to create a Comment object, which contains a List<Comment> object which contains all of its children comments, filled recursively through a static method. Each comment is assigned a int indent, which determines how far the level indicator is moved to the right, to show the parent and child relationship between comments.

The FragmentComments created to actually show the list of comments consists of a RecyclerView with a header, implemented by using 2 view types, giving position 0 a separate type and the ViewHolderHeader rather than the ViewHolderComment given to all other positions. This allows us to place a row_thread as a header view to show the parent link to the comment list, and have it scroll past as the user scrolls further down the comments. And like the links, we'll have options for the comments, expanded when a comment is clicked.

### Thread List

So, to start off with the thread list, we'll be using Google's new RecyclerView, which offers maximum control in editing the list entries and customizing the look and behavior. It's more complicated overall to use than a normal ListView, but the benefits will be worth it eventually.

First off, we'll need to import RecyclerView, as it's not part of the default Android SDK, using:

```
    compile 'com.android.support:recyclerview-v7:21.0.3'
```

Next, we need to create a new Fragment for our thread list, which we'll name FragmentThreadList, and then place an android.support.v7.widget.RecyclerView inside the fragment_thread_list.xml layout file, giving it an ID so we can reference it in code. Next, we'll do some boilerplate like setting hasFixedSize(true) and giving it a default LinearLayoutManager, as we'll just be using a vertical list. Next comes creating the actual RecyclerView.Adapter<VH> that will be used to pass data into the RecyclerView. Along with it, we'll create an inner ViewHolder class that will store references to our views, so that we can have instant access to them.

To add rows to our RecyclerView list, we'll create a row_thread.xml layout file, representing a single thread or row in the list. The thread row will contain a preview image, the thread title, some information about the thread including poster and points, and buttons to upvote and downvote the link. Since RecyclerView doesn't come with its own OnClickListener implementation, we'll have to add our own class and pass it into the adapter.

This adapter will make network requests and load images into the views in onBindViewHolder. We will also create a SearchView inside our Toolbar for changing the subreddit displayed by typing it into the search bar. The adapter will handle this change through a method call which will set the subreddit and sort parameters and then force a reload of the listingLinks which represents our RecyclerView's list of items.