package com.mart.quickpass.gate.entity;

public enum GateFailureReason {
    AI_TIMEOUT,
    AI_INFERENCE_ERROR,
    CAMERA_UNAVAILABLE,
    IMAGE_UPLOAD_FAILED,
    INVALID_IMAGE_DATA,
    INTERNAL_ERROR
}
