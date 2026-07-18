package com.retail.exception;

public class BranchHasActiveDataException extends RuntimeException {
    public BranchHasActiveDataException(String message) {
        super(message);
    }
}