package model.movie;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bảng MovieMedia: poster và trailer của phim. */
public class MovieMedia {

    public static final String TYPE_POSTER = "POSTER";
    public static final String TYPE_TRAILER = "TRAILER";

    private static final Pattern YOUTUBE_ID = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})");

    private int mediaId;
    private int movieId;
    private String mediaType;
    private String url;
    private String caption;
    private boolean primary;
    private LocalDateTime createdAt;

    public boolean isPoster() { return TYPE_POSTER.equals(mediaType); }

    /** Link nhúng YouTube, hoặc null nếu không phải link YouTube. */
    public String getYoutubeEmbedUrl() {
        if (url == null) return null;
        Matcher m = YOUTUBE_ID.matcher(url);
        return m.find() ? "https://www.youtube.com/embed/" + m.group(1) : null;
    }

    public int getMediaId() { return mediaId; }
    public void setMediaId(int mediaId) { this.mediaId = mediaId; }
    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
