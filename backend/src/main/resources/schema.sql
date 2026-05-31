CREATE TABLE IF NOT EXISTS `stations`
(
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `name`       VARCHAR(50) NOT NULL,
    `latitude`   DECIMAL(10, 7) NOT NULL,
    `longitude`  DECIMAL(10, 7) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stations_name` (`name`)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `users`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`   DATETIME(6)  NOT NULL,
    `updated_at`   DATETIME(6)  NOT NULL,
    `firebase_uid` VARCHAR(128) NOT NULL,
    `email`        VARCHAR(255) NOT NULL,
    `nickname`     VARCHAR(50)  NOT NULL,
    `role`         VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    `departure_station_id` BIGINT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_firebase_uid` (`firebase_uid`),
    UNIQUE KEY `uk_users_email` (`email`),
    UNIQUE KEY `uk_users_nickname` (`nickname`),
    CONSTRAINT `fk_users_departure_station` FOREIGN KEY (`departure_station_id`) REFERENCES `stations` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `routes`
(
    `id`                   BIGINT      NOT NULL AUTO_INCREMENT,
    `created_at`           DATETIME(6) NOT NULL,
    `updated_at`           DATETIME(6) NOT NULL,
    `departure_station_id` BIGINT      NOT NULL,
    `arrival_station_id`   BIGINT      NOT NULL,
    `duration_minutes`     INT         NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_routes_dep_arr` (`departure_station_id`, `arrival_station_id`),
    CONSTRAINT `fk_routes_departure` FOREIGN KEY (`departure_station_id`) REFERENCES `stations` (`id`),
    CONSTRAINT `fk_routes_arrival` FOREIGN KEY (`arrival_station_id`) REFERENCES `stations` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `focus_sessions`
(
    `id`                    BIGINT      NOT NULL AUTO_INCREMENT,
    `created_at`            DATETIME(6) NOT NULL,
    `updated_at`            DATETIME(6) NOT NULL,
    `user_id`               BIGINT      NOT NULL,
    `departure_station_id`  BIGINT      NOT NULL,
    `arrival_station_id`    BIGINT      NOT NULL,
    `status`                VARCHAR(20) NOT NULL,
    `base_duration_minutes` INT         NOT NULL,
    `delay_minutes`         INT         NOT NULL,
    `total_target_minutes`  INT         NOT NULL,
    `started_at`            DATETIME(6) NOT NULL,
    `planned_end_at`        DATETIME(6) NOT NULL,
    `ended_at`              DATETIME(6),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_sessions_departure` FOREIGN KEY (`departure_station_id`) REFERENCES `stations` (`id`),
    CONSTRAINT `fk_sessions_arrival` FOREIGN KEY (`arrival_station_id`) REFERENCES `stations` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `session_legs`
(
    `id`               BIGINT      NOT NULL AUTO_INCREMENT,
    `created_at`       DATETIME(6) NOT NULL,
    `updated_at`       DATETIME(6) NOT NULL,
    `session_id`       BIGINT      NOT NULL,
    `leg_number`       INT         NOT NULL,
    `started_at`       DATETIME(6) NOT NULL,
    `ended_at`         DATETIME(6),
    `duration_seconds` INT,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_legs_session_number` (`session_id`, `leg_number`),
    CONSTRAINT `fk_legs_session` FOREIGN KEY (`session_id`) REFERENCES `focus_sessions` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rooms`
(
    `id`                BIGINT NOT NULL AUTO_INCREMENT,
    `name`              VARCHAR(20) NOT NULL,
    `code`              VARCHAR(8) NOT NULL,
    `created_at`        DATETIME(6) NOT NULL,
    `updated_at`        DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rooms_code` (`code`)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `room_users`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `room_id`    BIGINT       NOT NULL,
    `user_id`    BIGINT       NOT NULL,
    `role`       VARCHAR(10)  NOT NULL,
    `created_at` DATETIME(6)  NOT NULL,
    `updated_at` DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_users_room_user` (`room_id`, `user_id`),
    CONSTRAINT `fk_room_users_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
    CONSTRAINT `fk_room_users_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
