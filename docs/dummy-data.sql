-- =========================================================
--  미림iN 개발용 더미 데이터
--  - 더미 계정 이메일은 모두 @dummy.mirim-in.test (실제 학교 메일과 겹치지 않게)
--  - 더미 계정 비밀번호: mirim1234!
--  - 지우기: docs/dummy-data-clean.sql
--  - 이미 넣은 상태에서 다시 실행하면 이메일 중복으로 멈추고 아무것도 들어가지 않는다
--  - 학년은 2026학년도 기준 (2024 입학 = 3학년, 2025 = 2학년, 2026 = 1학년)
-- =========================================================

set names utf8mb4;
start transaction;

set @pw = '$2a$10$Luz6PYv4yvKtnefhogkKu.EysCbLZy36x8ICPH5j6pD4/xHq/GAo2';

-- ---------- 회원 ----------
insert into users (created_at, updated_at, email, password_hash, name, provider, role, status, member_type, department,
                   admission_year, class_no, bio, mentor_enabled, mentor_intro, point)
values (now(6) - interval 60 day, now(6), 'mirim.kim@dummy.mirim-in.test', @pw, '김미림', 'LOCAL', 'USER', 'ACTIVE',
        'STUDENT', 'SOFTWARE', 2024, 1, '백엔드 개발을 좋아하는 소프트웨어과 3학년입니다.', 1,
        '기숙사 생활 3년차, 전공 동아리 질문 환영해요!', 0);
set @u1 = last_insert_id();

insert into users (created_at, updated_at, email, password_hash, name, provider, role, status, member_type, department,
                   admission_year, class_no, bio, mentor_enabled, mentor_intro, point)
values (now(6) - interval 58 day, now(6), 'seoyeon.lee@dummy.mirim-in.test', @pw, '이서연', 'LOCAL', 'USER', 'ACTIVE',
        'STUDENT', 'DESIGN', 2024, 3, 'UI/UX 디자인을 공부하고 있어요.', 1,
        '디자인과 포트폴리오, 입시 면접 경험 공유해요.', 0);
set @u2 = last_insert_id();

insert into users (created_at, updated_at, email, password_hash, name, provider, role, status, member_type, department,
                   admission_year, class_no, bio, mentor_enabled, mentor_intro, point)
values (now(6) - interval 45 day, now(6), 'soyeon.park@dummy.mirim-in.test', @pw, '박소연', 'LOCAL', 'USER', 'ACTIVE',
        'STUDENT', 'SOFTWARE', 2025, 2, '프론트엔드 개발자를 꿈꾸는 2학년입니다.', 1,
        'React 공부법, 학교생활 무엇이든 물어보세요.', 0);
set @u3 = last_insert_id();

insert into users (created_at, updated_at, email, password_hash, name, provider, role, status, member_type, department,
                   admission_year, class_no, bio, mentor_enabled, mentor_intro, point)
values (now(6) - interval 40 day, now(6), 'haeun.choi@dummy.mirim-in.test', @pw, '최하은', 'LOCAL', 'USER', 'ACTIVE',
        'STUDENT', 'DESIGN', 2025, 4, '일러스트와 브랜딩에 관심이 많아요.', 0, null, 0);
set @u4 = last_insert_id();

insert into users (created_at, updated_at, email, password_hash, name, provider, role, status, member_type, department,
                   admission_year, class_no, bio, mentor_enabled, mentor_intro, point)
values (now(6) - interval 20 day, now(6), 'saebom.jung@dummy.mirim-in.test', @pw, '정새봄', 'LOCAL', 'USER', 'ACTIVE',
        'STUDENT', 'SOFTWARE', 2026, 2, '이제 막 입학한 1학년이에요.', 0, null, 0);
set @u5 = last_insert_id();

insert into users (created_at, updated_at, email, password_hash, name, provider, role, status, member_type, department,
                   admission_year, class_no, bio, mentor_enabled, mentor_intro, point)
values (now(6) - interval 15 day, now(6), 'yebi.han@dummy.mirim-in.test', @pw, '한예비', 'LOCAL', 'USER', 'ACTIVE',
        'GENERAL', null, null, null, '미림마이스터고 입학을 준비하는 중학생입니다.', 0, null, 0);
set @u6 = last_insert_id();

