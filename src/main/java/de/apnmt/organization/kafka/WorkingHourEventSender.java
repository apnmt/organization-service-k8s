package de.apnmt.organization.kafka;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.ClosingTimeEventDTO;
import de.apnmt.common.event.value.WorkingHourEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkingHourEventSender implements ApnmtEventSender<WorkingHourEventDTO> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WorkingHourEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, ApnmtEvent<WorkingHourEventDTO> event) {
        this.kafkaTemplate.send(topic, event);
    }

}
