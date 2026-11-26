-- =========================================================
--  미림iN 더미 데이터 삭제 (docs/dummy-data.sql로 넣은 것)
--  기준: 이메일이 @dummy.mirim-in.test 인 회원, 그 회원이 쓴 질문/답변/댓글, 그 회원이 작성한 공지,
--        '[더미]'로 시작하는 FAQ
--  주의: 실제 회원이 더미 질문에 단 답변·댓글도 질문과 함께 지워진다
-- =========================================================

set names utf8mb4;
start transaction;

create temporary table tmp_dummy_users as
select id from users where email like '%@dummy.mirim-in.test';

create temporary table tmp_dummy_questions as
select id from questions where author_id in (select id from tmp_dummy_users);

create temporary table tmp_dummy_answers as
select id from answers
where question_id in (select id from tmp_dummy_questions)
   or author_id in (select id from tmp_dummy_users);

create temporary table tmp_dummy_notices as
select id from notices where author_id in (select id from tmp_dummy_users);

delete from answer_votes
where answer_id in (select id from tmp_dummy_answers) or user_id in (select id from tmp_dummy_users);

delete from comments
where question_id in (select id from tmp_dummy_questions)
   or answer_id in (select id from tmp_dummy_answers)
   or author_id in (select id from tmp_dummy_users);

delete from question_curiosities
where question_id in (select id from tmp_dummy_questions) or user_id in (select id from tmp_dummy_users);

delete from bookmarks
where question_id in (select id from tmp_dummy_questions) or user_id in (select id from tmp_dummy_users);

delete from question_views
where question_id in (select id from tmp_dummy_questions) or user_id in (select id from tmp_dummy_users);

delete from notifications
where receiver_id in (select id from tmp_dummy_users)
   or question_id in (select id from tmp_dummy_questions)
   or notice_id in (select id from tmp_dummy_notices);

delete from point_histories
where user_id in (select id from tmp_dummy_users) or admin_id in (select id from tmp_dummy_users);

delete from report_reasons
where report_id in (select id from reports
                    where reporter_id in (select id from tmp_dummy_users)
                       or target_user_id in (select id from tmp_dummy_users)
                       or processed_by in (select id from tmp_dummy_users));
delete from reports
where reporter_id in (select id from tmp_dummy_users)
   or target_user_id in (select id from tmp_dummy_users)
   or processed_by in (select id from tmp_dummy_users);

delete from answers where id in (select id from tmp_dummy_answers);
delete from question_categories where question_id in (select id from tmp_dummy_questions);
delete from question_images where question_id in (select id from tmp_dummy_questions);
delete from questions where id in (select id from tmp_dummy_questions);

delete from notice_images where notice_id in (select id from tmp_dummy_notices);
delete from notices where id in (select id from tmp_dummy_notices);
delete from faqs where question like '[더미]%';

update categories set created_by = null where created_by in (select id from tmp_dummy_users);
delete from user_categories where user_id in (select id from tmp_dummy_users);
delete from user_tech_stacks where user_id in (select id from tmp_dummy_users);
delete from refresh_tokens where user_id in (select id from tmp_dummy_users);
delete from member_verification_requests
where user_id in (select id from tmp_dummy_users) or reviewer_id in (select id from tmp_dummy_users);
delete from admin_logs where admin_id in (select id from tmp_dummy_users);
delete from users where id in (select id from tmp_dummy_users);

commit;

drop temporary table tmp_dummy_users, tmp_dummy_questions, tmp_dummy_answers, tmp_dummy_notices;

select count(*) as remaining_dummy_users from users where email like '%@dummy.mirim-in.test';
