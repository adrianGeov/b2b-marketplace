package com.marketplace.b2b.common.exception;


public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resource + " con identificador '" + identifier + "' no encontrado");
    }
}