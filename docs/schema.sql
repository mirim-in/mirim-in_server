-- =========================================================
--  미림iN 데이터베이스 스키마 (MySQL 8)
--  JPA 엔티티에서 Hibernate로 생성한 DDL입니다.
--  엔티티를 수정하면 이 파일도 다시 생성해야 합니다.
-- =========================================================

SET NAMES utf8mb4;

create table admin_logs (
    id bigint not null auto_increment,
    action varchar(30) not null,
    created_at datetime(6) not null,
    detail varchar(500),
    target_id bigint,
    target_type varchar(30) not null,
    admin_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table answer_votes (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    type varchar(10) not null,
    answer_id bigint not null,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table answers (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    accepted bit not null,
    accepted_at datetime(6),
    content TEXT not null,
    deleted_at datetime(6),
    dislike_count integer not null,
    like_count integer not null,
    author_id bigint not null,
    question_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table bookmarks (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    question_id bigint not null,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table categories (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6),
    name varchar(30) not null,
    official bit not null,
    sort_order integer not null,
    created_by bigint,
    primary key (id)
) engine=InnoDB;

create table comments (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    content varchar(1000) not null,
    deleted_at datetime(6),
    answer_id bigint,
    author_id bigint not null,
    question_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table email_verifications (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    attempt_count integer not null,
    code varchar(100) not null,
    email varchar(100) not null,
    expires_at datetime(6) not null,
    purpose varchar(20) not null,
    verified bit not null,
    primary key (id)
) engine=InnoDB;

create table faqs (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    answer TEXT not null,
    question varchar(200) not null,
    sort_order integer not null,
    category_id bigint,
    primary key (id)
) engine=InnoDB;

create table member_verification_requests (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    message varchar(500),
    reject_reason varchar(500),
    requested_type varchar(20) not null,
    reviewed_at datetime(6),
    status varchar(20) not null,
    reviewer_id bigint,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table notice_images (
    id bigint not null auto_increment,
    sort_order integer not null,
    url varchar(500) not null,
    notice_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table notices (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    content TEXT not null,
    deleted_at datetime(6),
    pinned bit not null,
    published_at datetime(6),
    status varchar(20) not null,
    title varchar(200) not null,
    author_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table notifications (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    message varchar(200) not null,
    read_at datetime(6),
    type varchar(30) not null,
    notice_id bigint,
    question_id bigint,
    receiver_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table point_histories (
    id bigint not null auto_increment,
    amount integer not null,
    created_at datetime(6) not null,
    memo varchar(200),
    reason varchar(30) not null,
    reference_id bigint,
    admin_id bigint,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table point_policies (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    action varchar(20) not null,
    amount integer not null,
    primary key (id)
) engine=InnoDB;

create table question_categories (
    id bigint not null auto_increment,
    category_id bigint not null,
    question_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table question_curiosities (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    question_id bigint not null,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table question_images (
    id bigint not null auto_increment,
    sort_order integer not null,
    url varchar(500) not null,
    question_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table question_views (
    id bigint not null auto_increment,
    viewed_at datetime(6) not null,
    viewer_key varchar(64) not null,
    question_id bigint not null,
    user_id bigint,
    primary key (id)
) engine=InnoDB;

create table questions (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    anonymous bit not null,
    answer_count integer not null,
    content TEXT not null,
    curiosity_count integer not null,
    deleted_at datetime(6),
    published_at datetime(6),
    status varchar(20) not null,
    title varchar(200) not null,
    view_count integer not null,
    author_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table report_reasons (
    report_id bigint not null,
    reason varchar(20) not null,
    primary key (report_id, reason)
) engine=InnoDB;

create table reports (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    content_snapshot TEXT not null,
    detail varchar(500),
    processed_at datetime(6),
    status varchar(20) not null,
    target_id bigint not null,
    target_type varchar(20) not null,
    processed_by bigint,
    reporter_id bigint not null,
    target_user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table user_categories (
    id bigint not null auto_increment,
    type varchar(20) not null,
    category_id bigint not null,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table user_tech_stacks (
    id bigint not null auto_increment,
    name varchar(50) not null,
    user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table users (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    admission_year integer,
    bio varchar(500),
    class_no integer,
    deleted_at datetime(6),
    department varchar(20),
    email varchar(100) not null,
    member_type varchar(20) not null,
    mentor_enabled bit not null,
    mentor_intro varchar(200),
    name varchar(30) not null,
    password_hash varchar(255),
    point integer not null,
    profile_image_url varchar(500),
    provider varchar(20) not null,
    provider_id varchar(100),
    role varchar(20) not null,
    status varchar(20) not null,
    suspended_until datetime(6),
    primary key (id)
) engine=InnoDB;

create index idx_admin_log_created 
   on admin_logs (created_at);

alter table answer_votes 
   add constraint UKl223hnu6pfmo8mqs2qevejwuc unique (user_id, answer_id);

create index idx_answer_question 
   on answers (question_id);

create index idx_answer_author 
   on answers (author_id);

alter table bookmarks 
   add constraint UKborot2acuvp2gytvn3p8dsbcw unique (user_id, question_id);

alter table categories 
   add constraint UKt8o6pivur7nn124jehx7cygw5 unique (name);

create index idx_comment_question 
   on comments (question_id);

create index idx_comment_answer 
   on comments (answer_id);

create index idx_email_verification_email_purpose 
   on email_verifications (email, purpose);

create index idx_mvr_status 
   on member_verification_requests (status);

create index idx_notice_status_pinned 
   on notices (status, pinned);

create index idx_notification_receiver 
   on notifications (receiver_id, read_at, created_at);

create index idx_point_history_user 
   on point_histories (user_id, created_at);

create index idx_point_history_reference 
   on point_histories (reference_id, reason);

alter table point_policies 
   add constraint UKegu1jrh1x6t4he5320ofhmtb1 unique (action);

create index idx_question_category_category 
   on question_categories (category_id);

alter table question_categories 
   add constraint UKo8wsmpc5o36cn9j6o9uhw002m unique (question_id, category_id);

alter table question_curiosities 
   add constraint UKdu642bbjycjl06tj2orh79pi7 unique (user_id, question_id);

create index idx_question_view_viewed_at 
   on question_views (viewed_at, question_id);

create index idx_question_view_dedup 
   on question_views (question_id, viewer_key, viewed_at);

create index idx_question_status_created 
   on questions (status, created_at);

create index idx_question_author 
   on questions (author_id);

create index idx_report_status_created 
   on reports (status, created_at);

create index idx_report_target_user 
   on reports (target_user_id);

create index idx_user_category_category_type 
   on user_categories (category_id, type);

alter table user_categories 
   add constraint UKqt31o7qdmu5esjequo42kkbed unique (user_id, category_id, type);

alter table user_tech_stacks 
   add constraint UKn4r7cyhj13wlhod4931egqrqo unique (user_id, name);

alter table users 
   add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

alter table admin_logs 
   add constraint FKoix4xxbvhimiroqel44k3h7uk 
   foreign key (admin_id) 
   references users (id);

alter table answer_votes 
   add constraint FKbxpmd373n8cndt4ruup9vesxt 
   foreign key (answer_id) 
   references answers (id);

alter table answer_votes 
   add constraint FK15yfsuxa91p72fxtixiv6v05t 
   foreign key (user_id) 
   references users (id);

alter table answers 
   add constraint FKaih8gs2e2ii62666wr3b91ulf 
   foreign key (author_id) 
   references users (id);

alter table answers 
   add constraint FK3erw1a3t0r78st8ty27x6v3g1 
   foreign key (question_id) 
   references questions (id);

alter table bookmarks 
   add constraint FKr9msdc6yttbmm1e276c5q9wx2 
   foreign key (question_id) 
   references questions (id);

alter table bookmarks 
   add constraint FKdbsho2e05w5r13fkjqfjmge5f 
   foreign key (user_id) 
   references users (id);

alter table categories 
   add constraint FK5yfru0au6kpyqs4tonky5vfne 
   foreign key (created_by) 
   references users (id);

alter table comments 
   add constraint FKoiwlwqmu9qm0tjnafxqr20rd8 
   foreign key (answer_id) 
   references answers (id);

alter table comments 
   add constraint FKn2na60ukhs76ibtpt9burkm27 
   foreign key (author_id) 
   references users (id);

alter table comments 
   add constraint FK5og2yrimec2mq7km4wgvvybw3 
   foreign key (question_id) 
   references questions (id);

alter table faqs 
   add constraint FKc5wxxu370vwystmvu6ey7tewu 
   foreign key (category_id) 
   references categories (id);

alter table member_verification_requests 
   add constraint FKg7pedo39iwxpn2oq62k0cbojs 
   foreign key (reviewer_id) 
   references users (id);

alter table member_verification_requests 
   add constraint FKkftkgitlnpaea4aeymf123pow 
   foreign key (user_id) 
   references users (id);

alter table notice_images 
   add constraint FKiqbbja0q8mlwc8nfd2gyhrux4 
   foreign key (notice_id) 
   references notices (id);

alter table notices 
   add constraint FKntrei71mnyo4hx6kbp6vg0cju 
   foreign key (author_id) 
   references users (id);

alter table notifications 
   add constraint FK8t4upabq73rsxsp55u8i8n0gs 
   foreign key (notice_id) 
   references notices (id);

alter table notifications 
   add constraint FK49b1aa693gok24psc3ucu1pk5 
   foreign key (question_id) 
   references questions (id);

alter table notifications 
   add constraint FK9kxl0whvhifo6gw4tjq36v53k 
   foreign key (receiver_id) 
   references users (id);

alter table point_histories 
   add constraint FK1o1bylcc1fi74dqhf4rg2w05l 
   foreign key (admin_id) 
   references users (id);

alter table point_histories 
   add constraint FKp1n0qput9f88pa5lwy2nsr19f 
   foreign key (user_id) 
   references users (id);

alter table question_categories 
   add constraint FKn0lndplxfkrr6599e27guxm33 
   foreign key (category_id) 
   references categories (id);

alter table question_categories 
   add constraint FKaj531sb6o5mjd6w0vfyecwkke 
   foreign key (question_id) 
   references questions (id);

alter table question_curiosities 
   add constraint FKnfes05c8rlfkqv60ur5y6dlx7 
   foreign key (question_id) 
   references questions (id);

alter table question_curiosities 
   add constraint FKcu0us61ec9ol90slcabuw7pe6 
   foreign key (user_id) 
   references users (id);

alter table question_images 
   add constraint FK7twynvv6nxw0dryu6k72su2ob 
   foreign key (question_id) 
   references questions (id);

alter table question_views 
   add constraint FKh793x17v5sg4ffxw9rjq5lgot 
   foreign key (question_id) 
   references questions (id);

alter table question_views 
   add constraint FK3ie21ruxxdw3xvyq0brlstbhm 
   foreign key (user_id) 
   references users (id);

alter table questions 
   add constraint FKii8pqtr2qjv47ht06bg8vtl9n 
   foreign key (author_id) 
   references users (id);

alter table report_reasons 
   add constraint FK4e50501swptpkh7g3pgocws1 
   foreign key (report_id) 
   references reports (id);

alter table reports 
   add constraint FKp8kw406ca0syx5wkphb6oelt5 
   foreign key (processed_by) 
   references users (id);

alter table reports 
   add constraint FKd3qiw2om5d2oh5xb7fbdcq225 
   foreign key (reporter_id) 
   references users (id);

alter table reports 
   add constraint FKpemp8lt5m0ujfywtgnuw4ypw9 
   foreign key (target_user_id) 
   references users (id);

alter table user_categories 
   add constraint FK6r91537otve5embvcuv40is3j 
   foreign key (category_id) 
   references categories (id);

alter table user_categories 
   add constraint FKdqpxght56isds8smi1frxg0xo 
   foreign key (user_id) 
   references users (id);

alter table user_tech_stacks 
   add constraint FKkftun92jnka7xqpe5s2uuta9f 
   foreign key (user_id) 
   references users (id);

-- =========================================================
--  초기 데이터
-- =========================================================

-- 포인트 정책 (질문 +5 / 답변 +10 / 채택 +30)
insert into point_policies (action, amount, created_at, updated_at) values
    ('QUESTION', 5, now(6), now(6)),
    ('ANSWER', 10, now(6), now(6)),
    ('ACCEPTED', 30, now(6), now(6));

-- 공식 카테고리 (질문 작성 화면 디자인 기준)
insert into categories (name, official, sort_order, created_at, updated_at) values
    ('기숙사', 1, 1, now(6), now(6)),
    ('전공', 1, 2, now(6), now(6)),
    ('학교생활', 1, 3, now(6), now(6)),
    ('진로', 1, 4, now(6), now(6)),
    ('입학', 1, 5, now(6), now(6)),
    ('면접', 1, 6, now(6), now(6)),
    ('기타', 1, 7, now(6), now(6));
