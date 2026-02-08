package com.nhn.cloud.photoservice.domain.audit;

public enum AuditAction {
    // Auth
    SIGNUP,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,

    // Photo
    PHOTO_UPLOAD,
    PHOTO_DELETE,

    // Album
    ALBUM_CREATE,
    ALBUM_DELETE,
    ALBUM_SHARE_ENABLE,
    ALBUM_SHARE_DISABLE
}
