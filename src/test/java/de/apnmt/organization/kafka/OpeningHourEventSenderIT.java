package de.apnmt.organization.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.OpeningHourEventDTO;
import de.apnmt.organization.IntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.OPENING_HOUR_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
@DirtiesContext
public class OpeningHourEventSenderIT {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(8, 0, 11, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(17, 0, 11, 0);

    private BlockingQueue<ConsumerRecord<String, Object>> records;

    private KafkaMessageListenerContainer<String, String> container;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private OpeningHourEventSender openingHourEventSender;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(this.getConsumerProperties());
        ContainerProperties containerProperties = new ContainerProperties(TopicConstants.OPENING_HOUR_CHANGED_TOPIC);
        this.container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        this.records = new LinkedBlockingQueue<>();
        this.container.setupMessageListener((MessageListener<String, Object>) this.records::add);
        this.container.start();
        ContainerTestUtils.waitForAssignment(this.container, this.embeddedKafkaBroker.getPartitionsPerTopic());
    }

    private Map<String, Object> getConsumerProperties() {
        return Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.embeddedKafkaBroker.getBrokersAsString(),
            ConsumerConfig.GROUP_ID_CONFIG, "consumer",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true",
            ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10",
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "60000",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    }

    @AfterEach
    public void tearDown() {
        this.container.stop();
    }

    @Test
    public void openingHourEventSenderTest() throws InterruptedException, JsonProcessingException {
        OpeningHourEventDTO closingTime = new OpeningHourEventDTO();
        closingTime.setId(1L);
        closingTime.setStartTime(DEFAULT_START_TIME);
        closingTime.setEndTime(DEFAULT_END_TIME);
        closingTime.setOrganizationId(2L);

        ApnmtEvent<OpeningHourEventDTO> event = new ApnmtEvent<OpeningHourEventDTO>().timestamp(LocalDateTime.now()).type(ApnmtEventType.openingHourCreated).value(closingTime);
        this.openingHourEventSender.send(TopicConstants.OPENING_HOUR_CHANGED_TOPIC, event);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OpeningHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OpeningHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult).isEqualTo(event);
    }

}
