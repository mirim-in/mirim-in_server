package com.mirim.mirimin.user.repository;

import com.mirim.mirimin.user.entity.UserTechStack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface UserTechStackRepository extends JpaRepository<UserTechStack, Long> {

    @Query("select t from UserTechStack t where t.user.id in :userIds order by t.id asc")
    List<UserTechStack> findByUserIds(Collection<Long> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserTechStack t where t.user.id = :userId")
    void deleteByUserId(Long userId);
}
