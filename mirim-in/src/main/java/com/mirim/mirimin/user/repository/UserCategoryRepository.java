package com.mirim.mirimin.user.repository;

import com.mirim.mirimin.user.entity.UserCategory;
import com.mirim.mirimin.user.entity.UserCategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {

    /** 삭제된 카테고리는 빼고 노출 순서대로 */
    @Query("""
            select uc from UserCategory uc join fetch uc.category c
            where uc.user.id in :userIds and c.deletedAt is null
            order by c.sortOrder asc, c.id asc
            """)
    List<UserCategory> findByUserIds(Collection<Long> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserCategory uc where uc.user.id = :userId and uc.type = :type")
    void deleteByUserIdAndType(Long userId, UserCategoryType type);
}