insert into users (created_at, updated_at, email, password_hash, name, provider, role, status, member_type, department,
                   admission_year, class_no, bio, mentor_enabled, mentor_intro, point)
values (now(6) - interval 50 day, now(6), 'jieun.yoon@dummy.mirim-in.test', @pw, '윤지은', 'LOCAL', 'USER', 'ACTIVE',
        'GRADUATE', 'SOFTWARE', null, null, '졸업 후 IT 기업에서 백엔드 개발자로 일하고 있어요.', 1,
        '취업 준비, 현장실습, 회사 생활 이야기 해드려요.', 0);
set @u7 = last_insert_id();

insert into users (created_at, updated_at, email, password_hash, name, provider, role, status, member_type, department,
                   admission_year, class_no, bio, mentor_enabled, mentor_intro, point)
values (now(6) - interval 55 day, now(6), 'teacher.kang@dummy.mirim-in.test', @pw, '강민수', 'LOCAL', 'USER', 'ACTIVE',
        'TEACHER', null, null, null, '소프트웨어과 담당 교사입니다.', 1,
        '입학 전형과 진로 상담 질문에 답해드립니다.', 0);
set @u8 = last_insert_id();

-- ---------- 카테고리 id (공식 카테고리 이름으로 찾기) ----------
select id into @c_dorm   from categories where name = '기숙사'   and deleted_at is null limit 1;
select id into @c_major  from categories where name = '전공'     and deleted_at is null limit 1;
select id into @c_school from categories where name = '학교생활' and deleted_at is null limit 1;
select id into @c_career from categories where name = '진로'     and deleted_at is null limit 1;
select id into @c_admit  from categories where name = '입학'     and deleted_at is null limit 1;
select id into @c_interv from categories where name = '면접'     and deleted_at is null limit 1;
select id into @c_etc    from categories where name = '기타'     and deleted_at is null limit 1;

-- 멘토 답변 가능 분야 / 관심 분야
insert into user_categories (user_id, category_id, type) values
    (@u1, @c_dorm, 'ANSWERABLE'), (@u1, @c_major, 'ANSWERABLE'), (@u1, @c_career, 'INTEREST'),
    (@u2, @c_interv, 'ANSWERABLE'), (@u2, @c_admit, 'ANSWERABLE'), (@u2, @c_major, 'INTEREST'),
    (@u3, @c_school, 'ANSWERABLE'), (@u3, @c_major, 'ANSWERABLE'),
    (@u4, @c_school, 'INTEREST'),
    (@u5, @c_dorm, 'INTEREST'), (@u5, @c_major, 'INTEREST'),
    (@u6, @c_admit, 'INTEREST'), (@u6, @c_interv, 'INTEREST'),
    (@u7, @c_career, 'ANSWERABLE'), (@u7, @c_major, 'ANSWERABLE'),
    (@u8, @c_admit, 'ANSWERABLE'), (@u8, @c_career, 'ANSWERABLE');

insert into user_tech_stacks (user_id, name) values
    (@u1, 'Java'), (@u1, 'Spring Boot'), (@u1, 'MySQL'),
    (@u2, 'Figma'), (@u2, 'Photoshop'),
    (@u3, 'React'), (@u3, 'TypeScript'),
    (@u4, 'Illustrator'),
    (@u7, 'Java'), (@u7, 'Kotlin'), (@u7, 'AWS');

-- ---------- 질문 ----------
-- q1: 기숙사 통금 (채택 완료)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 14 day, now(6) - interval 14 day, now(6) - interval 14 day, @u5,
        '기숙사 통금은 몇 시인가요?', '입학하고 처음 기숙사에 들어왔는데 평일 통금 시간이랑 주말 외박 신청 방법이 궁금해요.',
        0, 'RESOLVED', 87, 0, 0);
set @q1 = last_insert_id();

-- q2: 정보처리기능사 (채택 완료)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 12 day, now(6) - interval 12 day, now(6) - interval 12 day, @u4,
        '정보처리기능사는 언제 준비하는 게 좋을까요?', '디자인과인데 정보처리기능사도 따두면 좋다고 들었어요. 몇 학년 때 준비하는 게 좋은지, 공부는 어떻게 했는지 알려주세요.',
        0, 'RESOLVED', 64, 0, 0);
set @q2 = last_insert_id();

