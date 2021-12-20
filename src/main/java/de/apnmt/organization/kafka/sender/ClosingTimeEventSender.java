package de.apnmt.organization.kafka.sender;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.ClosingTimeEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClosingTimeEventSender implements ApnmtEventSender<ClosingTimeEventDTO> {

    private static final Logger log = LoggerFactory.getLogger(ClosingTimeEventSender.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ClosingTimeEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, ApnmtEvent<ClosingTimeEventDTO> event) {
        log.info("Send event {} to topic {}", event, topic);
        this.kafkaTemplate.send(topic, event);
    }

}
