-- =============================================================================
-- VYNK Baseline Schema Migration (Phase 3)
-- Version: 1.0.0
-- Generated from verified live MySQL 8.0 schema (vynk_db)
-- Character Set: utf8mb4 | Collation: utf8mb4_unicode_ci | Engine: InnoDB
-- Contains: 18 Tables (15 JPA Entities + 3 ElementCollections)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table 1: colleges
-- -----------------------------------------------------------------------------
CREATE TABLE `colleges` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `active` BIT(1) NOT NULL,
  `email_domain` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` VARCHAR(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5vcchkm0feha27k532o0va5yk` (`email_domain`),
  UNIQUE KEY `UK4q2vsgb93d2wyblodej8r5hmc` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 2: users
-- -----------------------------------------------------------------------------
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email_verified` BIT(1) NOT NULL,
  `password_hash` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` ENUM('ADMIN','STUDENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` ENUM('ACTIVE','DELETED','PENDING','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `college_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `FKq8c77pl7fllv195wbwqn13375` (`college_id`),
  CONSTRAINT `FKq8c77pl7fllv195wbwqn13375` FOREIGN KEY (`college_id`) REFERENCES `colleges` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 3: email_verification_tokens
-- -----------------------------------------------------------------------------
CREATE TABLE `email_verification_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `expires_at` DATETIME(6) NOT NULL,
  `token_hash` VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `used` BIT(1) NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_verification_token_hash` (`token_hash`),
  UNIQUE KEY `UKs3mje1c85ftmp2uld6dt1bffs` (`user_id`),
  CONSTRAINT `FKi1c4mmamlb8keqt74k4lrtwhc` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 4: student_profiles
-- -----------------------------------------------------------------------------
CREATE TABLE `student_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `bio` VARCHAR(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `course` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `current_year` INT NOT NULL,
  `department` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `discoverable` BIT(1) NOT NULL,
  `full_name` VARCHAR(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `onboarding_completed` BIT(1) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `availability` VARCHAR(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhlcwb2egnj8mj3e0mouse1lgy` (`user_id`),
  CONSTRAINT `FK32koy3tgqtaujxhfsn0b9pel2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 5: student_profile_skills
-- -----------------------------------------------------------------------------
CREATE TABLE `student_profile_skills` (
  `profile_id` BIGINT NOT NULL,
  `skill` VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`profile_id`, `skill`),
  CONSTRAINT `FKh8ncbqdov58q1435f52tai4u4` FOREIGN KEY (`profile_id`) REFERENCES `student_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 6: student_profile_interests
-- -----------------------------------------------------------------------------
CREATE TABLE `student_profile_interests` (
  `profile_id` BIGINT NOT NULL,
  `interest` VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`profile_id`, `interest`),
  CONSTRAINT `FKl1nhys9kcteu0vtd0t1hx7tq5` FOREIGN KEY (`profile_id`) REFERENCES `student_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 7: networking_goals
-- -----------------------------------------------------------------------------
CREATE TABLE `networking_goals` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `active` BIT(1) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `description` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expires_at` DATETIME(6) DEFAULT NULL,
  `purpose` ENUM('CASUAL_NETWORKING','HACKATHON_TEAM','PLACEMENT_PREP','PROJECT_COLLABORATION','RESEARCH','STUDY_PARTNER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_year` INT DEFAULT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_goal_user_active` (`user_id`, `active`),
  CONSTRAINT `FK7ke7nyoxk330349qt2k55jwkb` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 8: networking_goal_skills
-- -----------------------------------------------------------------------------
CREATE TABLE `networking_goal_skills` (
  `goal_id` BIGINT NOT NULL,
  `skill` VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`goal_id`, `skill`),
  CONSTRAINT `FKk8a1b319kmmhq2dauufycnx8p` FOREIGN KEY (`goal_id`) REFERENCES `networking_goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 9: connection_requests
-- -----------------------------------------------------------------------------
CREATE TABLE `connection_requests` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NOT NULL,
  `purpose_note` VARCHAR(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` ENUM('ACCEPTED','CANCELLED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `receiver_id` BIGINT NOT NULL,
  `sender_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_conn_sender_receiver` (`sender_id`, `receiver_id`),
  KEY `idx_conn_sender_status` (`sender_id`, `status`),
  KEY `idx_conn_receiver_status` (`receiver_id`, `status`),
  CONSTRAINT `FKj291paj3vuf40hnbggyqc4fr5` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKrbqpgx91y8jhk8glwkv33mqxu` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 10: communities
-- -----------------------------------------------------------------------------
CREATE TABLE `communities` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NOT NULL,
  `description` VARCHAR(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_active` BIT(1) NOT NULL,
  `name` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `purpose` VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `college_id` BIGINT NOT NULL,
  `creator_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_community_college_active` (`college_id`, `is_active`),
  KEY `idx_community_creator` (`creator_id`),
  CONSTRAINT `FKehds1lhi1y9a8rweslp1esncn` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKmk37soxhcrcji1rv87wmelitw` FOREIGN KEY (`college_id`) REFERENCES `colleges` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 11: community_members
-- -----------------------------------------------------------------------------
CREATE TABLE `community_members` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `joined_at` DATETIME(6) NOT NULL,
  `role` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `community_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_community_user` (`community_id`, `user_id`),
  KEY `idx_cm_user` (`user_id`),
  CONSTRAINT `FKme7k1stbnwi6cpmm8a6sgcikn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKqn9g17tqcwnoy41o2am9fnlep` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 12: community_discussions
-- -----------------------------------------------------------------------------
CREATE TABLE `community_discussions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `content` VARCHAR(4000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `title` VARCHAR(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `author_id` BIGINT NOT NULL,
  `community_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_disc_community_created` (`community_id`, `created_at`),
  KEY `idx_disc_author` (`author_id`),
  CONSTRAINT `FKgytu31t8av1lqu8g76ufnboul` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`),
  CONSTRAINT `FKkl794roxiir2i9kxal44wms9l` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 13: posts
-- -----------------------------------------------------------------------------
CREATE TABLE `posts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `content` VARCHAR(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `author_id` BIGINT NOT NULL,
  `college_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_post_college_created` (`college_id`, `created_at`),
  KEY `idx_post_author` (`author_id`),
  CONSTRAINT `FK6xvn0811tkyo3nfjk2xvqx6ns` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK84581upy6umr4od008i9kfl1s` FOREIGN KEY (`college_id`) REFERENCES `colleges` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 14: messages
-- -----------------------------------------------------------------------------
CREATE TABLE `messages` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `content` VARCHAR(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `read_at` DATETIME(6) DEFAULT NULL,
  `recipient_id` BIGINT NOT NULL,
  `sender_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_msg_sender_recipient` (`sender_id`, `recipient_id`, `created_at`),
  KEY `idx_msg_recipient_read` (`recipient_id`, `read_at`),
  CONSTRAINT `FK4ui4nnwntodh6wjvck53dbk9m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKhdkwfnspwb3s60j27vpg0rpg6` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 15: notifications
-- -----------------------------------------------------------------------------
CREATE TABLE `notifications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NOT NULL,
  `is_read` BIT(1) NOT NULL,
  `message` VARCHAR(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_url` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` VARCHAR(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` ENUM('COMMUNITY_ACTIVITY','CONNECTION_ACCEPTED','CONNECTION_REQUEST','NEW_MESSAGE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_id` BIGINT DEFAULT NULL,
  `recipient_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notif_recipient_created` (`recipient_id`, `created_at`),
  KEY `idx_notif_recipient_read` (`recipient_id`, `is_read`),
  KEY `FK4sd9fik0uthbk6d9rsxco4uja` (`actor_id`),
  CONSTRAINT `FK4sd9fik0uthbk6d9rsxco4uja` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKqqnsjxlwleyjbxlmm213jaj3f` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 16: uploaded_files
-- -----------------------------------------------------------------------------
CREATE TABLE `uploaded_files` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `association_id` BIGINT DEFAULT NULL,
  `association_type` VARCHAR(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `file_name` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` BIGINT NOT NULL,
  `file_type` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_file_name` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `uploader_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKse3hpc2pgmihtjoa1005aj8tm` (`stored_file_name`),
  KEY `idx_files_uploader` (`uploader_id`),
  KEY `idx_files_assoc` (`association_type`, `association_id`),
  CONSTRAINT `FK49kn9b9a9w1a00gwep7hfnjcs` FOREIGN KEY (`uploader_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 17: user_blocks
-- -----------------------------------------------------------------------------
CREATE TABLE `user_blocks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NOT NULL,
  `blocked_id` BIGINT NOT NULL,
  `blocker_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blocker_blocked` (`blocker_id`, `blocked_id`),
  KEY `idx_blocker` (`blocker_id`),
  KEY `idx_blocked` (`blocked_id`),
  CONSTRAINT `FK7k3mfgb03bnmwh81vqb1u5h80` FOREIGN KEY (`blocked_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKgvu85oyjrfafwttb7iphgmm0v` FOREIGN KEY (`blocker_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Table 18: user_reports
-- -----------------------------------------------------------------------------
CREATE TABLE `user_reports` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NOT NULL,
  `description` VARCHAR(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reported_user_id` BIGINT NOT NULL,
  `reporter_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_report_reporter` (`reporter_id`),
  KEY `idx_report_reported` (`reported_user_id`),
  CONSTRAINT `FK71g2iy5savq929wx5qhgeamne` FOREIGN KEY (`reported_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKo091kj8ufs63qduxpnbeuefa3` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