-- q3: 면접 질문 (답변 대기)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 10 day, now(6) - interval 10 day, now(6) - interval 10 day, @u6,
        '입학 면접에서 어떤 질문이 나오나요?', '내년에 미림마이스터고에 지원하려고 해요. 면접에서 주로 어떤 질문을 하는지, 준비는 어떻게 하면 좋을지 궁금합니다.',
        0, 'OPEN', 152, 0, 0);
set @q3 = last_insert_id();

-- q4: 동아리 (답변 대기, 익명)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 9 day, now(6) - interval 9 day, now(6) - interval 9 day, @u5,
        '전공 동아리는 어떻게 들어가나요?', '전공 동아리 모집이 언제 하는지, 면접이나 과제가 있는지 알고 싶어요. 코딩을 잘 못해도 들어갈 수 있을까요?',
        1, 'OPEN', 48, 0, 0);
set @q4 = last_insert_id();

-- q5: 포트폴리오 (채택 완료)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 8 day, now(6) - interval 8 day, now(6) - interval 8 day, @u4,
        '디자인과 포트폴리오는 몇 개 정도 준비하나요?', '현장실습 지원할 때 포트폴리오에 작업물을 몇 개 넣는지, 어떤 툴로 만드는지 궁금해요.',
        0, 'RESOLVED', 71, 0, 0);
set @q5 = last_insert_id();

-- q6: 취업 (답변 대기)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 7 day, now(6) - interval 7 day, now(6) - interval 7 day, @u3,
        '졸업 후 바로 취업하면 대학은 어떻게 하나요?', '취업하고 나서 재직자 전형으로 대학에 가는 선배들이 많다고 들었는데, 실제로 일하면서 병행할 수 있는지 궁금합니다.',
        0, 'OPEN', 39, 0, 0);
set @q6 = last_insert_id();

-- q7: 급식 (답변 대기)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 6 day, now(6) - interval 6 day, now(6) - interval 6 day, @u6,
        '급식은 맛있나요?', '학교 급식이랑 기숙사 석식은 어떤지 궁금해요. 매점도 있나요?',
        0, 'OPEN', 95, 0, 0);
set @q7 = last_insert_id();

-- q8: 노트북 (채택 완료, 익명)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 5 day, now(6) - interval 5 day, now(6) - interval 5 day, @u5,
        '소프트웨어과 노트북 사양 추천해주세요', '수업 때 노트북을 쓴다고 해서 새로 사려고 하는데, 램은 몇 기가 정도면 충분한가요? 맥북도 괜찮을까요?',
        1, 'RESOLVED', 120, 0, 0);
set @q8 = last_insert_id();

-- q9: 시험 기간 (답변 대기)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 4 day, now(6) - interval 4 day, now(6) - interval 4 day, @u4,
        '시험 기간에 전공이랑 일반 과목 공부는 어떻게 나누나요?', '중간고사가 다가오는데 전공 실기랑 국영수 공부 시간을 어떻게 배분하는지 선배님들 방법이 궁금해요.',
        0, 'OPEN', 33, 0, 0);
set @q9 = last_insert_id();

-- q10: 기숙사 준비물 (답변 대기)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 3 day, now(6) - interval 3 day, now(6) - interval 3 day, @u6,
        '기숙사 입사할 때 꼭 챙겨야 하는 준비물이 있나요?', '합격하면 기숙사에 들어갈 예정인데, 이불이나 생활용품은 직접 가져가야 하는지 궁금해요.',
        0, 'OPEN', 27, 0, 0);
set @q10 = last_insert_id();

-- q11: 현장실습 (답변 없음)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 2 day, now(6) - interval 2 day, now(6) - interval 2 day, @u3,
        '현장실습은 몇 학년 때 가나요?', '3학년 2학기에 현장실습을 간다고 들었는데, 회사는 학교에서 연결해주는 건지 직접 지원하는 건지 궁금합니다.',
        0, 'OPEN', 12, 0, 0);
set @q11 = last_insert_id();

-- q12: 방과후 (답변 없음)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 1 day, now(6) - interval 1 day, now(6) - interval 1 day, @u5,
        '방과후 수업은 필수인가요?', '방과후 수업 신청 안내를 받았는데 꼭 들어야 하는 건지, 어떤 수업이 인기가 많은지 알려주세요.',
        0, 'OPEN', 5, 0, 0);
set @q12 = last_insert_id();

