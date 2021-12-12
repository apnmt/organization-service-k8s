/*
 * OrganizationActivationEventConsumer.java
 *
 * (c) Copyright AUDI AG, 2021
 * All Rights reserved.
 *
 * AUDI AG
 * 85057 Ingolstadt
 * Germany
 */
package de.apnmt.organization.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.OrganizationActivationEventDTO;
import de.apnmt.organization.common.async.controller.OrganizationActivationEventConsumer;
import de.apnmt.organization.common.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class OrganizationActivationKafkaEventConsumer extends OrganizationActivationEventConsumer {

    private static final TypeReference<ApnmtEvent<OrganizationActivationEventDTO>> EVENT_TYPE = new TypeReference<>() {
    };

    private final Logger log = LoggerFactory.getLogger(OrganizationActivationKafkaEventConsumer.class);

    private final ObjectMapper objectMapper;

    public OrganizationActivationKafkaEventConsumer(OrganizationService organizationService, ObjectMapper objectMapper) {
        super(organizationService);
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC})
    public void receiveEvent(@Payload String message) {
        try {
            ApnmtEvent<OrganizationActivationEventDTO> event = this.objectMapper.readValue(message, EVENT_TYPE);
            super.receiveEvent(event);
        } catch (JsonProcessingException e) {
            this.log.error("Malformed message {} for topic {}. Event will be ignored.", message, TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC);
        }
    }

}
