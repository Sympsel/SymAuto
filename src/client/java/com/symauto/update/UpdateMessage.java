package com.symauto.update;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;

@Data
@AllArgsConstructor
public class UpdateMessage {
    private long createTime;
    private String version;
    private String message;

    /**
     *
     * @param formatedTime [yy-MM-dd]
     */
    public static UpdateMessage of(String formatedTime, String version, String message) {
        try {
            long createTime = new SimpleDateFormat("yyyy-MM-dd").parse(formatedTime).getTime();
            return new UpdateMessage(createTime, version, message);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