-- q13: 임시 보관 (목록에 나오지 않음, 정새봄의 임시 보관함에서만 보임)
insert into questions (created_at, updated_at, published_at, author_id, title, content, anonymous, status,
                       view_count, answer_count, curiosity_count)
values (now(6) - interval 1 day, now(6) - interval 1 day, null, @u5,
        '학생회 선거는 언제 하나요?', '(작성 중)',
        0, 'DRAFT', 0, 0, 0);
set @q13 = last_insert_id();

insert into question_categories (question_id, category_id) values
    (@q1, @c_dorm), (@q1, @c_school),
    (@q2, @c_major), (@q2, @c_career),
    (@q3, @c_admit), (@q3, @c_interv),
    (@q4, @c_major), (@q4, @c_school),
    (@q5, @c_major), (@q5, @c_career),
    (@q6, @c_career),
    (@q7, @c_school), (@q7, @c_dorm),
    (@q8, @c_major),
    (@q9, @c_school),
    (@q10, @c_dorm), (@q10, @c_admit),
    (@q11, @c_career),
    (@q12, @c_school), (@q12, @c_etc),
    (@q13, @c_school);

-- ---------- 답변 ----------
insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 14 day + interval 2 hour, now(6) - interval 14 day + interval 2 hour, @q1, @u1,
        '평일 통금은 밤 10시이고, 주말 외박은 목요일까지 사감 선생님께 신청서를 내면 돼요. 학기 초에 안내문도 나눠줘요!',
        1, now(6) - interval 13 day, 0, 0);
set @a1 = last_insert_id();
insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 14 day + interval 5 hour, now(6) - interval 14 day + interval 5 hour, @q1, @u3,
        '시험 기간에는 자습실을 조금 더 늦게까지 열어줄 때도 있어요.', 0, null, 0, 0);
set @a2 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 11 day, now(6) - interval 11 day, @q2, @u7,
        '저는 2학년 1학기에 땄어요. 필기는 기출 문제 위주로 2주, 실기는 학교 수업이랑 겹치는 부분이 많아서 크게 어렵지 않았어요.',
        1, now(6) - interval 10 day, 0, 0);
set @a3 = last_insert_id();
insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 11 day + interval 3 hour, now(6) - interval 11 day + interval 3 hour, @q2, @u2,
        '디자인과는 컴퓨터그래픽스운용기능사를 먼저 따는 친구들이 많아요. 두 개 다 따면 현장실습 지원할 때 도움이 돼요.',
        0, null, 0, 0);
set @a4 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 9 day, now(6) - interval 9 day, @q3, @u2,
        '지원 동기, 관심 있는 분야, 중학교 때 했던 활동을 많이 물어봤어요. 거창한 것보다 솔직하게 경험을 말하는 게 좋았어요.',
        0, null, 0, 0);
set @a5 = last_insert_id();
insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 8 day, now(6) - interval 8 day, @q3, @u8,
        '면접에서는 전공 지식보다 성실함과 진로에 대한 고민을 봅니다. 자기소개서에 쓴 내용을 다시 읽어보고 오세요.',
        0, null, 0, 0);
set @a6 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 8 day, now(6) - interval 8 day, @q4, @u1,
        '3월 말쯤 동아리 박람회가 있고 그때 지원서를 받아요. 1학년은 실력보다 의지를 많이 보니까 걱정 안 해도 돼요!',
        0, null, 0, 0);
set @a7 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 7 day, now(6) - interval 7 day, @q5, @u2,
        '보통 5~8개 정도 넣어요. 개수보다 작업 과정을 잘 보여주는 게 중요하고, 저는 Figma로 만들어서 PDF로 뽑았어요.',
        1, now(6) - interval 6 day, 0, 0);
set @a8 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 6 day, now(6) - interval 6 day, @q6, @u7,
        '저는 회사 다니면서 야간 학부를 다니고 있어요. 힘들긴 하지만 회사에서 학비를 지원해주는 곳도 있어서 알아보면 좋아요.',
        0, null, 0, 0);
set @a9 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 5 day, now(6) - interval 5 day, @q7, @u3,
        '급식 꽤 맛있어요! 특식 나오는 날은 다들 줄 서요. 매점도 있어서 간식 사 먹을 수 있어요.',
        0, null, 0, 0);
