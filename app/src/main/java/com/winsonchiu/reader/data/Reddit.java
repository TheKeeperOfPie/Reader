package com.winsonchiu.reader.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.winsonchiu.reader.AdapterCommentList;
import com.winsonchiu.reader.AdapterLinkGrid;
import com.winsonchiu.reader.AdapterLinkList;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.BuildConfig;
import com.winsonchiu.reader.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Reddit {


    // Constant values to represent Thing states
    public enum Distinguished {
        NOT_DISTINGUISHED, MODERATOR, ADMIN, SPECIAL
    }

    public static final String BASE_URL = "https://reddit.com";
    public static final String OAUTH_URL = "https://oauth.reddit.com";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_APP_JSON = "application/json; charset=utf-8";
    public static final String USER_AGENT = "User-Agent";
    public static final String CUSTOM_USER_AGENT = "android:com.winsonchiu.reader:" + BuildConfig.VERSION_NAME + " (by /u/TheKeeperOfPie)";
    public static final String REDIRECT_URI = "https://com.winsonchiu.reader";
    public static final String CLIENT_ID = "zo7k-Nsh7vgn-Q";

    public static final String GFYCAT_URL = "http://gfycat.com/cajax/get/";
    public static final String GFYCAT_WEBM = "webmUrl";
    public static final String GFYCAT_ITEM = "gfyItem";

    private static final String IMGUR_ALBUM_URL = "https://api.imgur.com/3/album/";
    private static final String IMGUR_GALLERY_URL = "https://api.imgur.com/3/gallery/";
    private static final String IMGUR_IMAGE_URL = "https://api.imgur.com/3/image/";

    public static final String GIFV = ".gifv";
    public static final String GIF = ".gif";
    public static final String PNG = ".png";
    public static final String JPG = ".jpg";
    public static final String JPEG = ".jpeg";
    public static final String WEBP = ".webp";

    public static final String DEFAULT = "default";
    public static final String NSFW = "nsfw";

    public static final String POST_TYPE_LINK = "Link";
    public static final String POST_TYPE_SELF = "Self";

    public static final String UTF_8 = "UTF-8";
    public static final String FRONT_PAGE = "Front Page";
    public static final String TIME_SEPARATOR = " - ";

    private static final String TAG = Reddit.class.getCanonicalName();

    public static final long SEC_TO_MS = 1000;

    public static final String ACCESS_URL = "https://www.reddit.com/api/v1/access_token";

    private static final String USER_AUTHENTICATION_URL = "https://www.reddit.com/api/v1/authorize.compact?";
    private static final String AUTH_SCOPES = "account,creddits,edit,flair,history,identity,livemanage,modconfig,modflair,modlog,modothers,modposts,modself,modwiki,mysubreddits,privatemessages,read,report,save,submit,subscribe,vote,wikiedit,wikiread";

    // Query fields
    public static final String INSTALLED_CLIENT_GRANT = Reddit.OAUTH_URL + "/grants/installed_client";
    public static final String CODE_GRANT = "authorization_code";
    public static final String QUERY_CODE = "code";
    public static final String QUERY_CLIENT_ID = "client_id";
    public static final String QUERY_REDIRECT_URI = "redirect_uri";
    public static final String QUERY_ACCESS_TOKEN = "access_token";
    public static final String QUERY_REFRESH_TOKEN = "refresh_token";
    public static final String QUERY_GRANT_TYPE = "grant_type";
    public static final String QUERY_DEVICE_ID = "device_id";
    public static final String QUERY_DURATION = "duration";
    public static final String QUERY_EXPIRES_IN = "expires_in";
    public static final String QUERY_ID = "id";
    public static final String QUERY_VOTE = "dir";
    public static final String QUERY_CATEGORY = "category";

    private static Reddit reddit;
    private Context context;
    private RequestQueue requestQueue;
    private SharedPreferences preferences;

    private Reddit(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        preferences = PreferenceManager.getDefaultSharedPreferences(
                context.getApplicationContext());
        this.context = context.getApplicationContext();
    }

    public static Reddit getInstance(Context context) {
        if (reddit == null) {
            reddit = new Reddit(context);
        }
        return reddit;
    }

    public boolean needsToken() {
        return preferences.getLong(AppSettings.EXPIRE_TIME,
                Long.MAX_VALUE) < System.currentTimeMillis() || "".equals(preferences.getString(
                AppSettings.ACCESS_TOKEN, ""));

    }

    public String getUserAuthUrl(String state) {

        return USER_AUTHENTICATION_URL + QUERY_CLIENT_ID + "=" + CLIENT_ID + "&response_type=code&state=" + state + "&" + QUERY_REDIRECT_URI + "=" + REDIRECT_URI + "&" + QUERY_DURATION + "=permanent&scope=" + AUTH_SCOPES;
    }

    public Request<String> fetchToken(final RedditErrorListener listener) {

        final HashMap<String, String> params = new HashMap<>(2);

        if (TextUtils.isEmpty(preferences.getString(AppSettings.REFRESH_TOKEN, ""))) {
            if ("".equals(preferences.getString(AppSettings.DEVICE_ID, ""))) {
                preferences.edit()
                        .putString(AppSettings.DEVICE_ID, UUID.randomUUID()
                                .toString())
                        .commit();
            }

            params.put(QUERY_GRANT_TYPE, INSTALLED_CLIENT_GRANT);
            params.put(QUERY_DEVICE_ID,
                    preferences.getString(AppSettings.DEVICE_ID, UUID.randomUUID()
                            .toString()));
        }
        else {
            params.put(QUERY_GRANT_TYPE, QUERY_REFRESH_TOKEN);
            params.put(QUERY_REFRESH_TOKEN, preferences.getString(AppSettings.REFRESH_TOKEN, ""));
        }

        return loadPostDefault(ACCESS_URL, new Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    preferences.edit()
                            .putString(AppSettings.ACCESS_TOKEN,
                                    jsonObject.getString(QUERY_ACCESS_TOKEN))
                            .commit();
                    preferences.edit()
                            .putLong(AppSettings.EXPIRE_TIME,
                                    System.currentTimeMillis() + jsonObject.getLong(
                                            QUERY_EXPIRES_IN) * SEC_TO_MS)
                            .commit();
                }
                catch (JSONException e1) {
                    e1.printStackTrace();
                }

                if (listener != null) {
                    listener.onErrorHandled();
                }

            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }, params);

    }

    public Request<String> loadPostDefault(final String url,
            final Listener<String> listener,
            final ErrorListener errorListener,
            final Map<String, String> params) {

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                listener, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                String credentials = CLIENT_ID + ":";
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(),
                        Base64.DEFAULT);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, auth);
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(postRequest);
    }


    /**
     * HTTP POST call to Reddit OAuth API
     *
     * @param url
     * @param listener
     * @param errorListener
     * @param iteration
     */
    public Request<String> loadPost(final String url,
            final Listener<String> listener,
            final ErrorListener errorListener,
            final Map<String, String> params,
            final int iteration) {

        if (iteration > 2) {
            errorListener.onErrorResponse(null);
            return null;
        }

        if (needsToken()) {
            fetchToken(new RedditErrorListener() {
                @Override
                public void onErrorHandled() {
                    loadPost(url, listener, errorListener, params, iteration + 1);
                }
            });
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.POST, url,
                listener, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "loadPost error: " + error);
                Toast.makeText(context, "loadPost error: " + error, Toast.LENGTH_SHORT).show();
                errorListener.onErrorResponse(error);
            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, getAuthorizationHeader());
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);
    }

    /**
     * HTTP GET call to Reddit OAuth API, query parameters must exist inside url param
     *
     * @param url
     * @param listener
     * @param errorListener
     * @param iteration
     */
    public Request<String> loadGet(final String url,
            final Listener<String> listener,
            final ErrorListener errorListener,
            final int iteration) {

        if (iteration > 2) {
            errorListener.onErrorResponse(null);
            return null;
        }

        if (needsToken()) {
            fetchToken(new RedditErrorListener() {
                @Override
                public void onErrorHandled() {
                    loadGet(url, listener, errorListener, iteration + 1);
                }
            });
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                listener, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "loadGet error: " + error);
                Toast.makeText(context, "loadGet error: " + error, Toast.LENGTH_SHORT).show();
                errorListener.onErrorResponse(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, getAuthorizationHeader());
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);
    }

    public void voteLink(final RecyclerView.ViewHolder viewHolder,
            final Link link,
            int vote,
            final VoteResponseListener voteResponseListener) {
        final int position = viewHolder.getAdapterPosition();

        final int oldVote = link.isLikes();
        int newVote = 0;

        if (link.isLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, link.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        link.setLikes(newVote);
        if (position == viewHolder.getAdapterPosition()) {
            if (viewHolder instanceof AdapterLinkList.ViewHolder) {
                ((AdapterLinkList.ViewHolder) viewHolder).setVoteColors();
                ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo(link);
            }
            else if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                ((AdapterLinkGrid.ViewHolder) viewHolder).setVoteColors();
            }
        }
        loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                voteResponseListener.onVoteFailed();
                link.setLikes(oldVote);
                if (position == viewHolder.getAdapterPosition()) {
                    if (viewHolder instanceof AdapterLinkList.ViewHolder) {
                        ((AdapterLinkList.ViewHolder) viewHolder).setVoteColors();
                        ((AdapterLinkList.ViewHolder) viewHolder).setTextInfo(link);
                    }
                    else if (viewHolder instanceof AdapterLinkGrid.ViewHolder) {
                        ((AdapterLinkGrid.ViewHolder) viewHolder).setVoteColors();
                    }
                }
            }
        }, params, 0);
    }

    public boolean voteComment(final AdapterCommentList.ViewHolderComment viewHolder, final Comment comment, int vote, final VoteResponseListener voteResponseListener) {

        final int position = viewHolder.getAdapterPosition();
        final int oldVote = comment.isLikes();
        int newVote = 0;

        if (comment.isLikes() != vote) {
            newVote = vote;
        }

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, comment.getName());
        params.put(Reddit.QUERY_VOTE, String.valueOf(newVote));

        comment.setLikes(newVote);
        if (position == viewHolder.getAdapterPosition()) {
            viewHolder.setVoteColors();
        }
        reddit.loadPost(Reddit.OAUTH_URL + "/api/vote", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                voteResponseListener.onVoteFailed();

                comment.setLikes(oldVote);
                if (position == viewHolder.getAdapterPosition()) {
                    viewHolder.setVoteColors();
                }
            }
        }, params, 0);
        return true;
    }

    public void save(Thing thing, String category, ErrorListener errorListener) {

        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, thing.getName());
        if (!TextUtils.isEmpty(category)) {
            params.put(Reddit.QUERY_CATEGORY, category);
        }

        reddit.loadPost(Reddit.OAUTH_URL + "/api/save", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, errorListener, params, 0);
    }

    public void unsave(Thing thing) {
        HashMap<String, String> params = new HashMap<>(2);
        params.put(Reddit.QUERY_ID, thing.getName());

        reddit.loadPost(Reddit.OAUTH_URL + "/api/unsave", new Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        }, params, 0);
    }

    public Request<String> loadImgurImage(String id,
            Listener<String> listener,
            final ErrorListener errorListener,
            final int iteration) {

        if (iteration > 2) {
            errorListener.onErrorResponse(null);
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, IMGUR_IMAGE_URL + id,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION);
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);

    }

    public Request<String> loadImgurAlbum(String id,
            Listener<String> listener,
            final ErrorListener errorListener,
            final int iteration) {

        if (iteration > 2) {
            errorListener.onErrorResponse(null);
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, IMGUR_ALBUM_URL + id,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION);
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);

    }

    public Request<String> loadImgurGallery(String id,
            Listener<String> listener,
            final ErrorListener errorListener,
            final int iteration) {

        if (iteration > 2) {
            errorListener.onErrorResponse(null);
            return null;
        }

        StringRequest getRequest = new StringRequest(Request.Method.GET, IMGUR_GALLERY_URL + id,
                listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>(3);
                headers.put(USER_AGENT, CUSTOM_USER_AGENT);
                headers.put(AUTHORIZATION, ApiKeys.IMGUR_AUTHORIZATION);
                headers.put(CONTENT_TYPE, CONTENT_TYPE_APP_JSON);
                return headers;
            }
        };

        return requestQueue.add(getRequest);

    }

    private String getAuthorizationHeader() {
        return Reddit.BEARER + preferences.getString(AppSettings.ACCESS_TOKEN, "");
    }

    public static boolean checkIsImage(String url) {
        return url.endsWith(GIF) || url.endsWith(PNG) || url.endsWith(JPG)
                || url.endsWith(JPEG) || url.endsWith(WEBP);
    }

    /**
     * Sets link's URL to proper image format if applicable
     *
     * @param link to set URL
     * @return try if link is single image file, false otherwise
     */
    public static boolean placeImageUrl(Link link) {

        String url = link.getUrl();
        if (!url.contains("http")) {
            url += "http://";
        }
        // TODO: Add support for popular image domains
        String domain = link.getDomain();
        if (domain.contains("imgur")) {
            if (url.contains(",")) {
                return false;
            }
            else if (url.endsWith(Reddit.GIFV)) {
                return false;
            }
            else if (url.contains(".com/gallery")) {
                return false;
            }
            else if (url.contains(".com/a/")) {
                return false;
            }
            else if (!Reddit.checkIsImage(url)) {
                if (url.charAt(url.length() - 1) == '/') {
                    url = url.substring(0, url.length() - 2);
                }
                url += ".jpg";
            }
        }

        boolean isImage = Reddit.checkIsImage(url);
        if (!isImage) {
            return false;
        }

        link.setUrl(url);
        return true;
    }

    public static String getImageHtml(String src) {
        return "<html><head><meta name=\"viewport\" content=\"width=device-width, minimum-scale=0.1\"><style>img {width:100%;}</style></head><body style=\"margin: 0px;\"><img style=\"-webkit-user-select: none; cursor: zoom-in;\" src=\"" + src + "\"/></body></html>";
    }

    public static CharSequence getTrimmedHtml(String html) {
        if (TextUtils.isEmpty(html)) {
            return html;
        }

        html = Html.fromHtml(html.trim())
                .toString();

        CharSequence sequence = Html.fromHtml(html);

        // Trims leading and trailing whitespace
        int start = 0;
        int end = sequence.length();
        while (start < end && Character.isWhitespace(sequence.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
            end--;
        }
        return sequence.subSequence(start, end);
    }

    public interface RedditErrorListener {
        void onErrorHandled();
    }

    public interface VoteResponseListener {
        void onVoteFailed();
    }

}
