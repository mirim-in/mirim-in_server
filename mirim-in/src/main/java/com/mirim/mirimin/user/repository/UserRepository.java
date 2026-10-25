package com.mirim.mirimin.user.repository;

import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.UserCategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

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
