package com.dl.langchain4jragdemo.enums;

import cn.hutool.core.util.ObjUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {

    USER("用户", "user"),
    AI("人工智能", "ai");

    private final String text;

    private final String value;

    MessageType(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    /**
     * JSON 序列化使用 "user"/"ai" 而非枚举名
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    @JsonCreator
    public static MessageType getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (MessageType anEnum : MessageType.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
