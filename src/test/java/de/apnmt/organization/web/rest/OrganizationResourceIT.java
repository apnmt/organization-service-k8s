package de.apnmt.organization.web.rest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import de.apnmt.organization.IntegrationTest;
import de.apnmt.organization.common.domain.Addresse;
import de.apnmt.organization.common.domain.Organization;
import de.apnmt.organization.common.repository.OrganizationRepository;
import de.apnmt.organization.common.service.dto.OrganizationDTO;
import de.apnmt.organization.common.service.mapper.OrganizationMapper;
import de.apnmt.organization.common.web.rest.OrganizationResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
 * Integration tests for the {@link OrganizationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
class OrganizationResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_MAIL = "AAAAAAAAAA";
    private static final String UPDATED_MAIL = "BBBBBBBBBB";

    private static final String DEFAULT_PHONE = "AAAAAAAAAA";
    private static final String UPDATED_PHONE = "BBBBBBBBBB";

    private static final Long DEFAULT_OWNER_ID = 1L;
    private static final Long UPDATED_OWNER_ID = 2L;

    private static final Boolean DEFAULT_ACTIVE = true;
    private static final Boolean UPDATED_ACTIVE = false;

    private static final String ENTITY_API_URL = "/api/organizations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restOrganizationMockMvc;

    private Organization organization;

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Organization createEntity(EntityManager em) {
        Organization organization = new Organization().name(DEFAULT_NAME)
            .mail(DEFAULT_MAIL)
            .phone(DEFAULT_PHONE)
            .ownerId(DEFAULT_OWNER_ID)
            .active(DEFAULT_ACTIVE)
            .addresse(new Addresse().line1("Teststraße 1")
                .city("Test")
                .country("Teststadt")
                .postalCode("12345"));
        return organization;
    }

    @BeforeEach
    public void initTest() {
        this.organization = createEntity(this.em);
    }

    @Test
    @Transactional
    void createOrganization() throws Exception {
        int databaseSizeBeforeCreate = this.organizationRepository.findAll()
            .size();
        // Create the Organization
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);
        this.restOrganizationMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isCreated());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeCreate + 1);
        Organization testOrganization = organizationList.get(organizationList.size() - 1);
        assertThat(testOrganization.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testOrganization.getMail()).isEqualTo(DEFAULT_MAIL);
        assertThat(testOrganization.getPhone()).isEqualTo(DEFAULT_PHONE);
        assertThat(testOrganization.getOwnerId()).isEqualTo(DEFAULT_OWNER_ID);
        assertThat(testOrganization.getActive()).isEqualTo(false);
    }

    @Test
    @Transactional
    void createOrganizationWithExistingId() throws Exception {
        // Create the Organization with an existing ID
        this.organization.setId(1L);
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        int databaseSizeBeforeCreate = this.organizationRepository.findAll()
            .size();

        // An entity with an existing ID cannot be created, so this API call must fail
        this.restOrganizationMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.organizationRepository.findAll()
            .size();
        // set the field null
        this.organization.setName(null);

        // Create the Organization, which fails.
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        this.restOrganizationMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMailIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.organizationRepository.findAll()
            .size();
        // set the field null
        this.organization.setMail(null);

        // Create the Organization, which fails.
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        this.restOrganizationMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPhoneIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.organizationRepository.findAll()
            .size();
        // set the field null
        this.organization.setPhone(null);

        // Create the Organization, which fails.
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        this.restOrganizationMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOwnerIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.organizationRepository.findAll()
            .size();
        // set the field null
        this.organization.setOwnerId(null);

        // Create the Organization, which fails.
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        this.restOrganizationMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        int databaseSizeBeforeTest = this.organizationRepository.findAll()
            .size();
        // set the field null
        this.organization.setActive(null);

        // Create the Organization, which fails.
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        this.restOrganizationMockMvc.perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllOrganizations() throws Exception {
        // Initialize the database
        this.organization.setActive(true);
        this.organizationRepository.saveAndFlush(this.organization);

        // Get all the organizationList
        this.restOrganizationMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(this.organization.getId()
                .intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].mail").value(hasItem(DEFAULT_MAIL)))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE)))
            .andExpect(jsonPath("$.[*].ownerId").value(hasItem(DEFAULT_OWNER_ID.intValue())))
            .andExpect(jsonPath("$.[*].active").value(hasItem(true)));
    }

    @Test
    @Transactional
    void getAllOrganizationsEmpty() throws Exception {
        // Initialize the database
        this.organizationRepository.deleteAll();
        this.organization.setActive(false);
        this.organizationRepository.saveAndFlush(this.organization);

        // Get all the organizationList
        this.restOrganizationMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*]").isEmpty());
    }

    @Test
    @Transactional
    void getOrganization() throws Exception {
        // Initialize the database
        this.organizationRepository.saveAndFlush(this.organization);

        // Get the organization
        this.restOrganizationMockMvc.perform(get(ENTITY_API_URL_ID, this.organization.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(this.organization.getId()
                .intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.mail").value(DEFAULT_MAIL))
            .andExpect(jsonPath("$.phone").value(DEFAULT_PHONE))
            .andExpect(jsonPath("$.ownerId").value(DEFAULT_OWNER_ID.toString()))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE.booleanValue()));
    }

    @Test
    @Transactional
    void getNonExistingOrganization() throws Exception {
        // Get the organization
        this.restOrganizationMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewOrganization() throws Exception {
        // Initialize the database
        this.organizationRepository.saveAndFlush(this.organization);

        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();

        // Update the organization
        Organization updatedOrganization = this.organizationRepository.findById(this.organization.getId())
            .get();
        // Disconnect from session so that the updates on updatedOrganization are not directly saved in db
        this.em.detach(updatedOrganization);
        updatedOrganization.name(UPDATED_NAME)
            .mail(UPDATED_MAIL)
            .phone(UPDATED_PHONE)
            .ownerId(UPDATED_OWNER_ID)
            .active(UPDATED_ACTIVE);
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(updatedOrganization);

        this.restOrganizationMockMvc.perform(put(ENTITY_API_URL_ID, organizationDTO.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isOk());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
        Organization testOrganization = organizationList.get(organizationList.size() - 1);
        assertThat(testOrganization.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testOrganization.getMail()).isEqualTo(UPDATED_MAIL);
        assertThat(testOrganization.getPhone()).isEqualTo(UPDATED_PHONE);
        assertThat(testOrganization.getOwnerId()).isEqualTo(UPDATED_OWNER_ID);
        assertThat(testOrganization.getActive()).isEqualTo(UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void putNonExistingOrganization() throws Exception {
        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();
        this.organization.setId(count.incrementAndGet());

        // Create the Organization
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restOrganizationMockMvc.perform(put(ENTITY_API_URL_ID, organizationDTO.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchOrganization() throws Exception {
        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();
        this.organization.setId(count.incrementAndGet());

        // Create the Organization
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restOrganizationMockMvc.perform(put(ENTITY_API_URL_ID, count.incrementAndGet()).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamOrganization() throws Exception {
        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();
        this.organization.setId(count.incrementAndGet());

        // Create the Organization
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restOrganizationMockMvc.perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateOrganizationWithPatch() throws Exception {
        // Initialize the database
        this.organizationRepository.saveAndFlush(this.organization);

        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();

        // Update the organization using partial update
        Organization partialUpdatedOrganization = new Organization();
        partialUpdatedOrganization.setId(this.organization.getId());

        partialUpdatedOrganization.name(UPDATED_NAME)
            .ownerId(UPDATED_OWNER_ID)
            .active(UPDATED_ACTIVE);

        this.restOrganizationMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedOrganization.getId()).contentType("application/merge-patch+json")
                .content(TestUtil.convertObjectToJsonBytes(partialUpdatedOrganization)))
            .andExpect(status().isOk());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
        Organization testOrganization = organizationList.get(organizationList.size() - 1);
        assertThat(testOrganization.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testOrganization.getMail()).isEqualTo(DEFAULT_MAIL);
        assertThat(testOrganization.getPhone()).isEqualTo(DEFAULT_PHONE);
        assertThat(testOrganization.getOwnerId()).isEqualTo(UPDATED_OWNER_ID);
        assertThat(testOrganization.getActive()).isEqualTo(UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void fullUpdateOrganizationWithPatch() throws Exception {
        // Initialize the database
        this.organizationRepository.saveAndFlush(this.organization);

        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();

        // Update the organization using partial update
        Organization partialUpdatedOrganization = new Organization();
        partialUpdatedOrganization.setId(this.organization.getId());

        partialUpdatedOrganization.name(UPDATED_NAME)
            .mail(UPDATED_MAIL)
            .phone(UPDATED_PHONE)
            .ownerId(UPDATED_OWNER_ID)
            .active(UPDATED_ACTIVE);

        this.restOrganizationMockMvc.perform(patch(ENTITY_API_URL_ID, partialUpdatedOrganization.getId()).contentType("application/merge-patch+json")
                .content(TestUtil.convertObjectToJsonBytes(partialUpdatedOrganization)))
            .andExpect(status().isOk());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
        Organization testOrganization = organizationList.get(organizationList.size() - 1);
        assertThat(testOrganization.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testOrganization.getMail()).isEqualTo(UPDATED_MAIL);
        assertThat(testOrganization.getPhone()).isEqualTo(UPDATED_PHONE);
        assertThat(testOrganization.getOwnerId()).isEqualTo(UPDATED_OWNER_ID);
        assertThat(testOrganization.getActive()).isEqualTo(UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void patchNonExistingOrganization() throws Exception {
        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();
        this.organization.setId(count.incrementAndGet());

        // Create the Organization
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        this.restOrganizationMockMvc.perform(patch(ENTITY_API_URL_ID, organizationDTO.getId()).contentType("application/merge-patch+json")
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchOrganization() throws Exception {
        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();
        this.organization.setId(count.incrementAndGet());

        // Create the Organization
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restOrganizationMockMvc.perform(patch(ENTITY_API_URL_ID, count.incrementAndGet()).contentType("application/merge-patch+json")
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamOrganization() throws Exception {
        int databaseSizeBeforeUpdate = this.organizationRepository.findAll()
            .size();
        this.organization.setId(count.incrementAndGet());

        // Create the Organization
        OrganizationDTO organizationDTO = this.organizationMapper.toDto(this.organization);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        this.restOrganizationMockMvc.perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json")
                .content(TestUtil.convertObjectToJsonBytes(organizationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Organization in the database
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteOrganization() throws Exception {
        // Initialize the database
        this.organizationRepository.saveAndFlush(this.organization);

        int databaseSizeBeforeDelete = this.organizationRepository.findAll()
            .size();

        // Delete the organization
        this.restOrganizationMockMvc.perform(delete(ENTITY_API_URL_ID, this.organization.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Organization> organizationList = this.organizationRepository.findAll();
        assertThat(organizationList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
