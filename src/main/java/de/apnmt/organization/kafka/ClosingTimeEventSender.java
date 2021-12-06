package de.apnmt.organization.kafka;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.ClosingTimeEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClosingTimeEventSender implements ApnmtEventSender<ClosingTimeEventDTO> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ClosingTimeEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, ApnmtEvent<ClosingTimeEventDTO> event) {
        this.kafkaTemplate.send(topic, event);
    }

}
