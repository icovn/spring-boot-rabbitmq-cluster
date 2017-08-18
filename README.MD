# RabbitMQ Cluster &  Spring Boot



## Tại sao Cluster? Tại sao Spring Boot?

- Tại sao __CLUSTER__? Để tăng tính sẵn sàng (availability) của hệ thống
- Tại sao __Spring Boot__? Đây là framework JAVA hỗ trợ lập trình viên phát triển nhanh ứng dụng ở mức chuyên
nghiệp
- Yêu cầu biết các đối tượng, khái niệm cơ bản trong mô hình chương trình sử dụng [amqp](https://www.rabbitmq.com/tutorials/amqp-concepts.html)
như __publisher__, __exchange__, __queue__, __consumer__, __messsage__, __broker__, __channel__ 


## Các khái niệm & cài đặt với Spring Booot

- __Message durability__: 
    + Khi __RabbitMQ__ _crash_ hoặc _quit_ các __message__ của __queue__ sẽ bị mất. Để __message__ không
    bị mất ta cần đảm bảo 2 thứ: __queue__ và __message__ được đánh dấu _durable_
    + Mặc định [Spring](https://github.com/spring-projects/spring-amqp/blob/master/spring-amqp/src/main/java/org/springframework/amqp/core/Queue.java#L47)
     đánh dấu __queue__ là _durable_ khi khai báo
     
    ```java
    public class RabbitmqConfiguration {
        @Bean
        public Queue defaultQueue() {
            Map<String, Object> arguments = new HashMap<>();
            return new Queue(defaultQueue);
        }
    }
    ```    
    
    + Mặc định [Spring](https://github.com/spring-projects/spring-amqp/blob/master/spring-amqp/src/main/java/org/springframework/amqp/core/MessageProperties.java#L60) 
    đánh dấu __message__ là _durable_  
    
    ```java
    public class RabbitmqPublisher {
        private void pushMultipleMessageToQueue(String queue) {
            for (int i = 1; i < 1000; i ++) {
                MyMessage message = new MyMessage(i, "Title " + i, "Message " + i);
                rabbitTemplate.convertAndSend(queue, message);
            }
        }
    }
    ```

    + Để gửi non-persistent __message__ với SPRING ta làm như sau
    
    ```java
    public class RabbitmqPublisher {
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
    ```
    
    + Dù __queue__ và __message__ được đánh dấu _durable_ vẫn không đảm bảo rằng __messsage__ 
    không bị mất. Vì mặc dù chương trình đã bảo __RabbitMQ__ lưu __messsage__ xuống ổ cứng nhưng vẫn có
    một khoảng thời gian ngắn __message__ được lưu ở __cache__ (RabbitMQ không thực hiện lưu 
    xuống ổ cứng - fsync cho mọi __message__)

- __Delivery tag__:
    + Khi __consumer__ đăng ký nhận __message__ với __RabbitMQ__, __message__ được __RabbitMQ__ chuyển đi bằng 
    phương thức _basic.deliver_. Phương thức này mang theo __delivery tag__ thông số độc nhất (unique) trên một
    __channel__

- __Acknowledgement (ACK)__:
    + __RabbitMQ__ sử dụng protocol __AMQP__ để giao tiếp với __publisher__ và __consumer__. Protocol này (cũng 
    như các protocol tương tự __STOMP__, __MQTT__, ...) không đảm bảo rằng __message__ được chuyển đến đích và 
    được xử lý thành công. Vì vậy cần có một cơ chế để xác nhận việc chuyển và xử lý thành công này.
    + Việc xác nhận chuyển và xử lý từ __consumer__ tới __RabbitMQ__ được gọi là __ACK__
    + Việc xác nhận từ __RabbitMQ__ tới __publisher__ là một phần mở rộng của giao thức __AMQP__ và được gọi là 
    __publisher confirm__ 
    + Các chế độ __ACK__:
        * _basic.ack_  xác nhận rằng __message__ được được chuyển và xử lý thành công
        * _basic.reject_ xác nhận rằng __message__ được chuyển thành công, xử lý không thành công và có thể yêu cầu 
        __RabbitMQ__ xoá hay không xoá __message__
        * _basic.nack_ tương tự _basic.reject_ nhưng có thể gửi _reject_ cho nhiều __message__
    + Khi nào __RabbitMQ__ gửi __confirm__ tới __publisher__?
        * với __message__ không định tuyến được: Khi __exchange__ xác nhận rằng không thể định tuyến được __messsage__
        * với __message__ định tuyến được và không _durable_: hi được chấp nhật bởi tất cả các __queue__
        * với __message__ định tuyến được và _durable_: khi được lưu xuống ổ cứng
    + Với __Spring__ mặc định nếu không có __Exception__ được _throw_ hoặc nếu có nhưng nằm ngoài [6 loại exception](http://docs.spring.io/spring-amqp//reference/html/_reference.html#exception-handling)
    được quy định trước __Spring__ sẽ gửi bản tin __ACK__ cho __RabbitMQ__. Nếu có __Exception__ và nằm ngoài 6 loại 
    exception được quy định trước thì bản tin __NACK__ sẽ được gửi (không thấy tài liệu nào nói về việc dùng __auto ACK__ 
    cho __consumer__ với __Spring__ như ví dụ với Java thuần của [RabbitMQ](https://www.rabbitmq.com/tutorials/tutorial-two-java.html)).
    + Chủ động __ACK__ từ __consumer__ tới __RabbitMQ__ với __Spring__
        * Chủ động __ACK__, gửi bản tin __REJECT__ khi xử lý không thành công và yêu cầu __RabbitMQ__ requeue
        
        ```java
        public class RabbitmqConsumer {
            @RabbitListener(queues = "${rabbitmq.queue.advance}")
            public void handleAdvanceQueue(MyMessage message, Channel channel,
                    @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
                boolean isProcessOk = processBussinessLogic();
                if(isProcessOk){
                    //ok send ACK
                    channel.basicAck(tag, false);
                }else {
                    //nok send REJECT and requeue
                    channel.basicReject(tag, true);
                }
            }
        }
        ```  
        
        * Thiết lập chủ động __ACK__ trong file cấu hình  
    
        ```
        #application.properties
        spring.rabbitmq.listener.acknowledge-mode=manual
        ```

    + Yêu cầu __ACK__ từ __RabbitMQ__ bởi __publisher__ với __Spring__
        * Bổ sung CorrelationData cho model
        
        ```java
        public class MyMessage implements Serializable {
            public CorrelationData getCorrelationData() {
                return new CorrelationData(id + "|" + title);
            }
        }
        
        ```
        
        * Thiết lập việc bắt sự kiện khi RabbitMQ gửi __ACK__
        
        ```java
        public class RabbitmqPublisherConfirm {
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
        
        ``` 
        
        * Mở rộng class CorrelationData (dữ liệu __RabbitMQ__ sẽ trả về cho __publisher__ cùng thông tin 
        __ACK__) chứa __message__ dùng để gửi lại cho __RabbitMQ__ khi nhận được __NACK__
        
        ```java
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
        ``` 
        
        * Thực hiện gửi bản tin tới __RabbitMQ__
        
        ```java
        public class RabbitmqPublisherConfirm {
            public void publishConfirm() throws Exception {
                setupCallbacks();
                MyMessage myMessage = new MyMessage(99, "test message", "test body");
                // send a message to the default exchange to be routed to the queue
                rabbitTemplate.convertAndSend("", defaultQueue, myMessage, myMessage.getCorrelationData());
        
                // send a message to the default exchange to be routed to a non-existent queue
                rabbitTemplate.convertAndSend("", defaultQueue + defaultQueue, "bar");
            }
        }
        ```
        
        * Thiết lập confirmCallback và returnCallback trong file cấu hình
        
        ```
        #application.properties
        spring.rabbitmq.publisher-confirms=true
        spring.rabbitmq.publisher-returns=true
        ```

- __Đa luồng với consumer_
    + Mặc định __Spring__ chỉ chạy 1 __consumer__ khi dùng __RabbitListener__
    + Ta có thể thay đổi số luồng (thread) __consumer__ bằng 2 cách sau:
        * Cấu hình trong file cấu hình

        ```
        #appllication.properties
        spring.rabbitmq.listener.simple.concurrency= # Số consumer tối thiểu khởi tạo khi chạy chương trình.
        spring.rabbitmq.listener.simple.max-concurrency= # Số consumer tối đa tạo ra khi nhận message từ RabbitMQ.
        ```

        * Tạo bean __SimpleRabbitListenerContainerFactory__ và thiết lập các cấu hình trong __Configuration__ class 

        ```java
        public class RabbitmqConfiguration {
            @Bean
            public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
                SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
                factory.setConcurrentConsumers(concurrentConsumers);
                factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
                factory.setConnectionFactory(connectionFactory);

                return factory;
            }
        }
        ```

    + Khi chạy đa luồng __consumer__ như thế này thì mỗi __consumer__ sẽ sử dụng 1 __channel__ riêng (delivery 
    tag của __channel__ khác nhau có thể giống nhau) nhưng dùng chung 1 __connection__ 


## Cluster

### Cài đặt cluster trên 1 server

- Cài đặt __erlang__, __RabbitMQ__ theo hướng dẫn trên trang chủ của __RabbitMQ__
- Chạy các node sử dụng file cấu hình data/rabbitmq.config:
    + chú ý đảm bảo mỗi node có các tham số cấu hình sau đây khác nhau:
        * node names: RABBITMQ_NODENAME
        * data store locations: RABBITMQ_MNESIA_DIR
        * log file locations: RABBITMQ_LOG_BASE, RABBITMQ_CONFIG_FILE
        * bind to different ports, including those used by plugins: RABBITMQ_NODE_PORT, RABBITMQ_DIST_PORT
    + chạy node thứ nhất: RABBITMQ_NODE_PORT=5671 RABBITMQ_DIST_PORT=25671 RABBITMQ_NODENAME=rabbit RABBITMQ_CONFIG_FILE=/u01/applications/rabbitmq/instance1/rabbitmq RABBITMQ_MNESIA_DIR=/u01/applications/rabbitmq/instance1/data RABBITMQ_LOG_BASE=/u01/applications/rabbitmq/instance1/log rabbitmq-server -detached
    + chạy node thứ hai: RABBITMQ_NODE_PORT=5672 RABBITMQ_DIST_PORT=25672 RABBITMQ_NODENAME=rabbit2 RABBITMQ_CONFIG_FILE=/u01/applications/rabbitmq/instance2/rabbitmq RABBITMQ_MNESIA_DIR=/u01/applications/rabbitmq/instance2/data RABBITMQ_LOG_BASE=/u01/applications/rabbitmq/instance2/log rabbitmq-server -detached
- Join node thứ hai với node thứ nhất
    rabbitmqctl -n rabbit2 stop_app
    rabbitmqctl -n rabbit2 join_cluster rabbit@`hostname -s`
    rabbitmqctl -n rabbit2 start_app    
- Thiết lập mirror policy cho các __queue__ có tên bắt đầu với "queue."
    
    ```
    rabbitmqctl -n rabbit set_policy ha-two "^queue\."    '{"ha-mode":"exactly","ha-params":2,"ha-sync-mode":"automatic"}'
    ```