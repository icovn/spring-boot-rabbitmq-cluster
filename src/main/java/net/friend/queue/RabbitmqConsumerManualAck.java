package net.friend.queue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import net.friend.model.MyMessage;

import com.rabbitmq.client.Channel;

import lombok.extern.slf4j.Slf4j;

@Profile("consumer-manual-ack")
@Service
@Slf4j
public class RabbitmqConsumerManualAck {
    @RabbitListener(queues = "${rabbitmq.queue.advance}")
    public void handleAdvanceQueue(MyMessage message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        boolean isProcessOk = processBussinessLogic();
        if(isProcessOk){
            //ok send ACK
            log.debug(String.format("Ack for message %s with tag %s", message, tag));
            channel.basicAck(tag, false);
        }else {
            //nok send REJECT and requeue
            log.debug(String.format("Reject for message %s with tag %s", message, tag));
            channel.basicReject(tag, true);
        }
    }

    private boolean processBussinessLogic(){
        return true;
    }
}
