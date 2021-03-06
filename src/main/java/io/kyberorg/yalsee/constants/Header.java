package io.kyberorg.yalsee.constants;

/**
 * Header names.
 *
 * @since 2.0
 */
public final class Header {

    public static final String LOCATION = "Location";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String ACCEPT = "Accept";
    public static final String X_DEVELOPER = "X-Developer";
    public static final String X_YALSEE_TOKEN = "X-Yalsee-Token";

    private Header() {
        throw new UnsupportedOperationException("Utility class");
    }
}
