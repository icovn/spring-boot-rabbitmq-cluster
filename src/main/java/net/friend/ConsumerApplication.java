package net.friend;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import lombok.extern.slf4j.Slf4j;

@Profile({"consumer", "consumer-manual-ack"})
@SpringBootApplication
@Slf4j
public class ConsumerApplication implements ApplicationRunner {
    public static void main(String[] args) {
        log.info("start consumer");
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {

    }
}
