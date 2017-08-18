package net.friend.queue;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.CorrelationData;

public class CorrelationDataWithMessage extends CorrelationData {
    private final Message message;

    public CorrelationDataWithMessage(String id, Message message){
        super(id);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "CorrelationDataWithMessage [id=" + getId() + ", message=" + message + "]";
    }
}
