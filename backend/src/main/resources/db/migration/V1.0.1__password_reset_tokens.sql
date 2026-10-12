-- -----------------------------------------------------------------------------
-- Migration V1.0.1: Password Reset Tokens Schema
-- -----------------------------------------------------------------------------

CREATE TABLE `password_reset_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `expires_at` DATETIME(6) NOT NULL,
  `token_hash` VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `used` BIT(1) NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_password_reset_token_hash` (`token_hash`),
  UNIQUE KEY `uk_password_reset_tokens_user` (`user_id`),
  CONSTRAINT `fk_password_reset_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
