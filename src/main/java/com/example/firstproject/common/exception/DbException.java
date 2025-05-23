package com.example.firstproject.common.exception;

/**
 * RuntimeException을 상속받아서 DbException 런타임 예외 생성
 */
public class DbException extends RuntimeException {
    public DbException() {
    }

    public DbException(String message) {
        super(message);
    }

    public DbException(String message, Throwable cause) {
        super(message, cause);
    }

    public DbException(Throwable cause) {
        super(cause);
    }
}
