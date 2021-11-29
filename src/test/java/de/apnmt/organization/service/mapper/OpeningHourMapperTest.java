package de.apnmt.organization.service.mapper;

import de.apnmt.organization.common.service.mapper.OpeningHourMapper;
import de.apnmt.organization.common.service.mapper.OpeningHourMapperImpl;
import org.junit.jupiter.api.BeforeEach;

class OpeningHourMapperTest {

    private OpeningHourMapper openingHourMapper;

    @BeforeEach
    public void setUp() {
        this.openingHourMapper = new OpeningHourMapperImpl();
    }
}
