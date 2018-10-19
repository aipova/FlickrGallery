## Flickr Gallery
This is an android image gallery app with Flickr API integration.
An old school educational project for Big Nerd Ranch course.

#### Features
* AsyncTask for API calls and HandlerThread for background image loading;
* Caching bitmap images in LruCache;
* Background checks for new photos with IntentService scheduled by AlarmManager or JobScheduler;
* Notification about new photos when app is in the background;
* Storing search string in SharedPreferences;
* WebView for image details.

#### Used libraries
* [Gson](https://github.com/google/gson)
* [Picasso](https://github.com/square/picasso)

<p align="center">
    <img src="https://github.com/aipova/photogallery/blob/master/screenshots/main.jpg" height="300">
    <img src="https://github.com/aipova/photogallery/blob/master/screenshots/search.jpg" height="300">
</p>
