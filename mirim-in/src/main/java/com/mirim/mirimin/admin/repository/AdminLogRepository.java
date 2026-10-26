package com.mirim.mirimin.admin.repository;

import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.admin.entity.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    /** 조건이 null이면 무시한다. 최신 순 */
    @Query(value = """
            select l from AdminLog l join fetch l.admin
            where (:action is null or l.action = :action)
              and (:targetType is null or l.targetType = :targetType)
            order by l.id desc
            """,
            countQuery = """
            select count(l) from AdminLog l
            where (:action is null or l.action = :action)
              and (:targetType is null or l.targetType = :targetType)
            """)
    Page<AdminLog> search(AdminAction action, String targetType, Pageable pageable);
}
