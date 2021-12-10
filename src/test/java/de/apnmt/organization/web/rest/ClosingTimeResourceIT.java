package de.apnmt.organization.web.rest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.ClosingTimeEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import de.apnmt.organization.IntegrationTest;
import de.apnmt.organization.common.domain.ClosingTime;
import de.apnmt.organization.common.domain.Organization;
import de.apnmt.organization.common.repository.ClosingTimeRepository;
import de.apnmt.organization.common.repository.OrganizationRepository;
import de.apnmt.organization.common.service.dto.ClosingTimeDTO;
import de.apnmt.organization.common.service.mapper.ClosingTimeMapper;
import de.apnmt.organization.common.web.rest.ClosingTimeResource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link ClosingTimeResource} REST controller.
 */
@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.CLOSING_TIME_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
class ClosingTimeResourceIT extends AbstractEventSenderIT {

    private static final LocalDateTime DEFAULT_START_AT = LocalDateTime.of(2021, 12, 24, 0, 0, 11, 0);
    private static final LocalDateTime UPDATED_START_AT = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final LocalDateTime DEFAULT_END_AT = LocalDateTime.of(2021, 12, 25, 0, 0, 11, 0);
    private static final LocalDateTime UPDATED_END_AT = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/closing-times";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ClosingTimeRepository closingTimeRepository;

    @Autowired
    private ClosingTimeMapper closingTimeMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restClosingTimeMockMvc;

