package com.niramoy.pharmacy.repository;

import com.niramoy.pharmacy.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> { }
