/* Users */
insert into users values (1, TRUE, 'ROLE_ADMIN',  'waliusmani@gmail.com', 'wali', 'usmani' , '$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC', null, null);
insert into users values (2, TRUE, 'ROLE_CONSUMER', 'wali@twotalltotems.com', 'wali', 'usmani', '$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC', null, null);

/* Workers */
insert into workers values (2, 'EVIL', 'CPU', null, 'ONLINE', null);

/* Candidate Value List */
insert into password_lists values ('test.txt', 'UTF-8');
insert into password_list_job_delimiter_set values ('test.txt', 2105248, 0, 0);
insert into password_list_job_delimiter_set values ('test.txt', 2818049, 1, 2105248);