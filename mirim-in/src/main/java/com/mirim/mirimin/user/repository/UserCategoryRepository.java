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

    /** 키워드 매칭 알림 대상: 이 카테고리 중 하나라도 해당 유형으로 설정한 사용자 */
    @Query("""
            select distinct uc.user.id from UserCategory uc
            where uc.category.id in :categoryIds and uc.type = :type and uc.user.deletedAt is null
            """)
    List<Long> findUserIds(Collection<Long> categoryIds, UserCategoryType type);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserCategory uc where uc.user.id = :userId and uc.type = :type")
    void deleteByUserIdAndType(Long userId, UserCategoryType type);
}
