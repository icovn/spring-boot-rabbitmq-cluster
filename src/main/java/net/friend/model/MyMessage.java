package net.friend.model;

import java.io.Serializable;

import org.springframework.amqp.rabbit.support.CorrelationData;

import lombok.Data;

@Data
public class MyMessage implements Serializable {
    private long id;
    private String title;
    private String message;

    public MyMessage() {}

    public MyMessage(long id, String title, String message) {
        this.id = id;
        this.title = title;
        this.message = message;
    }

    public CorrelationData getCorrelationData() {
        return new CorrelationData(id + "|" + title);
    }
}
