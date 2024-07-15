package org.halalz.kahftube.network

enum class ErrorCodeEnum(val value: Int) {
    OK(0),
    Canceled(1),
    Unknown(2),
    InvalidArgument(3),
    DeadlineExceeded(4),
    NotFound(5),
    AlreadyExists(6),
    PermissionDenied(7),
    ResourceExhausted(8),
    FailedPrecondition(9),
    Aborted(10),
    OutOfRange(11),
    Unimplemented(12),
    Internal(13),
    Unavailable(14),
    DataLoss(15),
    Unauthenticated(16);

    companion object {
        fun valueOf(value: Int) = entries.find { it.value == value }
    }
}
