package de.apnmt.organization.kafka.sender;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.OpeningHourEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OpeningHourEventSender implements ApnmtEventSender<OpeningHourEventDTO> {

    private final Logger log = LoggerFactory.getLogger(OpeningHourEventSender.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OpeningHourEventSender(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, ApnmtEvent<OpeningHourEventDTO> event) {
        this.log.info("Send event {} to kafka topic {}", event, topic);
        this.kafkaTemplate.send(topic, event);
    }

}
