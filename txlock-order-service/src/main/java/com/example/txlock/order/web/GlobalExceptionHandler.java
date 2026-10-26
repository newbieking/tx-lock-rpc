package com.example.txlock.order.web;

import com.example.txlock.common.ApiError;
import com.example.txlock.order.exception.DuplicateOrderException;
import com.example.txlock.order.exception.LockBusyException;
import com.example.txlock.order.exception.OrderNotFoundException;
import com.example.txlock.order.exception.RequeueNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LockBusyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleLockBusy(LockBusyException e) {
        return new ApiError("LOCK_BUSY", e.getMessage());
    }

    @ExceptionHandler(DuplicateOrderException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDuplicate(DuplicateOrderException e) {
        return new ApiError("DUPLICATE_ORDER", e.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(OrderNotFoundException e) {
        return new ApiError("ORDER_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(RequeueNotAllowedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleRequeue(RequeueNotAllowedException e) {
        return new ApiError("REQUEUE_NOT_ALLOWED", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ApiError("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadable(HttpMessageNotReadableException e) {
        return new ApiError("BAD_REQUEST", "request body is missing or malformed");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception e) {
        log.error("unexpected error", e);
        return new ApiError("INTERNAL_ERROR", e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
