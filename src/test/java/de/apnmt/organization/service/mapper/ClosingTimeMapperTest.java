package de.apnmt.organization.service.mapper;

import de.apnmt.organization.common.service.mapper.ClosingTimeMapper;
import de.apnmt.organization.common.service.mapper.ClosingTimeMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class ClosingTimeMapperTest {

    private ClosingTimeMapper closingTimeMapper;

    @BeforeEach
    public void setUp() {
        this.closingTimeMapper = new ClosingTimeMapperImpl();
    }
}
