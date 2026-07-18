package com.retail.exception;

public class DuplicateBranchCodeException extends RuntimeException {
    public DuplicateBranchCodeException(String message) {
        super(message);
    }
}