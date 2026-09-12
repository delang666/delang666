# -*- coding: utf-8 -*-
"""
论文查重核心模块：SimHash 算法实现。

SimHash 由 Google 提出，用于网页/文本去重，属于局部敏感哈希（LSH）。
原理：将一段文本映射为固定长度（默认 64 位）的指纹；文本越相似，
其指纹的汉明距离（Hamming Distance）越小。据此快速估算相似度：

    相似度 = 1 - 汉明距离 / 指纹位数

计算步骤：
    1. 分词并清洗：用 jieba 分词，过滤纯标点与空白 token。
    2. 统计词频：相同词只做一次按位加权，权重为词频（性能优化点）。
    3. 按位加权累加：对每个词的 hash 值，逐位把 1 记 +权重、0 记 -权重。
    4. 生成指纹：累加向量中 >=0 的位置记为 1，否则为 0，拼成 64 位整数。
"""

import re

import jieba

# 匹配「包含中文/字母/数字」的 token，用于过滤纯标点与空白
_WORD_PATTERN = re.compile(r"[\u4e00-\u9fff\w]")


class SimHash:
    """基于 SimHash 的文本指纹。

    Attributes:
        hash_bits: 指纹位数，默认 64。
        value: 计算得到的指纹值（int）。
    """

    def __init__(self, content, hash_bits=64):
        self.hash_bits = hash_bits
        self.value = self._compute(content)

    # ---------------- 分词 ----------------
    @staticmethod
    def _tokenize(content):
        """分词并清洗，返回有意义的 token 列表。"""
        tokens = []
        for word in jieba.cut(content):
            word = word.strip()
            # 只保留至少含一个中文/字母/数字的 token，过滤纯标点与空白
            if word and _WORD_PATTERN.search(word):
                tokens.append(word)
        return tokens

    # ---------------- 词哈希 ----------------
    def _string_hash(self, source):
        """确定性字符串哈希（与进程无关，保证结果稳定可复现）。"""
        if not source:
            return 0
        x = ord(source[0]) << 7
        mask = (1 << self.hash_bits) - 1
        for ch in source:
            x = ((x * 1000003) ^ ord(ch)) & mask
        x ^= len(source)
        return x

    # ---------------- 生成指纹 ----------------
    def _compute(self, content):
        tokens = self._tokenize(content)

        # 统计词频：相同词只加权一次，权重为出现次数，避免重复遍历
        freq = {}
        for token in tokens:
            freq[token] = freq.get(token, 0) + 1

        vector = [0] * self.hash_bits
        for token, weight in freq.items():
            h = self._string_hash(token)
            for i in range(self.hash_bits):
                if (h >> i) & 1:
                    vector[i] += weight
                else:
                    vector[i] -= weight

        fingerprint = 0
        for i in range(self.hash_bits):
            if vector[i] >= 0:
                fingerprint |= 1 << i
        return fingerprint

    # ---------------- 距离与相似度 ----------------
    def hamming_distance(self, other):
        """计算与另一个 SimHash 指纹的汉明距离。"""
        xor = self.value ^ other.value
        return bin(xor).count("1")

    def similarity(self, other):
        """计算相似度，取值范围 [0, 1]。"""
        return 1.0 - self.hamming_distance(other) / float(self.hash_bits)