set @a10 = last_insert_id();
insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 5 day + interval 4 hour, now(6) - interval 5 day + interval 4 hour, @q7, @u4,
        '기숙사 석식도 나와요. 가끔 메뉴가 겹치는 날도 있지만 괜찮은 편이에요.', 0, null, 0, 0);
set @a11 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 4 day, now(6) - interval 4 day, @q8, @u1,
        '램은 16GB 이상 추천해요. 맥북도 괜찮은데 가끔 윈도우 전용 프로그램을 쓰는 수업이 있어서 그건 학교 컴퓨터로 하면 돼요.',
        1, now(6) - interval 3 day, 0, 0);
set @a12 = last_insert_id();
insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 4 day + interval 6 hour, now(6) - interval 4 day + interval 6 hour, @q8, @u3,
        '저는 그램 쓰는데 가벼워서 들고 다니기 좋아요.', 0, null, 0, 0);
set @a13 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 3 day, now(6) - interval 3 day, @q9, @u3,
        '전공은 평소에 실습을 따라가 두고, 시험 2주 전부터는 일반 과목 비중을 늘려요.', 0, null, 0, 0);
set @a14 = last_insert_id();

insert into answers (created_at, updated_at, question_id, author_id, content, accepted, accepted_at, like_count, dislike_count)
values (now(6) - interval 2 day, now(6) - interval 2 day, @q10, @u1,
        '이불, 베개, 세면도구, 실내화는 꼭 챙기세요. 멀티탭은 규정이 있어서 안내문을 확인하는 게 좋아요.', 0, null, 0, 0);
set @a15 = last_insert_id();

-- ---------- 댓글 (answer_id가 null이면 질문에 단 댓글) ----------
insert into comments (created_at, updated_at, question_id, answer_id, author_id, content) values
    (now(6) - interval 13 day, now(6) - interval 13 day, @q1, @a1, @u5, '감사합니다! 바로 신청해볼게요.'),
    (now(6) - interval 13 day, now(6) - interval 13 day, @q1, null, @u6, '저도 궁금했던 내용이에요.'),
    (now(6) - interval 10 day, now(6) - interval 10 day, @q2, @a3, @u4, '기출 문제는 어디서 구하셨어요?'),
    (now(6) - interval 10 day, now(6) - interval 10 day, @q2, @a3, @u7, '큐넷 기출이랑 시중 문제집 하나면 충분했어요.'),
    (now(6) - interval 8 day, now(6) - interval 8 day, @q3, @a6, @u6, '선생님 답변 감사합니다!'),
    (now(6) - interval 7 day, now(6) - interval 7 day, @q4, @a7, @u5, '용기 내서 지원해볼게요 :)'),
    (now(6) - interval 4 day, now(6) - interval 4 day, @q7, null, @u1, '목요일 특식 진짜 맛있어요.'),
    (now(6) - interval 3 day, now(6) - interval 3 day, @q8, @a12, @u5, '16GB로 결정했어요. 감사합니다!');

-- ---------- 추천/비추천 ----------
insert into answer_votes (created_at, updated_at, answer_id, user_id, type) values
    (now(6), now(6), @a1, @u3, 'LIKE'), (now(6), now(6), @a1, @u5, 'LIKE'), (now(6), now(6), @a1, @u6, 'LIKE'),
    (now(6), now(6), @a3, @u4, 'LIKE'), (now(6), now(6), @a3, @u1, 'LIKE'),
    (now(6), now(6), @a5, @u6, 'LIKE'), (now(6), now(6), @a6, @u6, 'LIKE'), (now(6), now(6), @a6, @u2, 'LIKE'),
    (now(6), now(6), @a8, @u4, 'LIKE'),
    (now(6), now(6), @a10, @u6, 'LIKE'), (now(6), now(6), @a11, @u6, 'DISLIKE'),
    (now(6), now(6), @a12, @u5, 'LIKE'), (now(6), now(6), @a12, @u3, 'LIKE'), (now(6), now(6), @a13, @u1, 'DISLIKE');

-- ---------- 나도 궁금해요 ----------
insert into question_curiosities (created_at, updated_at, question_id, user_id) values
    (now(6), now(6), @q1, @u6), (now(6), now(6), @q3, @u5), (now(6), now(6), @q3, @u4),
    (now(6), now(6), @q4, @u6), (now(6), now(6), @q6, @u4), (now(6), now(6), @q7, @u5),
    (now(6), now(6), @q10, @u5), (now(6), now(6), @q11, @u4), (now(6), now(6), @q11, @u5);

