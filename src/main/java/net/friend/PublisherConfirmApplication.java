package net.friend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import net.friend.queue.RabbitmqPublisherConfirm;

import lombok.extern.slf4j.Slf4j;

@Profile("publisher-confirm")
@SpringBootApplication
@Slf4j
public class PublisherConfirmApplication implements ApplicationRunner {
    @Autowired
    private RabbitmqPublisherConfirm rabbitmqPublisherConfirm;

    public static void main(String[] args) {
        log.info("start publisher with confirm");
        SpringApplication.run(PublisherConfirmApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        rabbitmqPublisherConfirm.publishConfirm();
    }
}
