package com.mirim.mirimin.notice.repository;

import com.mirim.mirimin.notice.entity.Notice;
import com.mirim.mirimin.notice.entity.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Optional<Notice> findByIdAndDeletedAtIsNull(Long id);

    /** 사용자용 공지 목록: 등록된 공지, 고정 공지 먼저 */
    @Query("""
            select n from Notice n
            where n.deletedAt is null and n.status = :published
            order by n.pinned desc, n.publishedAt desc, n.id desc
            """)
    Page<Notice> findPublished(NoticeStatus published, Pageable pageable);

    List<Notice> findByDeletedAtIsNullAndStatusAndPinnedTrueOrderByPublishedAtDesc(NoticeStatus status);

    /** 관리자 공지 목록. 조건이 null이면 무시한다. */
    @Query("""
            select n from Notice n
            where n.deletedAt is null
              and (:status is null or n.status = :status)
              and (:keyword is null
                    or n.title like concat('%', :keyword, '%')
                    or n.content like concat('%', :keyword, '%'))
            """)
    Page<Notice> searchForAdmin(NoticeStatus status, String keyword, Pageable pageable);
}
