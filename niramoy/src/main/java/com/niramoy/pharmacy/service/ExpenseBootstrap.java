package com.niramoy.pharmacy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** অ্যাপ চালুর সময় ডিফল্ট খরচের ক্যাটাগরি তৈরি করে (শুধু ফাঁকা থাকলে) */
@Component
@RequiredArgsConstructor
public class ExpenseBootstrap implements ApplicationRunner {
    private final ExpenseService expenses;

    @Override
    public void run(ApplicationArguments args) {
        expenses.ensureDefaults();
    }
}
