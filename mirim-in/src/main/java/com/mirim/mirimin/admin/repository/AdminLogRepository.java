package com.mirim.mirimin.admin.repository;

import com.mirim.mirimin.admin.entity.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
}
