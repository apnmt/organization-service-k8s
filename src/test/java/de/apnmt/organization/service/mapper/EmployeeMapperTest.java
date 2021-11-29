package de.apnmt.organization.service.mapper;

import de.apnmt.organization.common.service.mapper.EmployeeMapper;
import de.apnmt.organization.common.service.mapper.EmployeeMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class EmployeeMapperTest {

    private EmployeeMapper employeeMapper;

    @BeforeEach
    public void setUp() {
        this.employeeMapper = new EmployeeMapperImpl();
    }
}
