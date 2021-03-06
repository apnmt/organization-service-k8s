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
import de.apnmt.common.event.value.WorkingHourEventDTO;
import de.apnmt.k8s.common.test.AbstractEventSenderIT;
import de.apnmt.organization.IntegrationTest;
import de.apnmt.organization.common.domain.Employee;
import de.apnmt.organization.common.domain.OpeningHour;
import de.apnmt.organization.common.domain.WorkingHour;
import de.apnmt.organization.common.repository.EmployeeRepository;
import de.apnmt.organization.common.repository.WorkingHourRepository;
import de.apnmt.organization.common.service.dto.WorkingHourDTO;
import de.apnmt.organization.common.service.mapper.WorkingHourMapper;
import de.apnmt.organization.common.web.rest.WorkingHourResource;
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
 * Integration tests for the {@link WorkingHourResource} REST controller.
 */
@EnableKafka
@EmbeddedKafka(ports = {58255}, topics = {TopicConstants.OPENING_HOUR_CHANGED_TOPIC})
@IntegrationTest
@AutoConfigureMockMvc
class WorkingHourResourceIT extends AbstractEventSenderIT {

    private static final LocalDateTime DEFAULT_START_AT = LocalDateTime.of(2021, 12, 24, 0, 0, 11, 0);
    private static final LocalDateTime UPDATED_START_AT = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final LocalDateTime DEFAULT_END_AT = LocalDateTime.of(2021, 12, 25, 0, 0, 11, 0);
    private static final LocalDateTime UPDATED_END_AT = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/working-hours";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private WorkingHourRepository workingHourRepository;

    @Autowired
    private WorkingHourMapper workingHourMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restWorkingHourMockMvc;

    private WorkingHour workingHour;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static WorkingHour createEntity(EntityManager em) {
        WorkingHour workingHour = new WorkingHour().startAt(DEFAULT_START_AT).endAt(DEFAULT_END_AT);
        return workingHour;
    }

