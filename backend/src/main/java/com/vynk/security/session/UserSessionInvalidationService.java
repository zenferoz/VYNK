package com.vynk.security.session;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class UserSessionInvalidationService {

    private final ConcurrentMap<String, Instant> passwordResetCutoffs = new ConcurrentHashMap<>();

    public void markPasswordReset(String email) {
        if (email != null && !email.isBlank()) {
            passwordResetCutoffs.put(email.trim().toLowerCase(Locale.ROOT), Instant.now());
        }
    }

    public boolean isSessionInvalid(String email, Instant sessionAuthTime) {
        if (email == null) {
            return false;
        }
        Instant cutoff = passwordResetCutoffs.get(email.trim().toLowerCase(Locale.ROOT));
        if (cutoff == null) {
            return false;
        }
        if (sessionAuthTime == null) {
            return true;
        }
        return sessionAuthTime.isBefore(cutoff);
    }

    public void clear(String email) {
        if (email != null) {
            passwordResetCutoffs.remove(email.trim().toLowerCase(Locale.ROOT));
        }
    }
}
