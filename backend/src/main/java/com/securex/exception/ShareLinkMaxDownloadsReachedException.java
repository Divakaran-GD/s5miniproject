package com.securex.exception;

public class ShareLinkMaxDownloadsReachedException extends RuntimeException {
    public ShareLinkMaxDownloadsReachedException(String message) {
        super(message);
    }
}
