package de.apnmt.organization.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.WorkingHourEventDTO;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.WORKING_HOUR_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
@DirtiesContext
public class WorkingHourEventSenderIT extends AbstractEventSenderIT {

    private static final LocalDateTime DEFAULT_START_AT = LocalDateTime.of(2021, 12, 24, 0, 0, 11, 0);
    private static final LocalDateTime DEFAULT_END_AT = LocalDateTime.of(2021, 12, 25, 0, 0, 11, 0);

    @Autowired
    private WorkingHourEventSender workingHourEventSender;

    @Override
    public String getTopic() {
        return TopicConstants.WORKING_HOUR_CHANGED_TOPIC;
    }

    @Test
    public void workingHourEventSenderTest() throws InterruptedException, JsonProcessingException {
        WorkingHourEventDTO closingTime = new WorkingHourEventDTO();
        closingTime.setId(1L);
        closingTime.setStartAt(DEFAULT_START_AT);
        closingTime.setEndAt(DEFAULT_END_AT);
        closingTime.setEmployeeId(2L);

        ApnmtEvent<WorkingHourEventDTO> event = new ApnmtEvent<WorkingHourEventDTO>().timestamp(LocalDateTime.now()).type(ApnmtEventType.workingHourCreated).value(closingTime);
        this.workingHourEventSender.send(TopicConstants.WORKING_HOUR_CHANGED_TOPIC, event);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<WorkingHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<WorkingHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult).isEqualTo(event);
    }

}
