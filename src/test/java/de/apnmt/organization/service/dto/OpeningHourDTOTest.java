package de.apnmt.organization.service.dto;

import de.apnmt.organization.common.service.dto.OpeningHourDTO;
import de.apnmt.organization.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpeningHourDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OpeningHourDTO.class);
        OpeningHourDTO openingHourDTO1 = new OpeningHourDTO();
        openingHourDTO1.setId(1L);
        OpeningHourDTO openingHourDTO2 = new OpeningHourDTO();
        assertThat(openingHourDTO1).isNotEqualTo(openingHourDTO2);
        openingHourDTO2.setId(openingHourDTO1.getId());
        assertThat(openingHourDTO1).isEqualTo(openingHourDTO2);
        openingHourDTO2.setId(2L);
        assertThat(openingHourDTO1).isNotEqualTo(openingHourDTO2);
        openingHourDTO1.setId(null);
        assertThat(openingHourDTO1).isNotEqualTo(openingHourDTO2);
    }
}
