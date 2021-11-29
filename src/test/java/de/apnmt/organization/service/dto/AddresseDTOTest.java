package de.apnmt.organization.service.dto;

import de.apnmt.organization.common.service.dto.AddresseDTO;
import de.apnmt.organization.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddresseDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AddresseDTO.class);
        AddresseDTO addresseDTO1 = new AddresseDTO();
        addresseDTO1.setId(1L);
        AddresseDTO addresseDTO2 = new AddresseDTO();
        assertThat(addresseDTO1).isNotEqualTo(addresseDTO2);
        addresseDTO2.setId(addresseDTO1.getId());
        assertThat(addresseDTO1).isEqualTo(addresseDTO2);
        addresseDTO2.setId(2L);
        assertThat(addresseDTO1).isNotEqualTo(addresseDTO2);
        addresseDTO1.setId(null);
        assertThat(addresseDTO1).isNotEqualTo(addresseDTO2);
    }
}
