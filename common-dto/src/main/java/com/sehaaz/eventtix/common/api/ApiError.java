package com.sehaaz.eventtix.common.api;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message) {
}
