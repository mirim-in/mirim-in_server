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
}
