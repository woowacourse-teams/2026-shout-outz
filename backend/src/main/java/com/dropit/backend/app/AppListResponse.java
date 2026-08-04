package com.dropit.backend.app;

import java.util.List;

public record AppListResponse(List<AppResponse> items, long total) {
}
