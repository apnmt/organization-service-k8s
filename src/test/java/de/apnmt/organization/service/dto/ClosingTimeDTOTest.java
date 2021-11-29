package de.apnmt.organization.service.dto;

import de.apnmt.organization.common.service.dto.ClosingTimeDTO;
import de.apnmt.organization.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingTimeDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ClosingTimeDTO.class);
        ClosingTimeDTO closingTimeDTO1 = new ClosingTimeDTO();
        closingTimeDTO1.setId(1L);
        ClosingTimeDTO closingTimeDTO2 = new ClosingTimeDTO();
        assertThat(closingTimeDTO1).isNotEqualTo(closingTimeDTO2);
        closingTimeDTO2.setId(closingTimeDTO1.getId());
        assertThat(closingTimeDTO1).isEqualTo(closingTimeDTO2);
        closingTimeDTO2.setId(2L);
        assertThat(closingTimeDTO1).isNotEqualTo(closingTimeDTO2);
        closingTimeDTO1.setId(null);
        assertThat(closingTimeDTO1).isNotEqualTo(closingTimeDTO2);
    }
}
