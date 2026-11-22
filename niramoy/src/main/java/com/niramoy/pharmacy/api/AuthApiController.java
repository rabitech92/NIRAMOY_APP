package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.api.ApiDtos.*;
import com.niramoy.pharmacy.entity.AppUser;
import com.niramoy.pharmacy.repository.AppUserRepository;
import com.niramoy.pharmacy.repository.BranchRepository;
import com.niramoy.pharmacy.security.JwtService;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.SettingService;
import com.niramoy.pharmacy.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthApiController {
    private static final int MAX_FAILS = 5;
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private final AppUserRepository users;
    private final BranchRepository branches;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final SubscriptionService subscriptions;
    private final SettingService settings;

    /** ব্যবহারকারীভিত্তিক ব্যর্থ লগইন গণনা: [সংখ্যা, প্রথম ব্যর্থতার সময়] */
    private final Map<String, long[]> failures = new ConcurrentHashMap<>();

    public record LoginRequest(String username, String password) { }

    @PostMapping("/auth/login")
    public LoginDto login(@RequestBody LoginRequest req) {
        String key = req.username() == null ? "" : req.username().trim().toLowerCase();
        long[] f = failures.get(key);
        if (f != null && f[0] >= MAX_FAILS && System.currentTimeMillis() - f[1] < WINDOW_MS)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "অনেকবার ভুল চেষ্টা হয়েছে। ৫ মিনিট পর আবার চেষ্টা করুন।");

        Optional<AppUser> found = key.isEmpty() || req.password() == null
                ? Optional.empty() : users.findByUsernameIgnoreCase(key);
        if (found.isEmpty() || !found.get().isActive() || !encoder.matches(req.password(), found.get().getPassword())) {
            failures.compute(key, (k, v) -> {
                long now = System.currentTimeMillis();
                if (v == null || now - v[1] >= WINDOW_MS) return new long[]{1, now};
                v[0]++;
                return v;
            });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ইউজারনেম বা পাসওয়ার্ড সঠিক নয়, অথবা অ্যাকাউন্টটি নিষ্ক্রিয়।");
        }
        failures.remove(key);
        UserPrincipal p = new UserPrincipal(found.get());
        return new LoginDto(jwt.issue(p), jwt.getTtlSeconds(), ApiMapper.user(p));
    }

    @GetMapping("/me")
    public MeDto me() {
        UserPrincipal p = UserPrincipal.current();
        return new MeDto(ApiMapper.user(p), ApiMapper.subscription(subscriptions.status()),
                branches.findByActiveTrueOrderByNameAsc().stream().map(ApiMapper::branch).toList(), settings.vat());
    }

    @GetMapping("/subscription")
    public SubscriptionDto subscription() {
        return ApiMapper.subscription(subscriptions.status());
    }
}
