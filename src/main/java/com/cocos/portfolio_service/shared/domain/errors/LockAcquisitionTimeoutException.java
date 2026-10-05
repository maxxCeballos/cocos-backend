package com.cocos.portfolio_service.shared.domain.errors;

public class LockAcquisitionTimeoutException extends RuntimeException {
    public LockAcquisitionTimeoutException(String key) {
        super("Timed out waiting to acquire lock " + key);
    }
}
