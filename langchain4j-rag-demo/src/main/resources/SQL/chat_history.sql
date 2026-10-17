/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80406
 Source Host           : localhost:3306
 Source Schema         : langchian4j-rag-demo

 Target Server Type    : MySQL
 Target Server Version : 80406
 File Encoding         : 65001

 Date: 15/08/2026 14:05:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息',
  `messageType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'user/ai',
  `sessionId` bigint NOT NULL COMMENT '会话id',
  `userId` bigint NULL DEFAULT NULL COMMENT '创建用户id(关联reservation.id)',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session`(`sessionId`) USING BTREE,
  INDEX `idx_createTime`(`createTime`) USING BTREE,
  INDEX `idx_sessionId_createTime`(`sessionId`, `createTime`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 411726391164284929 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '对话历史' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
