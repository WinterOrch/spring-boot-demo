package com.winter.exphandler.exception;

import com.winter.exphandler.Status;

public class JsonException extends AbstractException {

    public JsonException(Status status) {
        super(status);
    }

    public JsonException(Integer code, String message) {
        super(code, message);
    }
}
