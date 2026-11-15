package edu.utd.se3354.team10.rhrs.model;

/** A photo attached to a report. NFR (Space): photos may not exceed 5 MB. */
public class Photo {
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final String photoId;
    private final String url;
    private final long sizeBytes;

    public Photo(String photoId, String url, long sizeBytes) {
        this.photoId = photoId;
        this.url = url;
        this.sizeBytes = sizeBytes;
    }

    public String getPhotoId() { return photoId; }
    public String getUrl() { return url; }
    public long getSizeBytes() { return sizeBytes; }

    public boolean isValidSize() {
        return sizeBytes > 0 && sizeBytes <= MAX_SIZE_BYTES;
    }
}
