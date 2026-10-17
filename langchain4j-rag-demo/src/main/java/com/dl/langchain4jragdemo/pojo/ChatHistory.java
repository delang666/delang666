package com.dl.langchain4jragdemo.pojo;


import com.dl.langchain4jragdemo.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {

    private Long id;

    private String message;

    private MessageType messageType;

    private Long sessionId;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDelete;

}
