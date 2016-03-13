/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;

/**
 * Created by TheKeeperOfPie on 2/6/2016.
 */
public class UtilsImage {

    public static final String TAG = UtilsImage.class.getCanonicalName();

    public static final String GIFV = ".gifv";
    public static final String GIF = ".gif";
    public static final String PNG = ".png";
    public static final String JPG = ".jpg";
    public static final String JPEG = ".jpeg";
    public static final String WEBP = ".webp";

    public static int MAX_TEXTURE_SIZE = -1;

    /**
     * Ensures we know the OpenGL texture size limit. This is a hack.
     */
    public static void checkMaxTextureSize(final Handler handler, final Runnable runnable) {
        if (MAX_TEXTURE_SIZE > 0) {
            runnable.run();
        }
        else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int[] numConfigs = new int[1];
                    int[] configAttrs = {EGL14.EGL_RENDERABLE_TYPE,
                            EGL14.EGL_OPENGL_ES2_BIT,
                            EGL14.EGL_RED_SIZE, 8,
                            EGL14.EGL_GREEN_SIZE, 8,
                            EGL14.EGL_BLUE_SIZE, 8,
                            EGL14.EGL_ALPHA_SIZE, 0,
                            EGL14.EGL_DEPTH_SIZE, 0,
                            EGL14.EGL_STENCIL_SIZE, 0,
                            EGL14.EGL_NONE};
                    int[] contextAttrs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
                    int[] surfaceAttrs = {EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE};

                    EGLConfig[] configs = new EGLConfig[1];

                    EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
                    EGL14.eglChooseConfig(eglDisplay, configAttrs, 0, configs, 0, 1, numConfigs, 0);

                    EGLContext eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttrs, 0);
                    EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttrs, 0);

                    EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

                    int[] array = new int[1];

                    GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, array, 0);

                    Log.d(TAG, "calculateTextureSize() called with: " + array[0]);

                    MAX_TEXTURE_SIZE = array[0];

                    EGL14.eglDestroyContext(eglDisplay, eglContext);
                    EGL14.eglDestroySurface(eglDisplay, eglSurface);
                    EGL14.eglReleaseThread();

                    handler.post(runnable);
                }
            }).start();
        }
    }

    public static int getMaxTextureSize(){
        return MAX_TEXTURE_SIZE;
    }

    public static void calculateTextureSize() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int[] numConfigs = new int[1];
                int[] configAttrs = {EGL14.EGL_RENDERABLE_TYPE,
                        EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_RED_SIZE, 8,
                        EGL14.EGL_GREEN_SIZE, 8,
                        EGL14.EGL_BLUE_SIZE, 8,
                        EGL14.EGL_ALPHA_SIZE, 0,
                        EGL14.EGL_DEPTH_SIZE, 0,
                        EGL14.EGL_STENCIL_SIZE, 0,
                        EGL14.EGL_NONE};
                int[] contextAttrs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
                int[] surfaceAttrs = {EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE};

                EGLConfig[] configs = new EGLConfig[1];

                EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
                EGL14.eglChooseConfig(eglDisplay, configAttrs, 0, configs, 0, 1, numConfigs, 0);

                EGLContext eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttrs, 0);
                EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttrs, 0);

                EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

                int[] array = new int[1];

                GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, array, 0);

                Log.d(TAG, "calculateTextureSize() called with: " + array[0]);

                MAX_TEXTURE_SIZE = array[0];

                EGL14.eglDestroyContext(eglDisplay, eglContext);
                EGL14.eglDestroySurface(eglDisplay, eglSurface);
                EGL14.eglTerminate(eglDisplay);
                EGL14.eglReleaseThread();
            }
        }).start();
    }

    public static Drawable getDrawableForLink(Context context, Link link) {
        if (link.isSelf()) {
            return context.getResources().getDrawable(R.drawable.ic_chat_white_48dp);
        }

        if (Reddit.DEFAULT.equals(link.getThumbnail())) {
            return context.getResources().getDrawable(R.drawable.ic_web_white_48dp);
        }

        return null;
    }

    @Nullable
    public static String parseThumbnail(Link link) {
        if (URLUtil.isNetworkUrl(link.getThumbnail())) {
            return link.getThumbnail();
        }

        for (Link.Preview.Image image : link.getPreview().getImages()) {
            for (Link.Preview.Image.Thumbnail thumbnail : image.getResolutions()) {
                if (URLUtil.isNetworkUrl(thumbnail.getUrl())) {
                    return thumbnail.getUrl();
                }
            }

            Link.Preview.Image.Thumbnail source = image.getSource();
            if (source != null && URLUtil.isNetworkUrl(source.getUrl())) {
                return source.getUrl();
            }
        }

        String thumbnailUrl = link.getMedia().getOembed().getThumbnailUrl();
        if (URLUtil.isNetworkUrl(thumbnailUrl)) {
            return thumbnailUrl;
        }

        return null;
    }

    @Nullable
    public static String parseSourceImage(Link link) {
        for (Link.Preview.Image image : link.getPreview().getImages()) {
            Link.Preview.Image.Thumbnail source = image.getSource();
            if (source != null && URLUtil.isNetworkUrl(source.getUrl())) {
                return source.getUrl();
            }

            for (int index = image.getResolutions().size() - 1; index >= 0; index--) {
                Link.Preview.Image.Thumbnail thumbnail = image.getResolutions().get(index);
                if (URLUtil.isNetworkUrl(thumbnail.getUrl())) {
                    return thumbnail.getUrl();
                }
            }
        }

        String thumbnailUrl = link.getMedia().getOembed().getThumbnailUrl();
        if (URLUtil.isNetworkUrl(thumbnailUrl)) {
            return thumbnailUrl;
        }

        if (URLUtil.isNetworkUrl(link.getThumbnail())) {
            return link.getThumbnail();
        }

        return null;
    }

    public static boolean checkIsImageUrl(String url) {
        if (URLUtil.isNetworkUrl(url)) {
            return endsWithImageExtension(Uri.parse(url).getPath());
        }

        return endsWithImageExtension(url);
    }

    public static boolean endsWithImageExtension(String url) {
        return url.endsWith(GIF) || url.endsWith(PNG) || url.endsWith(JPG)
                || url.endsWith(JPEG) || url.endsWith(WEBP);
    }

    public static boolean endsWithImageExtension(String url, String extension) {
        if (URLUtil.isNetworkUrl(url)) {
            return Uri.parse(url).getPath().endsWith(extension);
        }

        return url.endsWith(extension);
    }

    public static String getImageFileEnding(String url) {
        if (url.endsWith(PNG)) {
            return PNG;
        }
        if (url.endsWith(JPG)) {
            return JPG;
        }
        if (url.endsWith(JPEG)) {
            return JPEG;
        }
        if (url.endsWith(WEBP)) {
            return WEBP;
        }
        if (url.endsWith(GIF)) {
            return GIF;
        }

        return PNG;
    }

    public static boolean showThumbnail(Link link) {
        if (TextUtils.isEmpty(link.getUrl())) {
            return false;
        }
        String domain = link.getDomain();
        return domain.contains("gfycat") || domain.contains("imgur") || placeImageUrl(link);
    }

    /**
     * Sets link's URL to proper image format if applicable
     *
     * @param link to set URL
     * @return true if link is single image file, false otherwise
     */
    public static boolean placeImageUrl(Link link) {

        String url = link.getUrl();
        if (!url.startsWith("http")) {
            url += "http://";
        }

        // TODO: Add support for popular image domains
        String domain = link.getDomain();
        if (domain.contains("imgur")) {
            if (url.contains(",")) {
                return false;
            }
            else if (endsWithImageExtension(url, GIFV)) {
                return false;
            }
            else if (url.contains(".com/gallery")) {
                return false;
            }
            else if (url.contains(".com/a/")) {
                return false;
            }
            else if (!checkIsImageUrl(url)) {
                if (url.charAt(url.length() - 1) == '/') {
                    url = url.substring(0, url.length() - 2);
                }
                url += ".jpg";
            }
        }

        boolean isImage = checkIsImageUrl(url);
        if (!isImage) {
            return false;
        }

        link.setUrl(url);
        return true;
    }

    public static String getImageHtml(String src) {
        return "<html>" +
                "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=0.1\">" +
                "<style>" +
                "    img {" +
                "        width:100%;" +
                "    }" +
                "    body {" +
                "        margin:0px;" +
                "    }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<img src=\"" + src + "\"/>" +
                "</body>" +
                "</html>";
    }

    public static String getImageHtmlForAlbum(String src, CharSequence title, CharSequence description, int textColor, int margin) {

        String rgbText = "rgb(" + Color.red(textColor) + ", " + Color.green(textColor) + ", " + Color.blue(textColor) + ")";

        String htmlTitle = TextUtils.isEmpty(title) ? "" : "<h2>" + title + "</h2>";
        String htmlDescription = TextUtils.isEmpty(description) ? "" : "<p>" + Html.escapeHtml(description) + "</p>";

        return "<html>" +
                "<head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=0.1\">" +
                "<style>" +
                "    img {" +
                "        width:100%;" +
                "    }" +
                "    body {" +
                "        color: " + rgbText + ";" +
                "        margin:0px;" +
                "    }" +
                "    h2 {" +
                "        margin-left:" + margin + "px;" +
                "        margin-right:" + margin + "px;" +
                "    }" +
                "    p {" +
                "        margin-left:" + margin + "px;" +
                "        margin-right:" + margin + "px;" +
                "        margin-bottom:" + margin + "px;" +
                "    }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<img src=\"" + src + "\"/>" +
                htmlTitle +
                htmlDescription +
                "</body>" +
                "</html>";
    }

}
