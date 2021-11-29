package de.apnmt.organization.service.mapper;

import de.apnmt.organization.common.service.mapper.OrganizationMapper;
import de.apnmt.organization.common.service.mapper.OrganizationMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class OrganizationMapperTest {

    private OrganizationMapper organizationMapper;

    @BeforeEach
    public void setUp() {
        this.organizationMapper = new OrganizationMapperImpl();
    }
}
