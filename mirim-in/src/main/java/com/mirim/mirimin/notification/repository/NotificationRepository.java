package com.mirim.mirimin.notification.repository;

import com.mirim.mirimin.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByIdDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndReadAtIsNull(Long receiverId);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.receiver.id = :receiverId and n.readAt is null")
    int markAllRead(Long receiverId, LocalDateTime now);

    /** 공지 알림: 탈퇴하지 않은 모든 회원(작성자 제외)에게 한 번의 쿼리로 넣는다. */
    @Modifying(flushAutomatically = true)
    @Query("""
            insert into Notification (receiver, type, notice, message, createdAt)
            select u, com.mirim.mirimin.notification.entity.NotificationType.NOTICE, n, :message, :now
            from User u, Notice n
            where n.id = :noticeId and u.deletedAt is null and u.id <> :authorId
            """)
    int insertNoticeForAllUsers(Long noticeId, Long authorId, String message, LocalDateTime now);
}
