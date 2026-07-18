package com.retail.exception;

public class BranchAlreadyHasManagerException extends RuntimeException {
    public BranchAlreadyHasManagerException(String message) {
        super(message);
    }
}