    private ClosingTime closingTime;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ClosingTime createEntity(EntityManager em) {
        ClosingTime closingTime = new ClosingTime().startAt(DEFAULT_START_AT).endAt(DEFAULT_END_AT);
        return closingTime;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ClosingTime createUpdatedEntity(EntityManager em) {
        ClosingTime closingTime = new ClosingTime().startAt(UPDATED_START_AT).endAt(UPDATED_END_AT);
        return closingTime;
    }

    @Override
    public String getTopic() {
        return TopicConstants.CLOSING_TIME_CHANGED_TOPIC;
    }

    @BeforeEach
    public void initTest() throws InterruptedException {
        Organization organization = OrganizationResourceIT.createEntity(this.em);
        this.organizationRepository.saveAndFlush(organization);
        this.closingTime = createEntity(this.em);
        this.closingTime.setOrganization(organization);
    }

    @AfterEach
    public void shutDown() throws InterruptedException {
        // All topics should be empty now
        assertThat(this.records.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @Transactional
    void createClosingTime() throws Exception {
        int databaseSizeBeforeCreate = this.closingTimeRepository.findAll().size();
        // Create the ClosingTime
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);
        this.restClosingTimeMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(closingTimeDTO)))
            .andExpect(status().isCreated());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeCreate + 1);
        ClosingTime testClosingTime = closingTimeList.get(closingTimeList.size() - 1);
        assertThat(testClosingTime.getStartAt()).isEqualTo(DEFAULT_START_AT);
        assertThat(testClosingTime.getEndAt()).isEqualTo(DEFAULT_END_AT);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ClosingTimeEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ClosingTimeEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.closingTimeCreated);
        ClosingTimeEventDTO closingTimeEventDTO = eventResult.getValue();
        assertThat(closingTimeEventDTO.getId()).isEqualTo(testClosingTime.getId());
        assertThat(closingTimeEventDTO.getOrganizationId()).isEqualTo(testClosingTime.getOrganization().getId());
        assertThat(closingTimeEventDTO.getStartAt()).isEqualTo(testClosingTime.getStartAt());
        assertThat(closingTimeEventDTO.getEndAt()).isEqualTo(testClosingTime.getEndAt());
    }

    @Test
    @Transactional
    void createClosingTimeWithExistingId() throws Exception {
        // Create the ClosingTime with an existing ID
        this.closingTime.setId(1L);
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        int databaseSizeBeforeCreate = this.closingTimeRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restClosingTimeMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(closingTimeDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStartAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.closingTimeRepository.findAll().size();
        // set the field null
        this.closingTime.setStartAt(null);

        // Create the ClosingTime, which fails.
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        this.restClosingTimeMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(closingTimeDTO)))
            .andExpect(status().isBadRequest());

        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEndAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.closingTimeRepository.findAll().size();
        // set the field null
        this.closingTime.setEndAt(null);

        // Create the ClosingTime, which fails.
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        this.restClosingTimeMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(closingTimeDTO)))
            .andExpect(status().isBadRequest());

        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllClosingTimes() throws Exception {
        // Initialize the database
        this.closingTimeRepository.saveAndFlush(this.closingTime);

        // Get all the closingTimeList
        this.restClosingTimeMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.closingTime.getId().intValue())))
            .andExpect(jsonPath("$.[*].startAt").value(hasItem(DEFAULT_START_AT.toString())))
            .andExpect(jsonPath("$.[*].endAt").value(hasItem(DEFAULT_END_AT.toString())));
    }

    @Test
    @Transactional
    void getClosingTime() throws Exception {
        // Initialize the database
        this.closingTimeRepository.saveAndFlush(this.closingTime);

        // Get the closingTime
        this.restClosingTimeMockMvc.perform(get(ENTITY_API_URL_ID, this.closingTime.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(this.closingTime.getId().intValue()))
            .andExpect(jsonPath("$.startAt").value(DEFAULT_START_AT.toString()))
            .andExpect(jsonPath("$.endAt").value(DEFAULT_END_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingClosingTime() throws Exception {
        // Get the closingTime
        this.restClosingTimeMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewClosingTime() throws Exception {
        // Initialize the database
        this.closingTimeRepository.saveAndFlush(this.closingTime);

        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();

        // Update the closingTime
        ClosingTime updatedClosingTime = this.closingTimeRepository.findById(this.closingTime.getId()).get();
        // Disconnect from session so that the updates on updatedClosingTime are not directly saved in db
        this.em.detach(updatedClosingTime);
        updatedClosingTime.startAt(UPDATED_START_AT).endAt(UPDATED_END_AT);
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(updatedClosingTime);

        this.restClosingTimeMockMvc.perform(put(ENTITY_API_URL_ID, closingTimeDTO.getId()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(closingTimeDTO))).andExpect(status().isOk());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
        ClosingTime testClosingTime = closingTimeList.get(closingTimeList.size() - 1);
        assertThat(testClosingTime.getStartAt()).isEqualTo(UPDATED_START_AT);
        assertThat(testClosingTime.getEndAt()).isEqualTo(UPDATED_END_AT);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ClosingTimeEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ClosingTimeEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.closingTimeCreated);
        ClosingTimeEventDTO closingTimeEventDTO = eventResult.getValue();
        assertThat(closingTimeEventDTO.getId()).isEqualTo(testClosingTime.getId());
        assertThat(closingTimeEventDTO.getOrganizationId()).isEqualTo(testClosingTime.getOrganization().getId());
        assertThat(closingTimeEventDTO.getStartAt()).isEqualTo(testClosingTime.getStartAt());
        assertThat(closingTimeEventDTO.getEndAt()).isEqualTo(testClosingTime.getEndAt());
    }

    @Test
    @Transactional
    void putNonExistingClosingTime() throws Exception {
        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();
        this.closingTime.setId(count.incrementAndGet());

        // Create the ClosingTime
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restClosingTimeMockMvc.perform(put(ENTITY_API_URL_ID, closingTimeDTO.getId()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(closingTimeDTO))).andExpect(status().isBadRequest());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchClosingTime() throws Exception {
        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();
        this.closingTime.setId(count.incrementAndGet());

        // Create the ClosingTime
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restClosingTimeMockMvc.perform(put(ENTITY_API_URL_ID, count.incrementAndGet()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(closingTimeDTO))).andExpect(status().isBadRequest());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamClosingTime() throws Exception {
        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();
        this.closingTime.setId(count.incrementAndGet());

        // Create the ClosingTime
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restClosingTimeMockMvc.perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(closingTimeDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateClosingTimeWithPatch() throws Exception {
        // Initialize the database
        this.closingTimeRepository.saveAndFlush(this.closingTime);

        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();

        // Update the closingTime using partial update
        ClosingTime partialUpdatedClosingTime = new ClosingTime();
        partialUpdatedClosingTime.setId(this.closingTime.getId());

        this.restClosingTimeMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedClosingTime.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(partialUpdatedClosingTime))).andExpect(status().isOk());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
        ClosingTime testClosingTime = closingTimeList.get(closingTimeList.size() - 1);
        assertThat(testClosingTime.getStartAt()).isEqualTo(DEFAULT_START_AT);
        assertThat(testClosingTime.getEndAt()).isEqualTo(DEFAULT_END_AT);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ClosingTimeEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ClosingTimeEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.closingTimeCreated);
        ClosingTimeEventDTO closingTimeEventDTO = eventResult.getValue();
        assertThat(closingTimeEventDTO.getId()).isEqualTo(testClosingTime.getId());
        assertThat(closingTimeEventDTO.getOrganizationId()).isEqualTo(testClosingTime.getOrganization().getId());
        assertThat(closingTimeEventDTO.getStartAt()).isEqualTo(testClosingTime.getStartAt());
        assertThat(closingTimeEventDTO.getEndAt()).isEqualTo(testClosingTime.getEndAt());
    }

    @Test
    @Transactional
    void fullUpdateClosingTimeWithPatch() throws Exception {
        // Initialize the database
        this.closingTimeRepository.saveAndFlush(this.closingTime);

        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();

        // Update the closingTime using partial update
        ClosingTime partialUpdatedClosingTime = new ClosingTime();
        partialUpdatedClosingTime.setId(this.closingTime.getId());

        partialUpdatedClosingTime.startAt(UPDATED_START_AT).endAt(UPDATED_END_AT);

        this.restClosingTimeMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedClosingTime.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(partialUpdatedClosingTime))).andExpect(status().isOk());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
        ClosingTime testClosingTime = closingTimeList.get(closingTimeList.size() - 1);
        assertThat(testClosingTime.getStartAt()).isEqualTo(UPDATED_START_AT);
        assertThat(testClosingTime.getEndAt()).isEqualTo(UPDATED_END_AT);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ClosingTimeEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ClosingTimeEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.closingTimeCreated);
        ClosingTimeEventDTO closingTimeEventDTO = eventResult.getValue();
        assertThat(closingTimeEventDTO.getId()).isEqualTo(testClosingTime.getId());
        assertThat(closingTimeEventDTO.getOrganizationId()).isEqualTo(testClosingTime.getOrganization().getId());
        assertThat(closingTimeEventDTO.getStartAt()).isEqualTo(testClosingTime.getStartAt());
        assertThat(closingTimeEventDTO.getEndAt()).isEqualTo(testClosingTime.getEndAt());
    }

    @Test
    @Transactional
    void patchNonExistingClosingTime() throws Exception {
        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();
        this.closingTime.setId(count.incrementAndGet());

        // Create the ClosingTime
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restClosingTimeMockMvc.perform(patch(ENTITY_API_URL_ID, closingTimeDTO.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(closingTimeDTO))).andExpect(status().isBadRequest());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchClosingTime() throws Exception {
        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();
        this.closingTime.setId(count.incrementAndGet());

        // Create the ClosingTime
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restClosingTimeMockMvc.perform(patch(ENTITY_API_URL_ID, count.incrementAndGet()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(closingTimeDTO))).andExpect(status().isBadRequest());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamClosingTime() throws Exception {
        int databaseSizeBeforeUpdate = this.closingTimeRepository.findAll().size();
        this.closingTime.setId(count.incrementAndGet());

        // Create the ClosingTime
        ClosingTimeDTO closingTimeDTO = this.closingTimeMapper.toDto(this.closingTime);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restClosingTimeMockMvc.perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(closingTimeDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ClosingTime in the database
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteClosingTime() throws Exception {
        // Initialize the database
        this.closingTimeRepository.saveAndFlush(this.closingTime);

        int databaseSizeBeforeDelete = this.closingTimeRepository.findAll().size();

        // Delete the closingTime
        this.restClosingTimeMockMvc.perform(delete(ENTITY_API_URL_ID, this.closingTime.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<ClosingTime> closingTimeList = this.closingTimeRepository.findAll();
        assertThat(closingTimeList).hasSize(databaseSizeBeforeDelete - 1);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<ClosingTimeEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<ClosingTimeEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.closingTimeDeleted);
        ClosingTimeEventDTO closingTimeEventDTO = eventResult.getValue();
        assertThat(closingTimeEventDTO.getId()).isEqualTo(this.closingTime.getId());
        assertThat(closingTimeEventDTO.getOrganizationId()).isEqualTo(this.closingTime.getOrganization().getId());
        assertThat(closingTimeEventDTO.getStartAt()).isEqualTo(this.closingTime.getStartAt());
        assertThat(closingTimeEventDTO.getEndAt()).isEqualTo(this.closingTime.getEndAt());
    }
}
