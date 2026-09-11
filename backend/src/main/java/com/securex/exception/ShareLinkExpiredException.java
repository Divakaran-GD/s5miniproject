package com.securex.exception;

public class ShareLinkExpiredException extends RuntimeException {
    public ShareLinkExpiredException(String message) {
        super(message);
    }
}
