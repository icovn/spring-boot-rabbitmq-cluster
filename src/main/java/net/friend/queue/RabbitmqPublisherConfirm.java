package net.friend.queue;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import net.friend.model.MyMessage;

import lombok.extern.slf4j.Slf4j;

@Profile("publisher-confirm")
@Service
@Slf4j
public class RabbitmqPublisherConfirm {
    @Value("${rabbitmq.queue.default}")
    private String defaultQueue;

    private RabbitTemplate rabbitTemplate;

    public RabbitmqPublisherConfirm(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishConfirm() throws Exception {
        setupCallbacks();
        MyMessage myMessage = new MyMessage(99, "test message", "test body");
        // send a message to the default exchange to be routed to the queue
        rabbitTemplate.convertAndSend("", defaultQueue, myMessage, myMessage.getCorrelationData());

        // send a message to the default exchange to be routed to a non-existent queue
        rabbitTemplate.convertAndSend("", defaultQueue + defaultQueue, "bar");
    }

    private void setupCallbacks() {
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            //receive this callback when message is not routable
            log.info("***************************************************************************************");
            log.error(String.format("Received returned message with result %s|%s|%s|%s|%s",
                    message.getBody(), replyCode, replyText, exchange, routingKey));
            log.debug(String.format("Message detail ", message));
        });
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            //receive this callback with ack/nack from RabbitMQ
            log.info("***************************************************************************************");
            log.info(String.format("Message received by broker %s|%s|%s", correlationData, ack, cause));

            //retry
            if (!ack) {
                if (correlationData instanceof CorrelationDataWithMessage) {
                    CorrelationDataWithMessage completeCorrelationData = (CorrelationDataWithMessage) correlationData;
                    rabbitTemplate.send("", defaultQueue, completeCorrelationData.getMessage());
                }
            }
        });

        /*
         * Replace the correlation data with one containing the converted message in case
         * we want to resend it after a nack.
         */
        rabbitTemplate
                .setCorrelationDataPostProcessor((message, correlationData) -> new CorrelationDataWithMessage(
                        correlationData != null ? correlationData.getId() : null, message));
    }
}
