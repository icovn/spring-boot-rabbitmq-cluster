package net.friend.queue;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import net.friend.model.MyMessage;

import lombok.extern.slf4j.Slf4j;

@Profile("publisher")
@Service
@Slf4j
public class RabbitmqPublisher {
    @Value("${rabbitmq.queue.default}")
    private String defaultQueue;

    @Value("${rabbitmq.queue.advance}")
    private String advanceQueue;

    private RabbitTemplate rabbitTemplate;

    public RabbitmqPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishToSimpleQueue() {
        pushMultipleMessageToQueue(defaultQueue);
    }

    public void publishToAdvanceQueue() {
        pushMultipleMessageToQueue(advanceQueue);
    }

    private void pushMultipleMessageToQueue(String queue) {
        for (int i = 1; i < 1000; i ++) {
            MyMessage message = new MyMessage(i, "Title " + i, "Message " + i);
            rabbitTemplate.convertAndSend(queue, message);
        }
    }

    private void pushNonPersistent(String queue) {
        for (int i = 1; i < 1000; i ++) {
            MyMessage myMessage = new MyMessage(i, "Title " + i, "Message " + i);
            rabbitTemplate.convertAndSend(myMessage, new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
                    return message;
                }
            });
        }
    }


}
