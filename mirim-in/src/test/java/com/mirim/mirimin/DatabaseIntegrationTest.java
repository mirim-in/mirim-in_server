package com.mirim.mirimin;

import com.mirim.mirimin.answer.entity.Answer;
import com.mirim.mirimin.answer.entity.AnswerVote;
import com.mirim.mirimin.answer.entity.VoteType;
import com.mirim.mirimin.category.entity.Category;
import com.mirim.mirimin.comment.entity.Comment;
import com.mirim.mirimin.notification.entity.Notification;
import com.mirim.mirimin.notification.entity.NotificationType;
import com.mirim.mirimin.point.entity.PointHistory;
import com.mirim.mirimin.point.entity.PointReason;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.entity.QuestionCategory;
import com.mirim.mirimin.question.entity.QuestionImage;
import com.mirim.mirimin.question.entity.QuestionStatus;
import com.mirim.mirimin.question.entity.QuestionView;
import com.mirim.mirimin.report.entity.Report;
import com.mirim.mirimin.report.entity.ReportReason;
import com.mirim.mirimin.report.entity.ReportTargetType;
import com.mirim.mirimin.user.entity.AuthProvider;
import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 DB(.env의 DB_*)에 붙어서 엔티티 매핑과 저장/조회를 확인한다.
 * ddl-auto=validate 로 스키마가 엔티티와 다르면 컨텍스트 로딩부터 실패한다.
 * 모든 테스트는 끝나면 롤백되어 데이터가 남지 않는다.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class DatabaseIntegrationTest {

    @Autowired
    EntityManager em;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void 회원_질문_답변_댓글_신고_알림_포인트를_저장하고_조회한다() {
        User student = persist(User.builder()
                .email("s2501-dbtest@e-mirim.hs.kr")
                .passwordHash("hash")
                .provider(AuthProvider.LOCAL)
                .name("테스트학생")
                .memberType(MemberType.STUDENT)
                .department(Department.SOFTWARE)
                .admissionYear(2025)
                .build());
        User teacher = persist(User.builder()
                .email("teacher-dbtest@e-mirim.hs.kr")
                .provider(AuthProvider.GOOGLE)
                .providerId("google-123")
                .name("테스트교사")
                .memberType(MemberType.TEACHER)
                .build());

        Category category = persist(Category.userCreated("DB테스트키워드", student));

        Question question = persist(Question.builder()
                .author(student)
                .title("기숙사에서 라면 먹어도 되나요?")
                .content("궁금합니다")
                .anonymous(true)
                .draft(false)
                .now(now)
                .build());
        question.getCategories().add(new QuestionCategory(question, category));
        question.getImages().add(new QuestionImage(question, "https://res.cloudinary.com/test.png", 0));

        Answer answer = persist(new Answer(question, teacher, "됩니다"));
        question.increaseAnswerCount();
        answer.accept(now);
        question.resolve();

        persist(new AnswerVote(student, answer, VoteType.LIKE));
        answer.applyVote(VoteType.LIKE, 1);

        persist(Comment.onAnswer(answer, student, "감사합니다"));
        persist(new QuestionView(question, student, "u:" + student.getId(), now));

        persist(Report.builder()
                .reporter(teacher)
                .targetType(ReportTargetType.QUESTION)
                .targetId(question.getId())
                .targetUser(student)
                .reasons(Set.of(ReportReason.SLANDER, ReportReason.PROFANITY))
                .contentSnapshot(question.getContent())
                .build());

        persist(Notification.builder()
                .receiver(student)
                .type(NotificationType.ANSWER_CREATED)
                .question(question)
                .message("새 답변이 등록되었어요")
                .now(now)
                .build());

        persist(PointHistory.builder()
                .user(teacher)
                .amount(30)
                .reason(PointReason.ANSWER_ACCEPTED)
                .referenceId(answer.getId())
                .now(now)
                .build());
        teacher.addPoint(30);

        em.flush();
        em.clear();

        Question found = em.find(Question.class, question.getId());
        assertThat(found.getStatus()).isEqualTo(QuestionStatus.RESOLVED);
        assertThat(found.isAnonymous()).isTrue();
        assertThat(found.getAuthor().getName()).isEqualTo("테스트학생");
        assertThat(found.getCategories()).extracting(qc -> qc.getCategory().getName()).containsExactly("DB테스트키워드");
        assertThat(found.getImages()).hasSize(1);
        assertThat(found.getCreatedAt()).isNotNull();

        Answer foundAnswer = em.find(Answer.class, answer.getId());
        assertThat(foundAnswer.isAccepted()).isTrue();
        assertThat(foundAnswer.getLikeCount()).isEqualTo(1);

        Report foundReport = em.createQuery("select r from Report r where r.targetId = :id", Report.class)
                .setParameter("id", question.getId())
                .getSingleResult();
        assertThat(foundReport.getReasons()).containsExactlyInAnyOrder(ReportReason.SLANDER, ReportReason.PROFANITY);

        User foundStudent = em.find(User.class, student.getId());
        assertThat(foundStudent.getDepartment()).isEqualTo(Department.SOFTWARE);
        assertThat(foundStudent.getGrade(LocalDate.of(2026, 10, 25))).isEqualTo(2);
        assertThat(foundStudent.canBeMentor(LocalDate.of(2026, 10, 25))).isTrue();
        assertThat(em.find(User.class, teacher.getId()).getPoint()).isEqualTo(30);
    }

    @Test
    void 같은_답변에_두_번_투표하면_unique_제약에_걸린다() {
        User user = persist(User.builder()
                .email("vote-dbtest@e-mirim.hs.kr")
                .provider(AuthProvider.LOCAL)
                .name("투표자")
                .build());
        Question question = persist(Question.builder()
                .author(user).title("제목").content("내용").draft(false).now(now).build());
        Answer answer = persist(new Answer(question, user, "답변"));

        persist(new AnswerVote(user, answer, VoteType.LIKE));
        em.flush();

        // IDENTITY 전략이라 persist 시점에 바로 INSERT 된다
        assertThatThrownBy(() -> {
            em.persist(new AnswerVote(user, answer, VoteType.DISLIKE));
            em.flush();
        }).hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void 초기_데이터가_들어있다() {
        Long policies = em.createQuery("select count(p) from PointPolicy p", Long.class).getSingleResult();
        Long officials = em.createQuery("select count(c) from Category c where c.official = true", Long.class)
                .getSingleResult();
        assertThat(policies).isEqualTo(3);
        assertThat(officials).isGreaterThanOrEqualTo(7);
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
