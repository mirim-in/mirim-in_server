package com.mirim.mirimin.point.repository;

import com.mirim.mirimin.point.entity.PointHistory;
import com.mirim.mirimin.point.entity.PointReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /** 회수할 금액 계산용. 질문·답변 id가 겹칠 수 있으므로 reason으로 구분한다. */
    @Query("""
            select coalesce(sum(h.amount), 0) from PointHistory h
            where h.user.id = :userId and h.referenceId = :referenceId and h.reason in :reasons
            """)
    int sumAmount(Long userId, Long referenceId, Collection<PointReason> reasons);
}
