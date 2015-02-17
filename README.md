# Reader

A Reddit client created to practice good UI/UX design

## Screenshots
<b>Starting view and thread list</b>

<img src="/Assets/Screenshots/Expanded Item.png" width="400px""/>

<b>Navigation drawer</b>

<img src="/Assets/Screenshots/Nav Drawer.png" width="400px""/>

<b>Comment thread</b>

<img src="/Assets/Screenshots/Comment Thread.png" width="400px""/>

## Layout Plan 

Universal
  - Navigation Drawer
      - Test
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
      - Click to scale up to full size image
    - Post details
    - Comment button
  - Smooth transition on click to top of screen

Comment Thread
  - Thread view
    - Show expanded view
    - Swipe down gesture to return and animate position back to thread list
  - Comment view
    - Level indicator
    - Comment details
    - Expandable view for actions
  - Easy parent comment navigation
