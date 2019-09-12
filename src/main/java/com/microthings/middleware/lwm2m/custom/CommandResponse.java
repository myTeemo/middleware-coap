package com.microthings.middleware.lwm2m.custom;

/**
 * @author MY-HE
 * @date 2019-09-10 21:25
 */

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.response.AbstractLwM2mResponse;

public class CommandResponse extends AbstractLwM2mResponse {

    public CommandResponse(ResponseCode code, String errorMessage) {
        this(code, errorMessage, null);
    }

    public CommandResponse(ResponseCode code, String errorMessage,
                           Object coapResponse) {
        super(code, errorMessage, coapResponse);
    }

    @Override
    public boolean isSuccess() {
        return getCode() == ResponseCode.CHANGED;
    }

    @Override
    public boolean isValid() {
        switch (code.getCode()) {
            case ResponseCode.CHANGED_CODE:
            case ResponseCode.BAD_REQUEST_CODE:
            case ResponseCode.UNAUTHORIZED_CODE:
            case ResponseCode.NOT_FOUND_CODE:
            case ResponseCode.METHOD_NOT_ALLOWED_CODE:
            case ResponseCode.INTERNAL_SERVER_ERROR_CODE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        if (errorMessage != null)
            return String.format("CommandResponse [code=%s, errormessage=%s]", code, errorMessage);
        else
            return String.format("CommandResponse [code=%s]", code);
    }
    // Syntactic sugar static constructors :

    public static CommandResponse success() {
        return new CommandResponse(ResponseCode.CHANGED, null);
    }

    public static CommandResponse badRequest(String errorMessage) {
        return new CommandResponse(ResponseCode.BAD_REQUEST, errorMessage);
    }

    public static CommandResponse notFound() {
        return new CommandResponse(ResponseCode.NOT_FOUND, null);
    }

    public static CommandResponse unauthorized() {
        return new CommandResponse(ResponseCode.UNAUTHORIZED, null);
    }

    public static CommandResponse methodNotAllowed() {
        return new CommandResponse(ResponseCode.METHOD_NOT_ALLOWED, null);
    }

    public static CommandResponse internalServerError(String errorMessage) {
        return new CommandResponse(ResponseCode.INTERNAL_SERVER_ERROR, errorMessage);
    }

}

