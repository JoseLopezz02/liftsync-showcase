package com.liftsync.dto.response.auth;

import com.liftsync.model.Enum.Role;

/**
 * Minimal identity tuple resolved per request from the token subject.
 * Only the fields the interceptors actually need, so authentication does not
 * have to hydrate the whole {@code User} entity.
 */
public record AuthUserProjection(String publicId, Role role) {
}
