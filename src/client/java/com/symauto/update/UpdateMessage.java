package com.symauto.update;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateMessage {
    private long createTime;
    private String version;
    private String message;
}
