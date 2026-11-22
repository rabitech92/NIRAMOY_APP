package com.niramoy.pharmacy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** অ্যাপ চালুর সময় ডিফল্ট প্ল্যান ও ফ্রি ট্রায়াল তৈরি করে (একবারই) */
@Component
@RequiredArgsConstructor
public class SubscriptionBootstrap implements ApplicationRunner {
    private final SubscriptionService subscriptions;

    @Override
    public void run(ApplicationArguments args) {
        subscriptions.ensureDefaults();
    }
}
