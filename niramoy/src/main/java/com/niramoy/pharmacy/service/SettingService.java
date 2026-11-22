package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.AppSetting;
import com.niramoy.pharmacy.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SettingService {
    private static final String VAT = "vat.percent";
    private static final String PREFIX = "invoice.prefix";
    private final AppSettingRepository repo;

    @Transactional(readOnly = true)
    public BigDecimal vat() {
        return repo.findById(VAT).map(s -> new BigDecimal(s.getValue())).orElse(new BigDecimal("5"));
    }

    @Transactional(readOnly = true)
    public String invoicePrefix() {
        return repo.findById(PREFIX).map(AppSetting::getValue).orElse("INV");
    }

    @Transactional
    public void update(BigDecimal vat, String prefix) {
        repo.save(new AppSetting(VAT, vat.stripTrailingZeros().toPlainString()));
        repo.save(new AppSetting(PREFIX, prefix));
    }
}
