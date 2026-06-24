package com.cristiane.salon.models.user.specification;

import com.cristiane.salon.models.user.dto.ClientFilter;
import com.cristiane.salon.models.user.dto.UserFilter;
import com.cristiane.salon.models.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserSpecificationsTest {

    @Test
    void testFilterClientsReturnsSpecification() {
        ClientFilter filter = new ClientFilter("John", "john@email.com", "999999", "12345678909", true);
        Specification<User> spec = UserSpecifications.filterClients(filter);
        assertNotNull(spec);
    }

    @Test
    void testFilterUsersReturnsSpecification() {
        UserFilter filter = new UserFilter("Jane", "jane@email.com", "888888", true, 1L);
        Specification<User> spec = UserSpecifications.filterUsers(filter);
        assertNotNull(spec);
    }
}
