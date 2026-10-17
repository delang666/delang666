-- 高校专业录取分数缓存表（缓存掌上高考接口返回的数据，避免重复请求）
DROP TABLE IF EXISTS `university_score_cache`;
CREATE TABLE `university_score_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `school_id` bigint NOT NULL COMMENT '学校id',
  `province_id` varchar(16) NOT NULL COMMENT '省份id',
  `year` int NOT NULL COMMENT '年份',
  `score_data` text NOT NULL COMMENT '专业录取分数JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_province_year` (`school_id`, `province_id`, `year`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '高校专业录取分数缓存';
