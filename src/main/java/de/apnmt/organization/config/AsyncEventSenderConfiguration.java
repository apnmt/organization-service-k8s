package de.apnmt.organization.config;

import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.ClosingTimeEventDTO;
import de.apnmt.common.event.value.OpeningHourEventDTO;
import de.apnmt.common.event.value.WorkingHourEventDTO;
import de.apnmt.common.sender.ApnmtEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncEventSenderConfiguration {

    private final Logger log = LoggerFactory.getLogger(AsyncEventSenderConfiguration.class);

    @Bean
    public ApnmtEventSender<ClosingTimeEventDTO> closingTimeEventSender() {
        // TODO replace with real implementation
        return new ApnmtEventSender<ClosingTimeEventDTO>() {
            @Override
            public void send(String topic, ApnmtEvent<ClosingTimeEventDTO> event) {
                AsyncEventSenderConfiguration.this.log.info("Event send to topic {} with message {}", topic, event);
            }
        };
    }

    @Bean
    public ApnmtEventSender<OpeningHourEventDTO> openingHourEventSender() {
        // TODO replace with real implementation
        return new ApnmtEventSender<OpeningHourEventDTO>() {
            @Override
            public void send(String topic, ApnmtEvent<OpeningHourEventDTO> event) {
                AsyncEventSenderConfiguration.this.log.info("Event send to topic {} with message {}", topic, event);
            }
        };
    }

    @Bean
    public ApnmtEventSender<WorkingHourEventDTO> workingHourEventSender() {
        // TODO replace with real implementation
        return new ApnmtEventSender<WorkingHourEventDTO>() {
            @Override
            public void send(String topic, ApnmtEvent<WorkingHourEventDTO> event) {
                AsyncEventSenderConfiguration.this.log.info("Event send to topic {} with message {}", topic, event);
            }
        };
    }
}
