package com.mirim.mirimin.question.repository;

import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.entity.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 공개된(임시 보관이 아닌) 질문 검색. 조건이 null이면 무시한다.
     * keyword는 제목, 내용, 카테고리 이름에서 찾는다.
     */
    @Query("""
            select q from Question q
            where q.deletedAt is null
              and q.publishedAt is not null
              and (:status is null or q.status = :status)
              and (:categoryId is null or exists (
                    select 1 from QuestionCategory qc where qc.question = q and qc.category.id = :categoryId))
              and (:keyword is null
                    or q.title like concat('%', :keyword, '%')
                    or q.content like concat('%', :keyword, '%')
                    or exists (select 1 from QuestionCategory qc2
                               where qc2.question = q and qc2.category.name like concat('%', :keyword, '%')))
            """)
    Page<Question> search(QuestionStatus status, Long categoryId, String keyword, Pageable pageable);

    List<Question> findByAuthorIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(Long authorId, QuestionStatus status);

    /**
     * 프로필 > 질문 목록. includeAnonymous=false면 익명 질문을 뺀다.
     * (다른 사람 프로필에서 익명 질문의 작성자가 드러나지 않도록)
     */
    @Query("""
            select q from Question q
            where q.author.id = :authorId and q.deletedAt is null and q.publishedAt is not null
              and (:includeAnonymous = true or q.anonymous = false)
            """)
    Page<Question> findByAuthor(Long authorId, boolean includeAnonymous, Pageable pageable);

    /** 기간 안의 조회 기록 수로 순위를 매긴다. 결과: [questionId, 조회 수] */
    @Query(value = """
            select v.question.id, count(v) from QuestionView v
            where v.viewedAt >= :from and v.question.deletedAt is null and v.question.publishedAt is not null
            group by v.question.id
            order by count(v) desc, v.question.id desc
            """,
            countQuery = """
            select count(distinct v.question.id) from QuestionView v
            where v.viewedAt >= :from and v.question.deletedAt is null and v.question.publishedAt is not null
            """)
    Page<Object[]> findPopularIds(LocalDateTime from, Pageable pageable);

    // 엔티티로 올리면 updatedAt이 바뀌고 동시 조회 시 값이 덮어써지므로 DB에서 직접 증가시킨다
    @Modifying
    @Query("update Question q set q.viewCount = q.viewCount + 1 where q.id = :id")
    void increaseViewCount(Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Question q set q.answerCount = q.answerCount + :delta where q.id = :id")
    void addAnswerCount(Long id, int delta);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Question q set q.curiosityCount = q.curiosityCount + :delta where q.id = :id")
    void addCuriosityCount(Long id, int delta);

    // increaseViewCount는 이미 불러온 엔티티에 반영되지 않으므로 응답용 값은 DB에서 다시 읽는다
    @Query("select q.viewCount from Question q where q.id = :id")
    int findViewCount(Long id);
}
