package de.apnmt.organization.kafka.sender;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.common.ApnmtTestUtil;
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
import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.WORKING_HOUR_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
@DirtiesContext
public class WorkingHourEventSenderIT extends AbstractEventSenderIT {

    @Autowired
    private WorkingHourEventSender workingHourEventSender;

    @Override
    public String getTopic() {
        return TopicConstants.WORKING_HOUR_CHANGED_TOPIC;
    }

    @Test
    public void workingHourEventSenderTest() throws InterruptedException, JsonProcessingException {
        ApnmtEvent<WorkingHourEventDTO> event = ApnmtTestUtil.createWorkingHourEvent(ApnmtEventType.workingHourCreated);
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
