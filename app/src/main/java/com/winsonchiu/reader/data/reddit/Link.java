/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.dagger.components.ComponentStatic;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.utils.UtilsJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Link extends Replyable implements Parcelable {

    public static final String TAG = Link.class.getCanonicalName();

    public static Func1<String, Observable<Link>> COMMENTS = new Func1<String, Observable<Link>>() {
        @Override
        public Observable<Link> call(String response) {
            try {
                return Observable.just(Link.fromJsonWithComments(
                        ComponentStatic.getObjectMapper().readValue(response,
                                JsonNode.class)));
            } catch (IOException e) {
                return Observable.error(e);
            }
        }
    };

    private String author = "";
    private String authorFlairCssClass = "";
    private String authorFlairText = "";
    private boolean clicked;
    private String domain = "";
    private boolean hidden;
    private boolean isSelf;
    private int likes;
    private String linkFlairCssClass = "";
    private String linkFlairText = "";
    private Media media = new Media();
    private String mediaEmbed = "";
    private int numComments;
    private boolean over18;
    private String permalink = "";
    private Preview preview = new Preview();
    private boolean saved;
    private int score;
    private CharSequence selfText = "";
    private CharSequence selfTextHtml = "";
    private String subreddit = "";
    private String subredditId = "";
    private Sort suggestedSort = Sort.CONFIDENCE;
    private String thumbnail = "";
    private String title = "";
    private String url = "";
    private long edited;
    private Reddit.Distinguished distinguished;
    private boolean stickied;
    private long created;
    private long createdUtc;

    private Listing comments = new Listing();
    private Album album;
    private boolean commentsClicked;
    private int backgroundColor;

    private int contextLevel;
    private String commentId;

    public static Link fromJson(JsonNode nodeRoot) {

        // TODO: Move parsing of HTML to asynchronous thread

        Link link = new Link();
        link.setJson(nodeRoot.toString());
        link.setKind(UtilsJson.getString(nodeRoot.get("kind")));

        JsonNode nodeData = nodeRoot.get("data");

        link.setId(UtilsJson.getString(nodeData.get("id")));
        link.setName(UtilsJson.getString(nodeData.get("name")));

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        link.setCreated(UtilsJson.getLong(nodeData.get("created")) * 1000);
        link.setCreatedUtc(UtilsJson.getLong(nodeData.get("created_utc")) * 1000);

        link.setAuthor(UtilsJson.getString(nodeData.get("author")));
        link.setAuthorFlairCssClass(UtilsJson.getString(
                nodeData.get("author_flair_css_class")));
        link.setAuthorFlairText(UtilsJson.getString(nodeData.get("author_flair_text")));
        link.setClicked(UtilsJson.getBoolean(nodeData.get("clicked")));
        link.setDomain(UtilsJson.getString(nodeData.get("domain")));
        link.setHidden(UtilsJson.getBoolean(nodeData.get("hidden")));
        link.setSelf(UtilsJson.getBoolean(nodeData.get("is_self")));

        switch (UtilsJson.getString(nodeData.get("likes"))) {
            case "null":
                link.setLikes(0);
                break;
            case "true":
                link.setLikes(1);
                break;
            case "false":
                link.setLikes(-1);
                break;
        }

        link.setLinkFlairCssClass(UtilsJson.getString(nodeData.get("link_flair_css_class")));
        link.setLinkFlairText(Html.fromHtml(
                UtilsJson.getString(nodeData.get("link_flair_text"))).toString());
        if (link.getLinkFlairText().equals("null")) {
            link.setLinkFlairText("");
        }
        link.setMedia(Media.fromJson(nodeData.get("media")));
        link.setMediaEmbed(UtilsJson.getString(nodeData.get("media_embed")));
        link.setNumComments(UtilsJson.getInt(nodeData.get("num_comments")));
        link.setOver18(UtilsJson.getBoolean(nodeData.get("over_18")));
        link.setPermalink(UtilsJson.getString(nodeData.get("permalink")));
        link.setPreview(Preview.fromJson(nodeData.get("preview")));
        link.setSaved(UtilsJson.getBoolean(nodeData.get("saved")));
        link.setScore(UtilsJson.getInt(nodeData.get("score")));
        link.setSelfText(Html.fromHtml(UtilsJson.getString(
                nodeData.get("selftext")).replaceAll("\n", "<br>")));
        link.setSelfTextHtml(Reddit.getFormattedHtml(UtilsJson.getString(
                nodeData.get("selftext_html"))));
        link.setSubreddit(UtilsJson.getString(nodeData.get("subreddit")));
        link.setSubredditId(UtilsJson.getString(nodeData.get("subreddit_id")));
        link.setSuggestedSort(Sort.fromString(UtilsJson.getString(nodeData.get("suggested_sort"))));
        link.setThumbnail(UtilsJson.getString(nodeData.get("thumbnail")));
        link.setTitle(Html.fromHtml(UtilsJson.getString(nodeData.get("title"))).toString());
        link.setUrl(String.valueOf(Html.fromHtml(UtilsJson.getString(nodeData.get("url")))));

        String edited = UtilsJson.getString(nodeData.get("edited"));
        switch (edited) {
            case "true":
                link.setEdited(1);
                break;
            case "false":
                link.setEdited(0);
                break;
            default:
                link.setEdited(UtilsJson.getLong(nodeData.get("edited")) * 1000);
                break;
        }

        switch (UtilsJson.getString(nodeData.get("distinguished"))) {
            case "null":
                link.setDistinguished(Reddit.Distinguished.NOT_DISTINGUISHED);
                break;
            case "moderator":
                link.setDistinguished(Reddit.Distinguished.MODERATOR);
                break;
            case "admin":
                link.setDistinguished(Reddit.Distinguished.ADMIN);
                break;
            case "special":
                link.setDistinguished(Reddit.Distinguished.SPECIAL);
                break;
        }

        link.setStickied(UtilsJson.getBoolean(nodeData.get("stickied")));

        return link;
    }

    public static Link fromJsonWithComments(JsonNode nodeRoot) throws IOException {

        Link link = fromJson(nodeRoot.get(0)
                .get("data")
                .get("children")
                .get(0));

        link.setComments(Listing.fromJson(nodeRoot.get(1)));

        return link;
    }

    public Link() {
        super();
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorFlairCssClass() {
        return authorFlairCssClass;
    }

    public void setAuthorFlairCssClass(String authorFlairCssClass) {
        this.authorFlairCssClass = authorFlairCssClass;
    }

    public String getAuthorFlairText() {
        return authorFlairText;
    }

    public void setAuthorFlairText(String authorFlairText) {
        this.authorFlairText = authorFlairText;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getLinkFlairCssClass() {
        return linkFlairCssClass;
    }

    public void setLinkFlairCssClass(String linkFlairCssClass) {
        this.linkFlairCssClass = linkFlairCssClass;
    }

    public String getLinkFlairText() {
        return linkFlairText;
    }

    public void setLinkFlairText(String linkFlairText) {
        this.linkFlairText = linkFlairText;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public String getMediaEmbed() {
        return mediaEmbed;
    }

    public void setMediaEmbed(String mediaEmbed) {
        this.mediaEmbed = mediaEmbed;
    }

    public int getNumComments() {
        return numComments;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public boolean isOver18() {
        return over18;
    }

    public void setOver18(boolean over18) {
        this.over18 = over18;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public Preview getPreview() {
        return preview;
    }

    public void setPreview(Preview preview) {
        this.preview = preview;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public CharSequence getSelfText() {
        return selfText;
    }

    public void setSelfText(CharSequence selfText) {
        if (Reddit.NULL.equals(selfText)) {
            selfText = "";
        }
        this.selfText = selfText;
    }

    public CharSequence getSelfTextHtml() {
        return selfTextHtml;
    }

    public void setSelfTextHtml(CharSequence selfTextHtml) {
        if (Reddit.NULL.equals(selfText)) {
            selfText = "";
        }
        this.selfTextHtml = selfTextHtml;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public String getSubredditId() {
        return subredditId;
    }

    public void setSubredditId(String subredditId) {
        this.subredditId = subredditId;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getEdited() {
        return edited;
    }

    public void setEdited(long edited) {
        this.edited = edited;
    }

    public Reddit.Distinguished getDistinguished() {
        return distinguished;
    }

    public void setDistinguished(Reddit.Distinguished distinguished) {
        this.distinguished = distinguished;
    }

    public boolean isStickied() {
        return stickied;
    }

    public void setStickied(boolean stickied) {
        this.stickied = stickied;
    }

    public Listing getComments() {
        return comments;
    }

    public void setComments(Listing comments) {
        this.comments = comments;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getCreatedUtc() {
        return createdUtc;
    }

    public void setCreatedUtc(long createdUtc) {
        this.createdUtc = createdUtc;
    }

    public boolean isCommentsClicked() {
        return commentsClicked;
    }

    public void setCommentsClicked(boolean commentsClicked) {
        this.commentsClicked = commentsClicked;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public CharSequence getParentHtml() {
        return getSelfTextHtml();
    }

    public Sort getSuggestedSort() {
        return suggestedSort;
    }

    public void setSuggestedSort(Sort suggestedSort) {
        this.suggestedSort = suggestedSort;
    }

    @Override
    public String toString() {
        return "Link{" +
                "album=" + album +
                ", author='" + author + '\'' +
                ", authorFlairCssClass='" + authorFlairCssClass + '\'' +
                ", authorFlairText='" + authorFlairText + '\'' +
                ", clicked=" + clicked +
                ", domain='" + domain + '\'' +
                ", hidden=" + hidden +
                ", isSelf=" + isSelf +
                ", likes=" + likes +
                ", linkFlairCssClass='" + linkFlairCssClass + '\'' +
                ", linkFlairText='" + linkFlairText + '\'' +
                ", media='" + media + '\'' +
                ", mediaEmbed='" + mediaEmbed + '\'' +
                ", numComments=" + numComments +
                ", over18=" + over18 +
                ", permalink='" + permalink + '\'' +
                ", saved=" + saved +
                ", score=" + score +
                ", selfText=" + selfText +
                ", selfTextHtml=" + selfTextHtml +
                ", subreddit='" + subreddit + '\'' +
                ", subredditId='" + subredditId + '\'' +
                ", suggestedSort=" + suggestedSort +
                ", thumbnail='" + thumbnail + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", edited=" + edited +
                ", distinguished=" + distinguished +
                ", stickied=" + stickied +
                ", created=" + created +
                ", createdUtc=" + createdUtc +
                ", comments=" + comments +
                ", commentsClicked=" + commentsClicked +
                ", backgroundColor=" + backgroundColor +
                '}';
    }

    public int getContextLevel() {
        return contextLevel;
    }

    public void setContextLevel(int contextLevel) {
        this.contextLevel = contextLevel;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public static class Media implements Parcelable {

        private String type;
        private Oembed oembed = new Oembed();

        public static Media fromJson(JsonNode nodeRoot) {
            Media media = new Media();

            if (nodeRoot == null) {
                return media;
            }

            media.setType(UtilsJson.getString(nodeRoot.get("type")));
            media.setOembed(Oembed.fromJson(nodeRoot.get("oembed")));

            return media;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Oembed getOembed() {
            return oembed;
        }

        public void setOembed(Oembed oembed) {
            this.oembed = oembed;
        }

        public static class Oembed implements Parcelable {

            private String type;
            private String version;
            private String html;
            private String title;
            private String description;
            private String providerName;
            private String providerUrl;
            private int width;
            private int height;
            private int thumbnailWidth;
            private int thumbnailHeight;
            private String thumbnailUrl;

            public static Oembed fromJson(JsonNode nodeRoot) {
                Oembed oembed = new Oembed();

                if (nodeRoot == null) {
                    return oembed;
                }

                oembed.setType(UtilsJson.getString(nodeRoot.get("type")));
                oembed.setVersion(UtilsJson.getString(nodeRoot.get("version")));
                oembed.setHtml(UtilsJson.getString(nodeRoot.get("html")));
                oembed.setTitle(UtilsJson.getString(nodeRoot.get("title")));
                oembed.setDescription(UtilsJson.getString(nodeRoot.get("description")));
                oembed.setProviderName(UtilsJson.getString(nodeRoot.get("provider_name")));
                oembed.setProviderUrl(UtilsJson.getString(nodeRoot.get("provider_url")));
                oembed.setWidth(UtilsJson.getInt(nodeRoot.get("width")));
                oembed.setHeight(UtilsJson.getInt(nodeRoot.get("height")));
                oembed.setThumbnailWidth(UtilsJson.getInt(nodeRoot.get("thumbnail_width")));
                oembed.setThumbnailHeight(UtilsJson.getInt(nodeRoot.get("thumbnail_height")));
                oembed.setThumbnailUrl(UtilsJson.getString(nodeRoot.get("thumbnail_url")));

                return oembed;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getVersion() {
                return version;
            }

            public void setVersion(String version) {
                this.version = version;
            }

            public String getHtml() {
                return html;
            }

            public void setHtml(String html) {
                this.html = html;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getProviderName() {
                return providerName;
            }

            public void setProviderName(String providerName) {
                this.providerName = providerName;
            }

            public String getProviderUrl() {
                return providerUrl;
            }

            public void setProviderUrl(String providerUrl) {
                this.providerUrl = providerUrl;
            }

            public int getWidth() {
                return width;
            }

            public void setWidth(int width) {
                this.width = width;
            }

            public int getHeight() {
                return height;
            }

            public void setHeight(int height) {
                this.height = height;
            }

            public int getThumbnailWidth() {
                return thumbnailWidth;
            }

            public void setThumbnailWidth(int thumbnailWidth) {
                this.thumbnailWidth = thumbnailWidth;
            }

            public int getThumbnailHeight() {
                return thumbnailHeight;
            }

            public void setThumbnailHeight(int thumbnailHeight) {
                this.thumbnailHeight = thumbnailHeight;
            }

            public String getThumbnailUrl() {
                return thumbnailUrl;
            }

            public void setThumbnailUrl(String thumbnailUrl) {
                this.thumbnailUrl = thumbnailUrl;
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeString(this.type);
                dest.writeString(this.version);
                dest.writeString(this.html);
                dest.writeString(this.title);
                dest.writeString(this.description);
                dest.writeString(this.providerName);
                dest.writeString(this.providerUrl);
                dest.writeInt(this.width);
                dest.writeInt(this.height);
                dest.writeInt(this.thumbnailWidth);
                dest.writeInt(this.thumbnailHeight);
                dest.writeString(this.thumbnailUrl);
            }

            public Oembed() {
            }

            protected Oembed(Parcel in) {
                this.type = in.readString();
                this.version = in.readString();
                this.html = in.readString();
                this.title = in.readString();
                this.description = in.readString();
                this.providerName = in.readString();
                this.providerUrl = in.readString();
                this.width = in.readInt();
                this.height = in.readInt();
                this.thumbnailWidth = in.readInt();
                this.thumbnailHeight = in.readInt();
                this.thumbnailUrl = in.readString();
            }

            public static final Creator<Oembed> CREATOR = new Creator<Oembed>() {
                public Oembed createFromParcel(Parcel source) {
                    return new Oembed(source);
                }

                public Oembed[] newArray(int size) {
                    return new Oembed[size];
                }
            };
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.type);
            dest.writeParcelable(this.oembed, flags);
        }

        public Media() {
        }

        protected Media(Parcel in) {
            this.type = in.readString();
            this.oembed = in.readParcelable(Oembed.class.getClassLoader());
        }

        public static final Creator<Media> CREATOR = new Creator<Media>() {
            public Media createFromParcel(Parcel source) {
                return new Media(source);
            }

            public Media[] newArray(int size) {
                return new Media[size];
            }
        };
    }

    public static class Preview implements Parcelable {

        private List<Image> images = new ArrayList<>();

        public static Preview fromJson(JsonNode nodeRoot) {
            Preview preview = new Preview();

            if (nodeRoot == null) {
                return preview;
            }

            JsonNode nodeImages = nodeRoot.get("images");

            if (nodeImages != null && nodeImages.isArray()) {
                List<Image> images = new ArrayList<>(nodeImages.size());
                for (JsonNode node : nodeImages) {
                    images.add(Image.fromJson(node));
                }
                preview.setImages(images);
            }

            return preview;
        }

        public List<Image> getImages() {
            return images;
        }

        public void setImages(List<Image> images) {
            this.images = images;
        }

        public static class Image implements Parcelable {

            private Thumbnail source;
            private List<Thumbnail> resolutions = new ArrayList<>();
            private List<Image> variants = new ArrayList<>();
            private Image nsfw;

            public static Image fromJson(JsonNode nodeRoot) {
                Image image = new Image();
                image.setSource(Thumbnail.fromJson(nodeRoot.get("source")));

                JsonNode nodeResolutions = nodeRoot.get("resolutions");

                if (nodeResolutions != null && nodeResolutions.isArray()) {
                    List<Thumbnail> resolutions = new ArrayList<>(nodeResolutions.size());
                    for (JsonNode node : nodeResolutions) {
                        resolutions.add(Thumbnail.fromJson(node));
                    }

                    Collections.sort(resolutions, new Comparator<Thumbnail>() {
                        @Override
                        public int compare(Thumbnail lhs, Thumbnail rhs) {
                            int resolutionFirst = lhs.getWidth() * lhs.getHeight();
                            int resolutionSecond = rhs.getWidth() * rhs.getHeight();

                            return resolutionFirst < resolutionSecond ? -1 : (resolutionFirst == resolutionSecond ? 0 : 1);
                        }
                    });

                    image.setResolutions(resolutions);
                }

                JsonNode nodeVariants = nodeRoot.get("variants");

                if (nodeVariants != null) {
                    if (nodeVariants.isArray()) {
                        List<Image> variants = new ArrayList<>(nodeVariants.size());
                        for (JsonNode node : nodeVariants) {
                            variants.add(Image.fromJson(node));
                        }
                        image.setVariants(variants);
                    }
                    else {
                        JsonNode nodeNsfw = nodeVariants.get("nsfw");
                        if (nodeNsfw != null) {
                            image.setNsfw(Image.fromJson(nodeNsfw));
                        }
                    }
                }

                return image;
            }

            public Thumbnail getSource() {
                return source;
            }

            public void setSource(Thumbnail source) {
                this.source = source;
            }

            public List<Thumbnail> getResolutions() {
                return resolutions;
            }

            public void setResolutions(List<Thumbnail> resolutions) {
                this.resolutions = resolutions;
            }

            public List<Image> getVariants() {
                return variants;
            }

            public void setVariants(List<Image> variants) {
                this.variants = variants;
            }

            public Image getNsfw() {
                return nsfw;
            }

            public void setNsfw(Image nsfw) {
                this.nsfw = nsfw;
            }

            public static class Thumbnail implements Parcelable {
                private String url;
                private int width;
                private int height;

                public static Thumbnail fromJson(JsonNode nodeRoot) {
                    Thumbnail thumbnail = new Thumbnail();

                    thumbnail.setUrl(UtilsJson.getString(nodeRoot.get("url")));
                    thumbnail.setWidth(UtilsJson.getInt(nodeRoot.get("width")));
                    thumbnail.setHeight(UtilsJson.getInt(nodeRoot.get("height")));

                    return thumbnail;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                public int getWidth() {
                    return width;
                }

                public void setWidth(int width) {
                    this.width = width;
                }

                public int getHeight() {
                    return height;
                }

                public void setHeight(int height) {
                    this.height = height;
                }

                @Override
                public int describeContents() {
                    return 0;
                }

                @Override
                public void writeToParcel(Parcel dest, int flags) {
                    dest.writeString(this.url);
                    dest.writeInt(this.width);
                    dest.writeInt(this.height);
                }

                public Thumbnail() {
                }

                protected Thumbnail(Parcel in) {
                    this.url = in.readString();
                    this.width = in.readInt();
                    this.height = in.readInt();
                }

                public static final Creator<Thumbnail> CREATOR = new Creator<Thumbnail>() {
                    public Thumbnail createFromParcel(Parcel source) {
                        return new Thumbnail(source);
                    }

                    public Thumbnail[] newArray(int size) {
                        return new Thumbnail[size];
                    }
                };
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeParcelable(this.source, 0);
                dest.writeTypedList(resolutions);
                dest.writeList(this.variants);
                dest.writeParcelable(this.nsfw, flags);
            }

            public Image() {
            }

            protected Image(Parcel in) {
                this.source = in.readParcelable(Thumbnail.class.getClassLoader());
                this.resolutions = in.createTypedArrayList(Thumbnail.CREATOR);
                this.variants = new ArrayList<Image>();
                in.readList(this.variants, List.class.getClassLoader());
                this.nsfw = in.readParcelable(Image.class.getClassLoader());
            }

            public static final Creator<Image> CREATOR = new Creator<Image>() {
                public Image createFromParcel(Parcel source) {
                    return new Image(source);
                }

                public Image[] newArray(int size) {
                    return new Image[size];
                }
            };
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedList(images);
        }

        public Preview() {
        }

        protected Preview(Parcel in) {
            this.images = in.createTypedArrayList(Image.CREATOR);
        }

        public static final Creator<Preview> CREATOR = new Creator<Preview>() {
            public Preview createFromParcel(Parcel source) {
                return new Preview(source);
            }

            public Preview[] newArray(int size) {
                return new Preview[size];
            }
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.author);
        dest.writeString(this.authorFlairCssClass);
        dest.writeString(this.authorFlairText);
        dest.writeByte(clicked ? (byte) 1 : (byte) 0);
        dest.writeString(this.domain);
        dest.writeByte(hidden ? (byte) 1 : (byte) 0);
        dest.writeByte(isSelf ? (byte) 1 : (byte) 0);
        dest.writeInt(this.likes);
        dest.writeString(this.linkFlairCssClass);
        dest.writeString(this.linkFlairText);
        dest.writeParcelable(this.media, flags);
        dest.writeString(this.mediaEmbed);
        dest.writeInt(this.numComments);
        dest.writeByte(over18 ? (byte) 1 : (byte) 0);
        dest.writeString(this.permalink);
        dest.writeParcelable(this.preview, flags);
        dest.writeByte(saved ? (byte) 1 : (byte) 0);
        dest.writeInt(this.score);
        TextUtils.writeToParcel(this.selfText, dest, flags);
        TextUtils.writeToParcel(this.selfTextHtml, dest, flags);
        dest.writeString(this.subreddit);
        dest.writeString(this.subredditId);
        dest.writeInt(this.suggestedSort == null ? -1 : this.suggestedSort.ordinal());
        dest.writeString(this.thumbnail);
        dest.writeString(this.title);
        dest.writeString(this.url);
        dest.writeLong(this.edited);
        dest.writeInt(this.distinguished == null ? -1 : this.distinguished.ordinal());
        dest.writeByte(stickied ? (byte) 1 : (byte) 0);
        dest.writeLong(this.created);
        dest.writeLong(this.createdUtc);
        dest.writeParcelable(this.comments, 0);
        dest.writeParcelable(this.album, 0);
        dest.writeByte(commentsClicked ? (byte) 1 : (byte) 0);
        dest.writeInt(this.backgroundColor);
        dest.writeInt(this.contextLevel);
        dest.writeString(this.commentId);
    }

    protected Link(Parcel in) {
        this.author = in.readString();
        this.authorFlairCssClass = in.readString();
        this.authorFlairText = in.readString();
        this.clicked = in.readByte() != 0;
        this.domain = in.readString();
        this.hidden = in.readByte() != 0;
        this.isSelf = in.readByte() != 0;
        this.likes = in.readInt();
        this.linkFlairCssClass = in.readString();
        this.linkFlairText = in.readString();
        this.media = in.readParcelable(Media.class.getClassLoader());
        this.mediaEmbed = in.readString();
        this.numComments = in.readInt();
        this.over18 = in.readByte() != 0;
        this.permalink = in.readString();
        this.preview = in.readParcelable(Preview.class.getClassLoader());
        this.saved = in.readByte() != 0;
        this.score = in.readInt();
        this.selfText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.selfTextHtml = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.subreddit = in.readString();
        this.subredditId = in.readString();
        int tmpSuggestedSort = in.readInt();
        this.suggestedSort = tmpSuggestedSort == -1 ? null : Sort.values()[tmpSuggestedSort];
        this.thumbnail = in.readString();
        this.title = in.readString();
        this.url = in.readString();
        this.edited = in.readLong();
        int tmpDistinguished = in.readInt();
        this.distinguished = tmpDistinguished == -1 ? null : Reddit.Distinguished.values()[tmpDistinguished];
        this.stickied = in.readByte() != 0;
        this.created = in.readLong();
        this.createdUtc = in.readLong();
        this.comments = in.readParcelable(Listing.class.getClassLoader());
        this.album = in.readParcelable(Album.class.getClassLoader());
        this.commentsClicked = in.readByte() != 0;
        this.backgroundColor = in.readInt();
        this.contextLevel = in.readInt();
        this.commentId = in.readString();
    }

    public static final Creator<Link> CREATOR = new Creator<Link>() {
        public Link createFromParcel(Parcel source) {
            return new Link(source);
        }

        public Link[] newArray(int size) {
            return new Link[size];
        }
    };
}