    /**
     * Create an updated entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static WorkingHour createUpdatedEntity(EntityManager em) {
        WorkingHour workingHour = new WorkingHour().startAt(UPDATED_START_AT).endAt(UPDATED_END_AT);
        return workingHour;
    }

    @Override
    public String getTopic() {
        return TopicConstants.WORKING_HOUR_CHANGED_TOPIC;
    }

    @BeforeEach
    public void initTest() {
        Employee employee = EmployeeResourceIT.createEntity(this.em);
        this.employeeRepository.saveAndFlush(employee);
        this.workingHour = createEntity(this.em);
        this.workingHour.setEmployee(employee);
    }

    @AfterEach
    public void shutDown() throws InterruptedException {
        // All topics should be empty now
        assertThat(this.records.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @Transactional
    void createWorkingHour() throws Exception {
        int databaseSizeBeforeCreate = this.workingHourRepository.findAll().size();
        // Create the WorkingHour
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);
        this.restWorkingHourMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(workingHourDTO)))
            .andExpect(status().isCreated());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeCreate + 1);
        WorkingHour testWorkingHour = workingHourList.get(workingHourList.size() - 1);
        assertThat(testWorkingHour.getStartAt()).isEqualTo(DEFAULT_START_AT);
        assertThat(testWorkingHour.getEndAt()).isEqualTo(DEFAULT_END_AT);

        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<WorkingHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<WorkingHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.workingHourCreated);
        WorkingHourEventDTO workingHourEventDTO = eventResult.getValue();
        assertThat(workingHourEventDTO.getId()).isEqualTo(testWorkingHour.getId());
        assertThat(workingHourEventDTO.getEmployeeId()).isEqualTo(testWorkingHour.getEmployee().getId());
        assertThat(workingHourEventDTO.getStartAt()).isEqualTo(testWorkingHour.getStartAt());
        assertThat(workingHourEventDTO.getEndAt()).isEqualTo(testWorkingHour.getEndAt());
    }

    @Test
    @Transactional
    void createWorkingHourWithExistingId() throws Exception {
        // Create the WorkingHour with an existing ID
        this.workingHour.setId(1L);
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        int databaseSizeBeforeCreate = this.workingHourRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restWorkingHourMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(workingHourDTO)))
            .andExpect(status().isBadRequest());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStartAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.workingHourRepository.findAll().size();
        // set the field null
        this.workingHour.setStartAt(null);

        // Create the WorkingHour, which fails.
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        this.restWorkingHourMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(workingHourDTO)))
            .andExpect(status().isBadRequest());

        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEndAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.workingHourRepository.findAll().size();
        // set the field null
        this.workingHour.setEndAt(null);

        // Create the WorkingHour, which fails.
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        this.restWorkingHourMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(workingHourDTO)))
            .andExpect(status().isBadRequest());

        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllWorkingHours() throws Exception {
        // Initialize the database
        this.workingHourRepository.saveAndFlush(this.workingHour);

        // Get all the workingHourList
        this.restWorkingHourMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.workingHour.getId().intValue())))
            .andExpect(jsonPath("$.[*].startAt").value(hasItem(DEFAULT_START_AT.toString())))
            .andExpect(jsonPath("$.[*].endAt").value(hasItem(DEFAULT_END_AT.toString())));
    }

    @Test
    @Transactional
    void getWorkingHour() throws Exception {
        // Initialize the database
        this.workingHourRepository.saveAndFlush(this.workingHour);

        // Get the workingHour
        this.restWorkingHourMockMvc.perform(get(ENTITY_API_URL_ID, this.workingHour.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(this.workingHour.getId().intValue()))
            .andExpect(jsonPath("$.startAt").value(DEFAULT_START_AT.toString()))
            .andExpect(jsonPath("$.endAt").value(DEFAULT_END_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingWorkingHour() throws Exception {
        // Get the workingHour
        this.restWorkingHourMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewWorkingHour() throws Exception {
        // Initialize the database
        this.workingHourRepository.saveAndFlush(this.workingHour);

        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();

        // Update the workingHour
        WorkingHour updatedWorkingHour = this.workingHourRepository.findById(this.workingHour.getId()).get();
        // Disconnect from session so that the updates on updatedWorkingHour are not directly saved in db
        this.em.detach(updatedWorkingHour);
        updatedWorkingHour.startAt(UPDATED_START_AT).endAt(UPDATED_END_AT);
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(updatedWorkingHour);

        this.restWorkingHourMockMvc.perform(put(ENTITY_API_URL_ID, workingHourDTO.getId()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(workingHourDTO))).andExpect(status().isOk());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
        WorkingHour testWorkingHour = workingHourList.get(workingHourList.size() - 1);
        assertThat(testWorkingHour.getStartAt()).isEqualTo(UPDATED_START_AT);
        assertThat(testWorkingHour.getEndAt()).isEqualTo(UPDATED_END_AT);


        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<WorkingHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<WorkingHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.workingHourCreated);
        WorkingHourEventDTO workingHourEventDTO = eventResult.getValue();
        assertThat(workingHourEventDTO.getId()).isEqualTo(testWorkingHour.getId());
        assertThat(workingHourEventDTO.getEmployeeId()).isEqualTo(testWorkingHour.getEmployee().getId());
        assertThat(workingHourEventDTO.getStartAt()).isEqualTo(testWorkingHour.getStartAt());
        assertThat(workingHourEventDTO.getEndAt()).isEqualTo(testWorkingHour.getEndAt());
    }

    @Test
    @Transactional
    void putNonExistingWorkingHour() throws Exception {
        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();
        this.workingHour.setId(count.incrementAndGet());

        // Create the WorkingHour
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restWorkingHourMockMvc.perform(put(ENTITY_API_URL_ID, workingHourDTO.getId()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(workingHourDTO))).andExpect(status().isBadRequest());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchWorkingHour() throws Exception {
        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();
        this.workingHour.setId(count.incrementAndGet());

        // Create the WorkingHour
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restWorkingHourMockMvc.perform(put(ENTITY_API_URL_ID, count.incrementAndGet()).contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(workingHourDTO))).andExpect(status().isBadRequest());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamWorkingHour() throws Exception {
        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();
        this.workingHour.setId(count.incrementAndGet());

        // Create the WorkingHour
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restWorkingHourMockMvc.perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(workingHourDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateWorkingHourWithPatch() throws Exception {
        // Initialize the database
        this.workingHourRepository.saveAndFlush(this.workingHour);

        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();

        // Update the workingHour using partial update
        WorkingHour partialUpdatedWorkingHour = new WorkingHour();
        partialUpdatedWorkingHour.setId(this.workingHour.getId());

        partialUpdatedWorkingHour.endAt(UPDATED_END_AT);

        this.restWorkingHourMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedWorkingHour.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(partialUpdatedWorkingHour))).andExpect(status().isOk());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
        WorkingHour testWorkingHour = workingHourList.get(workingHourList.size() - 1);
        assertThat(testWorkingHour.getStartAt()).isEqualTo(DEFAULT_START_AT);
        assertThat(testWorkingHour.getEndAt()).isEqualTo(UPDATED_END_AT);


        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<WorkingHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<WorkingHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.workingHourCreated);
        WorkingHourEventDTO workingHourEventDTO = eventResult.getValue();
        assertThat(workingHourEventDTO.getId()).isEqualTo(testWorkingHour.getId());
        assertThat(workingHourEventDTO.getEmployeeId()).isEqualTo(testWorkingHour.getEmployee().getId());
        assertThat(workingHourEventDTO.getStartAt()).isEqualTo(testWorkingHour.getStartAt());
        assertThat(workingHourEventDTO.getEndAt()).isEqualTo(testWorkingHour.getEndAt());
    }

    @Test
    @Transactional
    void fullUpdateWorkingHourWithPatch() throws Exception {
        // Initialize the database
        this.workingHourRepository.saveAndFlush(this.workingHour);

        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();

        // Update the workingHour using partial update
        WorkingHour partialUpdatedWorkingHour = new WorkingHour();
        partialUpdatedWorkingHour.setId(this.workingHour.getId());

        partialUpdatedWorkingHour.startAt(UPDATED_START_AT).endAt(UPDATED_END_AT);

        this.restWorkingHourMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedWorkingHour.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(partialUpdatedWorkingHour))).andExpect(status().isOk());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
        WorkingHour testWorkingHour = workingHourList.get(workingHourList.size() - 1);
        assertThat(testWorkingHour.getStartAt()).isEqualTo(UPDATED_START_AT);
        assertThat(testWorkingHour.getEndAt()).isEqualTo(UPDATED_END_AT);


        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<WorkingHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<WorkingHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.workingHourCreated);
        WorkingHourEventDTO workingHourEventDTO = eventResult.getValue();
        assertThat(workingHourEventDTO.getId()).isEqualTo(testWorkingHour.getId());
        assertThat(workingHourEventDTO.getEmployeeId()).isEqualTo(testWorkingHour.getEmployee().getId());
        assertThat(workingHourEventDTO.getStartAt()).isEqualTo(testWorkingHour.getStartAt());
        assertThat(workingHourEventDTO.getEndAt()).isEqualTo(testWorkingHour.getEndAt());
    }

    @Test
    @Transactional
    void patchNonExistingWorkingHour() throws Exception {
        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();
        this.workingHour.setId(count.incrementAndGet());

        // Create the WorkingHour
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restWorkingHourMockMvc.perform(patch(ENTITY_API_URL_ID, workingHourDTO.getId()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(workingHourDTO))).andExpect(status().isBadRequest());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchWorkingHour() throws Exception {
        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();
        this.workingHour.setId(count.incrementAndGet());

        // Create the WorkingHour
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restWorkingHourMockMvc.perform(patch(ENTITY_API_URL_ID, count.incrementAndGet()).contentType("application/merge-patch+json")
            .content(TestUtil.convertObjectToJsonBytes(workingHourDTO))).andExpect(status().isBadRequest());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamWorkingHour() throws Exception {
        int databaseSizeBeforeUpdate = this.workingHourRepository.findAll().size();
        this.workingHour.setId(count.incrementAndGet());

        // Create the WorkingHour
        WorkingHourDTO workingHourDTO = this.workingHourMapper.toDto(this.workingHour);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restWorkingHourMockMvc.perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(workingHourDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the WorkingHour in the database
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteWorkingHour() throws Exception {
        // Initialize the database
        this.workingHourRepository.saveAndFlush(this.workingHour);

        int databaseSizeBeforeDelete = this.workingHourRepository.findAll().size();

        // Delete the workingHour
        this.restWorkingHourMockMvc.perform(delete(ENTITY_API_URL_ID, this.workingHour.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<WorkingHour> workingHourList = this.workingHourRepository.findAll();
        assertThat(workingHourList).hasSize(databaseSizeBeforeDelete - 1);


        ConsumerRecord<String, Object> message = this.records.poll(500, TimeUnit.MILLISECONDS);
        assertThat(message).isNotNull();
        assertThat(message.value()).isNotNull();

        TypeReference<ApnmtEvent<WorkingHourEventDTO>> eventType = new TypeReference<>() {
        };
        ApnmtEvent<WorkingHourEventDTO> eventResult = this.objectMapper.readValue(message.value().toString(), eventType);
        assertThat(eventResult.getType()).isEqualTo(ApnmtEventType.workingHourDeleted);
        WorkingHourEventDTO workingHourEventDTO = eventResult.getValue();
        assertThat(workingHourEventDTO.getId()).isEqualTo(this.workingHour.getId());
        assertThat(workingHourEventDTO.getEmployeeId()).isEqualTo(this.workingHour.getEmployee().getId());
        assertThat(workingHourEventDTO.getStartAt()).isEqualTo(this.workingHour.getStartAt());
        assertThat(workingHourEventDTO.getEndAt()).isEqualTo(this.workingHour.getEndAt());
    }

    @Test
    @Transactional
    void deleteAllWorkingHozurs() throws Exception {
        // Initialize the database
        this.workingHourRepository.saveAndFlush(this.workingHour);

        int databaseSizeBeforeDelete = this.workingHourRepository.findAll().size();

        // Delete the appointment
        this.restWorkingHourMockMvc.perform(delete(ENTITY_API_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains no more item
        List<WorkingHour> list = this.workingHourRepository.findAll();
        assertThat(list).hasSize(databaseSizeBeforeDelete - 1);
    }
}
