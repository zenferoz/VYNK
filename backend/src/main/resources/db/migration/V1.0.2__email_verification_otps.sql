-- -----------------------------------------------------------------------------
-- Migration V1.0.2: Email Verification OTPs Schema
-- -----------------------------------------------------------------------------

CREATE TABLE `email_verification_otps` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `otp_hash` VARCHAR(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `expires_at` DATETIME(6) NOT NULL,
  `attempts` INT NOT NULL DEFAULT 0,
  `used` BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email_verification_otps_user` (`user_id`),
  CONSTRAINT `fk_email_verification_otps_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
