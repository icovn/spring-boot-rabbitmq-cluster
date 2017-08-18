package net.friend.configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RabbitmqConfiguration {
    @Value("${rabbitmq.queue.default}")
    private String defaultQueue;

    @Value("${rabbitmq.queue.advance}")
    private String advanceQueue;

    @Value("${rabbitmq.queue.advance.delay}")
    private String advanceDelayQueue;

    @Value("${rabbitmq.message.ttl}")
    private int messageTtl;

    @Value("${rabbitmq.concurrent.consumers}")
    private int concurrentConsumers;

    @Value("${rabbitmq.max.concurrent.consumers}")
    private int maxConcurrentConsumers;

    @Autowired
    private Environment environment;

    @Bean
    public Queue defaultQueue() {
        Map<String, Object> arguments = new HashMap<>();
        return new Queue(defaultQueue);
    }

    @Bean
    public Queue advanceQueue() {
        return new Queue(advanceQueue, true, false, false);
    }

    @Bean
    public Queue advanceDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl", messageTtl);
        arguments.put("x-dead-letter-exchange", "");
        arguments.put("x-dead-letter-routing-key", advanceQueue);
        return new Queue(advanceDelayQueue, true, false, false, arguments);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        for(String profile: environment.getActiveProfiles()){
            if(profile.equals("consumer-manual-ack")){
                factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
                break;
            }
        }

        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
