package com.mirim.mirimin.user.repository;

import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.UserCategoryType;
import com.mirim.mirimin.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // 동시에 여러 포인트가 쌓일 수 있고, 엔티티로 올리면 updatedAt이 바뀌므로 DB에서 직접 증감한다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.point = u.point + :delta where u.id = :id")
    void addPoint(Long id, int delta);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndMemberTypeNot(MemberType memberType);

    /**
     * 관리자 회원 목록. suspended: true면 지금 정지 중인 회원만, false면 정지 중이 아닌 회원만, null이면 전체.
     * (정지 기간이 지난 회원은 정지 중이 아닌 것으로 본다)
     */
    @Query("""
            select u from User u
            where u.deletedAt is null
              and (:memberType is null or u.memberType = :memberType)
              and (:keyword is null
                    or u.name like concat('%', :keyword, '%')
                    or u.email like concat('%', :keyword, '%'))
              and (:suspended is null
                    or (:suspended = true and u.status = :suspendedStatus
                        and (u.suspendedUntil is null or u.suspendedUntil > :now))
                    or (:suspended = false and not (u.status = :suspendedStatus
                        and (u.suspendedUntil is null or u.suspendedUntil > :now))))
            """)
    Page<User> searchForAdmin(String keyword, MemberType memberType, Boolean suspended,
                              UserStatus suspendedStatus, LocalDateTime now, Pageable pageable);

    /** 멘토 목록. 조건이 null이면 무시한다. keyword는 이름, 멘토 소개에서 찾는다. */
    @Query("""
            select u from User u
            where u.mentorEnabled = true and u.deletedAt is null
              and (:department is null or u.department = :department)
              and (:keyword is null
                    or u.name like concat('%', :keyword, '%')
                    or u.mentorIntro like concat('%', :keyword, '%'))
              and (:categoryId is null or exists (
                    select 1 from UserCategory uc
                    where uc.user = u and uc.type = :answerable and uc.category.id = :categoryId))
            """)
    Page<User> searchMentors(Department department, String keyword, Long categoryId,
                             UserCategoryType answerable, Pageable pageable);
}
