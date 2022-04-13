package de.apnmt.organization.web.rest;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import com.fasterxml.jackson.core.type.TypeReference;
import de.apnmt.common.TopicConstants;
import de.apnmt.common.enumeration.Day;
import de.apnmt.common.event.ApnmtEvent;
import de.apnmt.common.event.ApnmtEventType;
import de.apnmt.common.event.value.OpeningHourEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import de.apnmt.organization.IntegrationTest;
import de.apnmt.organization.common.domain.ClosingTime;
import de.apnmt.organization.common.domain.OpeningHour;
import de.apnmt.organization.common.domain.Organization;
import de.apnmt.organization.common.repository.OpeningHourRepository;
import de.apnmt.organization.common.repository.OrganizationRepository;
import de.apnmt.organization.common.service.dto.OpeningHourDTO;
import de.apnmt.organization.common.service.mapper.OpeningHourMapper;
import de.apnmt.organization.common.web.rest.OpeningHourResource;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link OpeningHourResource} REST controller.
 */
@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.OPENING_HOUR_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
class OpeningHourResourceIT extends AbstractEventSenderIT {

    private static final Day DEFAULT_DAY = Day.Monday;
    private static final Day UPDATED_DAY = Day.Tuesday;

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(0, 0, 11, 0);
    private static final LocalTime UPDATED_START_TIME = LocalTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(23, 0, 11, 0);
    private static final LocalTime UPDATED_END_TIME = LocalTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/opening-hours";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private OpeningHourRepository openingHourRepository;

    @Autowired
    private OpeningHourMapper openingHourMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOpeningHourMockMvc;

