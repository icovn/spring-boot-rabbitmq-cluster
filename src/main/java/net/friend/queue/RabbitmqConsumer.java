package net.friend.queue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import net.friend.model.MyMessage;

import lombok.extern.slf4j.Slf4j;

@Profile("consumer")
@Service
@Slf4j
public class RabbitmqConsumer {
    @RabbitListener(queues = "${rabbitmq.queue.default}")
    public void handleDefaultQueue(MyMessage message) {
        log.info(String.format("Get message %s", message));
    }
}
