package com.marketplace.b2b.common.exception;

import lombok.Getter;

/**
 * Excepción base de la aplicación. Los servicios lanzan esta excepción con un {@link ErrorCode}
 * y el GlobalExceptionHandler la traduce a la respuesta HTTP correspondiente.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }
}