    private OpeningHour openingHour;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OpeningHour createEntity(EntityManager em) {
        OpeningHour openingHour = new OpeningHour().day(DEFAULT_DAY).startTime(DEFAULT_START_TIME).endTime(DEFAULT_END_TIME);
        return openingHour;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OpeningHour createUpdatedEntity(EntityManager em) {
        OpeningHour openingHour = new OpeningHour().day(UPDATED_DAY).startTime(UPDATED_START_TIME).endTime(UPDATED_END_TIME);
        return openingHour;
    }

    @Override
    public String getTopic() {
        return TopicConstants.OPENING_HOUR_CHANGED_TOPIC;
    }

    @BeforeEach
    public void initTest() {
        Organization organization = OrganizationResourceIT.createEntity(this.em);
        this.organizationRepository.saveAndFlush(organization);
        this.openingHour = createEntity(this.em);
        this.openingHour.setOrganization(organization);
    }

    @AfterEach
    public void shutDown() throws InterruptedException {
        // All topics should be empty now
        assertThat(this.records.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @Transactional
    void createOpeningHour() throws Exception {
        int databaseSizeBeforeCreate = this.openingHourRepository.findAll().size();
        // Create the OpeningHour
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);
        this.restOpeningHourMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(openingHourDTO)))
            .andExpect(status().isCreated());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeCreate + 1);
        OpeningHour testOpeningHour = openingHourList.get(openingHourList.size() - 1);
        assertThat(testOpeningHour.getDay()).isEqualTo(DEFAULT_DAY);
        assertThat(testOpeningHour.getStartTime()).isEqualTo(DEFAULT_START_TIME);
        assertThat(testOpeningHour.getEndTime()).isEqualTo(DEFAULT_END_TIME);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OpeningHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OpeningHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.openingHourCreated);
        OpeningHourEventDTO openingHourEventDTO = eventResult.getValue();
        assertThat(openingHourEventDTO.getId()).isEqualTo(testOpeningHour.getId());
        assertThat(openingHourEventDTO.getOrganizationId()).isEqualTo(testOpeningHour.getOrganization().getId());
        assertThat(openingHourEventDTO.getStartTime()).isEqualTo(testOpeningHour.getStartTime());
        assertThat(openingHourEventDTO.getEndTime()).isEqualTo(testOpeningHour.getEndTime());
    }

    @Test
    @Transactional
    void createOpeningHourWithExistingId() throws Exception {
        // Create the OpeningHour with an existing ID
        this.openingHour.setId(1L);
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        int databaseSizeBeforeCreate = this.openingHourRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restOpeningHourMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(openingHourDTO)))
            .andExpect(status().isBadRequest());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStartTimeIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.openingHourRepository.findAll().size();
        // set the field null
        this.openingHour.setStartTime(null);

        // Create the OpeningHour, which fails.
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        this.restOpeningHourMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(openingHourDTO)))
            .andExpect(status().isBadRequest());

        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEndTimeIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.openingHourRepository.findAll().size();
        // set the field null
        this.openingHour.setEndTime(null);

        // Create the OpeningHour, which fails.
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        this.restOpeningHourMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(openingHourDTO)))
            .andExpect(status().isBadRequest());

        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOpeningHours() throws Exception {
        // Initialize the database
        this.openingHourRepository.saveAndFlush(this.openingHour);

        // Get all the openingHourList
        this.restOpeningHourMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.openingHour.getId().intValue())))
            .andExpect(jsonPath("$.[*].day").value(hasItem(DEFAULT_DAY.toString())))
            .andExpect(jsonPath("$.[*].startTime").value(hasItem(DEFAULT_START_TIME.toString())))
            .andExpect(jsonPath("$.[*].endTime").value(hasItem(DEFAULT_END_TIME.toString())));
    }

    @Test
    @Transactional
    void getAllOpeningHourForOrganization() throws Exception {
        // Initialize the database
        Organization organization = OrganizationResourceIT.createEntity(em);
        this.organizationRepository.saveAndFlush(organization);
        this.openingHour.organization(organization);
        this.openingHourRepository.saveAndFlush(this.openingHour);

        // Get all the employeeList
        this.restOpeningHourMockMvc
            .perform(get(ENTITY_API_URL + "/organization/" + openingHour.getOrganization().getId().intValue() + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*]").value(hasSize(1)))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.openingHour.getId().intValue())))
            .andExpect(jsonPath("$.[*].day").value(hasItem(DEFAULT_DAY.toString())))
            .andExpect(jsonPath("$.[*].startTime").value(hasItem(DEFAULT_START_TIME.toString())))
            .andExpect(jsonPath("$.[*].endTime").value(hasItem(DEFAULT_END_TIME.toString())));
    }

    @Test
    @Transactional
    void getOpeningHour() throws Exception {
        // Initialize the database
        this.openingHourRepository.saveAndFlush(this.openingHour);

        // Get the openingHour
        this.restOpeningHourMockMvc.perform(get(ENTITY_API_URL_ID, this.openingHour.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(this.openingHour.getId().intValue()))
            .andExpect(jsonPath("$.day").value(DEFAULT_DAY.toString()))
            .andExpect(jsonPath("$.startTime").value(DEFAULT_START_TIME.toString()))
            .andExpect(jsonPath("$.endTime").value(DEFAULT_END_TIME.toString()));
    }

    @Test
    @Transactional
    void getNonExistingOpeningHour() throws Exception {
        // Get the openingHour
        this.restOpeningHourMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewOpeningHour() throws Exception {
        // Initialize the database
        this.openingHourRepository.saveAndFlush(this.openingHour);

        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();

        // Update the openingHour
        OpeningHour updatedOpeningHour = this.openingHourRepository.findById(this.openingHour.getId()).get();
        // Disconnect from session so that the updates on updatedOpeningHour are not directly saved in db
        this.em.detach(updatedOpeningHour);
        updatedOpeningHour.day(UPDATED_DAY).startTime(UPDATED_START_TIME).endTime(UPDATED_END_TIME);
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(updatedOpeningHour);

        this.restOpeningHourMockMvc.perform(put(ENTITY_API_URL_ID, openingHourDTO.getId()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(openingHourDTO))).andExpect(status().isOk());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
        OpeningHour testOpeningHour = openingHourList.get(openingHourList.size() - 1);
        assertThat(testOpeningHour.getDay()).isEqualTo(UPDATED_DAY);
        assertThat(testOpeningHour.getStartTime()).isEqualTo(UPDATED_START_TIME);
        assertThat(testOpeningHour.getEndTime()).isEqualTo(UPDATED_END_TIME);


        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OpeningHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OpeningHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.openingHourCreated);
        OpeningHourEventDTO openingHourEventDTO = eventResult.getValue();
        assertThat(openingHourEventDTO.getId()).isEqualTo(testOpeningHour.getId());
        assertThat(openingHourEventDTO.getOrganizationId()).isEqualTo(testOpeningHour.getOrganization().getId());
        assertThat(openingHourEventDTO.getStartTime()).isEqualTo(testOpeningHour.getStartTime());
        assertThat(openingHourEventDTO.getEndTime()).isEqualTo(testOpeningHour.getEndTime());
    }

    @Test
    @Transactional
    void putNonExistingOpeningHour() throws Exception {
        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();
        this.openingHour.setId(count.incrementAndGet());

        // Create the OpeningHour
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restOpeningHourMockMvc.perform(put(ENTITY_API_URL_ID, openingHourDTO.getId()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(openingHourDTO))).andExpect(status().isBadRequest());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOpeningHour() throws Exception {
        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();
        this.openingHour.setId(count.incrementAndGet());

        // Create the OpeningHour
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restOpeningHourMockMvc.perform(put(ENTITY_API_URL_ID, count.incrementAndGet()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(openingHourDTO))).andExpect(status().isBadRequest());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOpeningHour() throws Exception {
        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();
        this.openingHour.setId(count.incrementAndGet());

        // Create the OpeningHour
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restOpeningHourMockMvc.perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(openingHourDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOpeningHourWithPatch() throws Exception {
        // Initialize the database
        this.openingHourRepository.saveAndFlush(this.openingHour);

        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();

        // Update the openingHour using partial update
        OpeningHour partialUpdatedOpeningHour = new OpeningHour();
        partialUpdatedOpeningHour.setId(this.openingHour.getId());

        partialUpdatedOpeningHour.day(UPDATED_DAY).endTime(UPDATED_END_TIME);

        this.restOpeningHourMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedOpeningHour.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(partialUpdatedOpeningHour))).andExpect(status().isOk());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
        OpeningHour testOpeningHour = openingHourList.get(openingHourList.size() - 1);
        assertThat(testOpeningHour.getDay()).isEqualTo(UPDATED_DAY);
        assertThat(testOpeningHour.getStartTime()).isEqualTo(DEFAULT_START_TIME);
        assertThat(testOpeningHour.getEndTime()).isEqualTo(UPDATED_END_TIME);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OpeningHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OpeningHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.openingHourCreated);
        OpeningHourEventDTO openingHourEventDTO = eventResult.getValue();
        assertThat(openingHourEventDTO.getId()).isEqualTo(testOpeningHour.getId());
        assertThat(openingHourEventDTO.getOrganizationId()).isEqualTo(testOpeningHour.getOrganization().getId());
        assertThat(openingHourEventDTO.getStartTime()).isEqualTo(testOpeningHour.getStartTime());
        assertThat(openingHourEventDTO.getEndTime()).isEqualTo(testOpeningHour.getEndTime());
    }

    @Test
    @Transactional
    void fullUpdateOpeningHourWithPatch() throws Exception {
        // Initialize the database
        this.openingHourRepository.saveAndFlush(this.openingHour);

        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();

        // Update the openingHour using partial update
        OpeningHour partialUpdatedOpeningHour = new OpeningHour();
        partialUpdatedOpeningHour.setId(this.openingHour.getId());

        partialUpdatedOpeningHour.day(UPDATED_DAY).startTime(UPDATED_START_TIME).endTime(UPDATED_END_TIME);

        this.restOpeningHourMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedOpeningHour.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(partialUpdatedOpeningHour))).andExpect(status().isOk());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
        OpeningHour testOpeningHour = openingHourList.get(openingHourList.size() - 1);
        assertThat(testOpeningHour.getDay()).isEqualTo(UPDATED_DAY);
        assertThat(testOpeningHour.getStartTime()).isEqualTo(UPDATED_START_TIME);
        assertThat(testOpeningHour.getEndTime()).isEqualTo(UPDATED_END_TIME);


        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OpeningHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OpeningHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.openingHourCreated);
        OpeningHourEventDTO openingHourEventDTO = eventResult.getValue();
        assertThat(openingHourEventDTO.getId()).isEqualTo(testOpeningHour.getId());
        assertThat(openingHourEventDTO.getOrganizationId()).isEqualTo(testOpeningHour.getOrganization().getId());
        assertThat(openingHourEventDTO.getStartTime()).isEqualTo(testOpeningHour.getStartTime());
        assertThat(openingHourEventDTO.getEndTime()).isEqualTo(testOpeningHour.getEndTime());
    }

    @Test
    @Transactional
    void patchNonExistingOpeningHour() throws Exception {
        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();
        this.openingHour.setId(count.incrementAndGet());

        // Create the OpeningHour
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restOpeningHourMockMvc.perform(patch(ENTITY_API_URL_ID, openingHourDTO.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(openingHourDTO))).andExpect(status().isBadRequest());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOpeningHour() throws Exception {
        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();
        this.openingHour.setId(count.incrementAndGet());

        // Create the OpeningHour
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restOpeningHourMockMvc.perform(patch(ENTITY_API_URL_ID, count.incrementAndGet()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(openingHourDTO))).andExpect(status().isBadRequest());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOpeningHour() throws Exception {
        int databaseSizeBeforeUpdate = this.openingHourRepository.findAll().size();
        this.openingHour.setId(count.incrementAndGet());

        // Create the OpeningHour
        OpeningHourDTO openingHourDTO = this.openingHourMapper.toDto(this.openingHour);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restOpeningHourMockMvc.perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(openingHourDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the OpeningHour in the database
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOpeningHour() throws Exception {
        // Initialize the database
        this.openingHourRepository.saveAndFlush(this.openingHour);

        int databaseSizeBeforeDelete = this.openingHourRepository.findAll().size();

        // Delete the openingHour
        this.restOpeningHourMockMvc.perform(delete(ENTITY_API_URL_ID, this.openingHour.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<OpeningHour> openingHourList = this.openingHourRepository.findAll();
        assertThat(openingHourList).hasSize(databaseSizeBeforeDelete - 1);


        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<OpeningHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<OpeningHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.openingHourDeleted);
        OpeningHourEventDTO openingHourEventDTO = eventResult.getValue();
        assertThat(openingHourEventDTO.getId()).isEqualTo(this.openingHour.getId());
        assertThat(openingHourEventDTO.getOrganizationId()).isEqualTo(this.openingHour.getOrganization().getId());
        assertThat(openingHourEventDTO.getStartTime()).isEqualTo(this.openingHour.getStartTime());
        assertThat(openingHourEventDTO.getEndTime()).isEqualTo(this.openingHour.getEndTime());
    }

    @Test
    @Transactional
    void deleteAllOpeningHours() throws Exception {
        // Initialize the database
        this.openingHourRepository.saveAndFlush(this.openingHour);

        int databaseSizeBeforeDelete = this.openingHourRepository.findAll().size();

        // Delete the appointment
        this.restOpeningHourMockMvc.perform(delete(ENTITY_API_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains no more item
        List<OpeningHour> list = this.openingHourRepository.findAll();
        assertThat(list).hasSize(databaseSizeBeforeDelete - 1);
    }
}
