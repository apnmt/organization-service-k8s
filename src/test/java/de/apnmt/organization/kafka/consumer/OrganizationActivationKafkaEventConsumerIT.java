package de.apnmt.organization.kafka.consumer;

import java.util.List;
import java.util.Optional;

import de.apnmt.common.ApnmtTestUtil;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.value.OrganizationActivationEventDTO;
import de.apnmt.k8s.common.test.AbstractKafkaConsumerIT;
import de.apnmt.organization.OrganizationserviceApp;
import de.apnmt.organization.common.domain.Organization;
import de.apnmt.organization.common.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.assertThat;

@EnableKafka
@EmbeddedKafka(partitions = 1, topics = {TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC})
@SpringBootTest(classes = OrganizationserviceApp.class, properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@DirtiesContext
class OrganizationActivationKafkaEventConsumerIT extends AbstractKafkaConsumerIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    public void initTest() {
        this.organizationRepository.deleteAll();
        this.waitForAssignment();
    }

    @Test
    void organizationActivationTest() throws InterruptedException {
        Organization organization = new Organization().name("Test").mail("test@test.de").phone("12345678").owner("user_1").active(false);
        this.organizationRepository.saveAndFlush(organization);

        int databaseSizeBeforeCreate = this.organizationRepository.findAll().size();
        ApnmtEvent<OrganizationActivationEventDTO> event = ApnmtTestUtil.createOrganizationActivationEvent(organization.getId(), true);

        this.kafkaTemplate.send(TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC, event);

        Thread.sleep(1000);

        List<Organization> organizations = this.organizationRepository.findAll();
        assertThat(organizations).hasSize(databaseSizeBeforeCreate);
        Optional<Organization> maybe = this.organizationRepository.findById(organization.getId());
        assertThat(maybe).isPresent();
        assertThat(maybe.get().getActive()).isTrue();
    }

    @Test
    void organizationDeactivationTest() throws InterruptedException {
        Organization organization = new Organization().name("Test").mail("test@test.de").phone("12345678").owner("user_1").active(true);
        this.organizationRepository.saveAndFlush(organization);

        int databaseSizeBeforeCreate = this.organizationRepository.findAll().size();
        ApnmtEvent<OrganizationActivationEventDTO> event = ApnmtTestUtil.createOrganizationActivationEvent(organization.getId(), false);

        this.kafkaTemplate.send(TopicConstants.ORGANIZATION_ACTIVATION_CHANGED_TOPIC, event);

        Thread.sleep(1000);

        List<Organization> organizations = this.organizationRepository.findAll();
        assertThat(organizations).hasSize(databaseSizeBeforeCreate);
        Optional<Organization> maybe = this.organizationRepository.findById(organization.getId());
        assertThat(maybe).isPresent();
        assertThat(maybe.get().getActive()).isFalse();
    }

}
