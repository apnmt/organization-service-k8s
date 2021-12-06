package de.apnmt.organization.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.OpeningHourEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import de.apnmt.organization.IntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.OPENING_HOUR_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
@DirtiesContext
public class OpeningHourEventSenderIT extends AbstractEventSenderIT {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(8, 0, 11, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(17, 0, 11, 0);

    @Autowired
    private OpeningHourEventSender openingHourEventSender;

    @Override
    public String getTopic() {
        return TopicConstants.OPENING_HOUR_CHANGED_TOPIC;
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