-- ---------- 북마크 ----------
insert into bookmarks (created_at, updated_at, question_id, user_id) values
    (now(6), now(6), @q1, @u6), (now(6), now(6), @q3, @u6), (now(6), now(6), @q8, @u4), (now(6), now(6), @q2, @u5);

-- ---------- 공지 / FAQ ----------
insert into notices (created_at, updated_at, published_at, author_id, title, content, status, pinned) values
    (now(6) - interval 30 day, now(6) - interval 30 day, now(6) - interval 30 day, @u8,
     '[더미] 미림iN 이용 안내', '미림iN은 미림마이스터고 학생, 졸업생, 선생님과 예비 신입생이 함께 묻고 답하는 공간입니다. 서로를 존중하는 질문과 답변을 부탁드립니다.',
     'PUBLISHED', 1),
    (now(6) - interval 5 day, now(6) - interval 5 day, now(6) - interval 5 day, @u8,
     '[더미] 2027학년도 입학 설명회 일정', '입학 설명회는 학교 강당에서 진행됩니다. 자세한 일정은 학교 홈페이지를 확인해주세요.',
     'PUBLISHED', 0);

insert into faqs (created_at, updated_at, question, answer, sort_order, category_id) values
    (now(6), now(6), '[더미] 미림인 인증은 어떻게 하나요?', '학교 이메일(@e-mirim.hs.kr)로 가입하면 자동으로 인증됩니다. 졸업생은 마이페이지에서 승인 신청을 해주세요.', 1, @c_etc),
    (now(6), now(6), '[더미] 포인트는 어떻게 얻나요?', '질문 작성 5점, 답변 작성 10점, 답변이 채택되면 30점을 받습니다.', 2, @c_etc),
    (now(6), now(6), '[더미] 기숙사 신청은 언제 하나요?', '합격 후 안내되는 기간에 신청서를 제출하면 됩니다.', 3, @c_dorm);

-- ---------- 집계 값 맞추기 ----------
update questions q
set q.answer_count    = (select count(*) from answers a where a.question_id = q.id and a.deleted_at is null),
    q.curiosity_count = (select count(*) from question_curiosities c where c.question_id = q.id)
where q.id in (@q1, @q2, @q3, @q4, @q5, @q6, @q7, @q8, @q9, @q10, @q11, @q12, @q13);

update answers a
set a.like_count    = (select count(*) from answer_votes v where v.answer_id = a.id and v.type = 'LIKE'),
    a.dislike_count = (select count(*) from answer_votes v where v.answer_id = a.id and v.type = 'DISLIKE')
where a.question_id in (@q1, @q2, @q3, @q4, @q5, @q6, @q7, @q8, @q9, @q10, @q11, @q12);

-- ---------- 포인트 내역 (질문 +5, 답변 +10, 채택 +30) ----------
insert into point_histories (created_at, user_id, amount, reason, reference_id)
select q.published_at, q.author_id, 5, 'QUESTION_CREATED', q.id
from questions q
where q.id in (@q1, @q2, @q3, @q4, @q5, @q6, @q7, @q8, @q9, @q10, @q11, @q12);

insert into point_histories (created_at, user_id, amount, reason, reference_id)
select a.created_at, a.author_id, 10, 'ANSWER_CREATED', a.id
from answers a
where a.question_id in (@q1, @q2, @q3, @q4, @q5, @q6, @q7, @q8, @q9, @q10, @q11, @q12);

insert into point_histories (created_at, user_id, amount, reason, reference_id)
select a.accepted_at, a.author_id, 30, 'ANSWER_ACCEPTED', a.id
from answers a
where a.accepted = 1
  and a.question_id in (@q1, @q2, @q3, @q4, @q5, @q6, @q7, @q8, @q9, @q10, @q11, @q12);

update users u
set u.point = (select coalesce(sum(p.amount), 0) from point_histories p where p.user_id = u.id)
where u.id in (@u1, @u2, @u3, @u4, @u5, @u6, @u7, @u8);

commit;

-- 결과 확인
select id, name, member_type, point from users where email like '%@dummy.mirim-in.test' order by id;
select id, status, answer_count, curiosity_count, title from questions where id between @q1 and @q13 order by id;
