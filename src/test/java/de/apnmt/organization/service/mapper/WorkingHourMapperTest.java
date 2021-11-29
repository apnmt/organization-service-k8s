package de.apnmt.organization.service.mapper;

import de.apnmt.organization.common.service.mapper.WorkingHourMapper;
import de.apnmt.organization.common.service.mapper.WorkingHourMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class WorkingHourMapperTest {

    private WorkingHourMapper workingHourMapper;

    @BeforeEach
    public void setUp() {
        this.workingHourMapper = new WorkingHourMapperImpl();
    }
}
