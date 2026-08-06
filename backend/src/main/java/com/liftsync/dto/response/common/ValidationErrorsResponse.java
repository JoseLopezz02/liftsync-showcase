package com.liftsync.dto.response.common;

import java.util.List;

public record ValidationErrorsResponse(List<String> errors) {
}