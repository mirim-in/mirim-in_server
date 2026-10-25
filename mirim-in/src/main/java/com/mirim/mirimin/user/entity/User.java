package com.mirim.mirimin.user.entity;

import com.mirim.mirimin.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    private static final int MENTOR_MIN_GRADE = 2;
    private static final int SCHOOL_YEAR_START_MONTH = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Google 가입자는 null
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(length = 100)
    private String providerId;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MemberType memberType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private Department department;

    // 재학생만. 이메일 s2501 -> 2025
    private Integer admissionYear;

    private Integer classNo;

    @Column(length = 500)
    private String bio;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean mentorEnabled;

    @Column(length = 200)
    private String mentorIntro;

    @Column(nullable = false)
    private int point;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    // SUSPENDED 상태에서 null이면 영구 정지
    private LocalDateTime suspendedUntil;

    private LocalDateTime deletedAt;

    @Builder
    private User(String email, String passwordHash, AuthProvider provider, String providerId, String name,
                 Role role, MemberType memberType, Department department, Integer admissionYear) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.provider = provider;
        this.providerId = providerId;
        this.name = name;
        this.role = role != null ? role : Role.USER;
        this.memberType = memberType != null ? memberType : MemberType.GENERAL;
        this.department = department;
        this.admissionYear = admissionYear;
        this.status = UserStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isVerifiedMember() {
        return memberType != MemberType.GENERAL;
    }

    /** 재학생만 입학년도로 계산한다. 학년은 3월에 올라간다. */
    public Integer getGrade(LocalDate today) {
        if (memberType != MemberType.STUDENT || admissionYear == null) {
            return null;
        }
        int schoolYear = today.getMonthValue() >= SCHOOL_YEAR_START_MONTH ? today.getYear() : today.getYear() - 1;
        return schoolYear - admissionYear + 1;
    }

    public boolean canBeMentor(LocalDate today) {
        return switch (memberType) {
            case STUDENT -> {
                Integer grade = getGrade(today);
                yield grade != null && grade >= MENTOR_MIN_GRADE;
            }
            case GRADUATE, TEACHER -> true;
            case GENERAL -> false;
        };
    }

    /** 정지 중에도 로그인은 되고 글쓰기만 막는다. */
    public boolean isWriteRestricted(LocalDateTime now) {
        return status == UserStatus.SUSPENDED && (suspendedUntil == null || suspendedUntil.isAfter(now));
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void updateProfile(String name, Integer classNo, String bio) {
        this.name = name;
        this.classNo = classNo;
        this.bio = bio;
    }

    /** 재학생의 학과는 이메일에서 정해지므로 바꾸지 않는다. */
    public void changeDepartment(Department department) {
        if (memberType != MemberType.STUDENT) {
            this.department = department;
        }
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    public void verifyAs(MemberType memberType) {
        this.memberType = memberType;
    }

    public void changeMentorEnabled(boolean mentorEnabled) {
        this.mentorEnabled = mentorEnabled;
    }

    public void updateMentorIntro(String mentorIntro) {
        this.mentorIntro = mentorIntro;
    }

    /** until이 null이면 영구 정지 */
    public void suspend(LocalDateTime until) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedUntil = until;
    }

    public void unsuspend() {
        this.status = UserStatus.ACTIVE;
        this.suspendedUntil = null;
    }

    public void addPoint(int amount) {
        this.point += amount;
    }

    /**
     * 개인정보를 지우고 탈퇴 처리한다. 작성한 글은 "탈퇴한 사용자"로 남는다.
     * 이메일을 비워 두므로 같은 이메일로 다시 가입할 수 있다.
     */
    public void withdraw(LocalDateTime now) {
        this.deletedAt = now;
        this.email = "withdrawn-" + id + "@deleted.mirim-in";
        this.name = "탈퇴한 사용자";
        this.passwordHash = null;
        this.providerId = null;
        this.profileImageUrl = null;
        this.bio = null;
        this.classNo = null;
        this.mentorEnabled = false;
        this.mentorIntro = null;
    }
}
