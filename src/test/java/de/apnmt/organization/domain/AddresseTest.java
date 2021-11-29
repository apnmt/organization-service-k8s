package de.apnmt.organization.domain;

import de.apnmt.organization.common.domain.Addresse;
import de.apnmt.organization.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddresseTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Addresse.class);
        Addresse addresse1 = new Addresse();
        addresse1.setId(1L);
        Addresse addresse2 = new Addresse();
        addresse2.setId(addresse1.getId());
        assertThat(addresse1).isEqualTo(addresse2);
        addresse2.setId(2L);
        assertThat(addresse1).isNotEqualTo(addresse2);
        addresse1.setId(null);
        assertThat(addresse1).isNotEqualTo(addresse2);
    }
}
