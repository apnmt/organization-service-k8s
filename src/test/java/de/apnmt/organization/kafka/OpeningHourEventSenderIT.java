package de.apnmt.organization.kafka;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.common.ApnmtTestUtil;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.OpeningHourEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import de.apnmt.organization.IntegrationTest;
import de.apnmt.organization.kafka.sender.OpeningHourEventSender;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.OPENING_HOUR_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
@DirtiesContext
public class OpeningHourEventSenderIT extends AbstractEventSenderIT {

    @Autowired
    private OpeningHourEventSender openingHourEventSender;

    @Override
    public String getTopic() {
        return TopicConstants.OPENING_HOUR_CHANGED_TOPIC;
    }

    @Test
    public void openingHourEventSenderTest() throws InterruptedException, JsonProcessingException {
        ApnmtEvent<OpeningHourEventDTO> event = ApnmtTestUtil.createOpeningHourEvent(ApnmtEventType.openingHourCreated);
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
