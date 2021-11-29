package de.apnmt.organization.domain;

import de.apnmt.organization.common.domain.ClosingTime;
import de.apnmt.organization.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingTimeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ClosingTime.class);
        ClosingTime closingTime1 = new ClosingTime();
        closingTime1.setId(1L);
        ClosingTime closingTime2 = new ClosingTime();
        closingTime2.setId(closingTime1.getId());
        assertThat(closingTime1).isEqualTo(closingTime2);
        closingTime2.setId(2L);
        assertThat(closingTime1).isNotEqualTo(closingTime2);
        closingTime1.setId(null);
        assertThat(closingTime1).isNotEqualTo(closingTime2);
    }
}
