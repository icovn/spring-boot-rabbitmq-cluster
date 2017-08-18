package net.friend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import net.friend.queue.RabbitmqPublisher;

import lombok.extern.slf4j.Slf4j;

@EnableScheduling
@Profile("publisher")
@SpringBootApplication
@Slf4j
public class PublisherApplication implements ApplicationRunner {
    @Autowired
    private RabbitmqPublisher rabbitmqPublisher;

    public static void main(String[] args) {
        log.info("start publisher");
        SpringApplication.run(PublisherApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {

    }

    @Scheduled(cron = "${app.publisher.cron}")
    public void publishData() {
        log.info("publish data");
        rabbitmqPublisher.publishToSimpleQueue();
        rabbitmqPublisher.publishToAdvanceQueue();
    }
}
