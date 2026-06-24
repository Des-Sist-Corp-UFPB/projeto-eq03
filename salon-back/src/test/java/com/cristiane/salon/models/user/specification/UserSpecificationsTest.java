package com.cristiane.salon.models.user.specification;

import com.cristiane.salon.models.user.dto.ClientFilter;
import com.cristiane.salon.models.user.dto.UserFilter;
import com.cristiane.salon.models.user.entity.Role;
import com.cristiane.salon.models.user.entity.User;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class UserSpecificationsTest {

    @Test
    @SuppressWarnings("unchecked")
    void testFilterClientsAllBranches() {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Join<User, Role> roleJoin = mock(Join.class);
        Path<Object> roleNamePath = mock(Path.class);
        doReturn(roleJoin).when(root).join("role");
        when(roleJoin.get("name")).thenReturn(roleNamePath);

        Path<String> namePath = mock(Path.class);
        Expression<String> lowerName = mock(Expression.class);
        doReturn(namePath).when(root).get("name");
        when(cb.lower(namePath)).thenReturn(lowerName);

        Path<String> emailPath = mock(Path.class);
        Expression<String> lowerEmail = mock(Expression.class);
        doReturn(emailPath).when(root).get("email");
        when(cb.lower(emailPath)).thenReturn(lowerEmail);

        Path<String> phonePath = mock(Path.class);
        Expression<String> lowerPhone = mock(Expression.class);
        doReturn(phonePath).when(root).get("phone");
        when(cb.lower(phonePath)).thenReturn(lowerPhone);

        Path<Object> cpfPath = mock(Path.class);
        when(root.get("cpf")).thenReturn(cpfPath);

        Path<Object> activePath = mock(Path.class);
        when(root.get("active")).thenReturn(activePath);

        // Test with full filter
        ClientFilter filter = new ClientFilter("Maria", "maria@email.com", "99999", "123.456.789-09", true);
        Specification<User> spec = UserSpecifications.filterClients(filter);
        spec.toPredicate(root, query, cb);

        verify(cb).equal(roleNamePath, "CLIENTE");
        verify(cb).like(lowerName, "%maria%");
        verify(cb).like(lowerEmail, "%maria@email.com%");
        verify(cb).like(lowerPhone, "%99999%");
        verify(cb).equal(cpfPath, "12345678909");
        verify(cb).equal(activePath, true);

        // Test with empty/null/blank filter values to trigger other branches
        ClientFilter emptyFilter = new ClientFilter("", "  ", null, "", null);
        Specification<User> specEmpty = UserSpecifications.filterClients(emptyFilter);
        specEmpty.toPredicate(root, query, cb);

        // Test with null filter object
        Specification<User> specNull = UserSpecifications.filterClients(null);
        specNull.toPredicate(root, query, cb);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFilterUsersAllBranches() {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Join<User, Role> roleJoin = mock(Join.class);
        Path<Object> roleNamePath = mock(Path.class);
        Path<Object> roleIdPath = mock(Path.class);
        doReturn(roleJoin).when(root).join("role");
        when(roleJoin.get("name")).thenReturn(roleNamePath);
        when(roleJoin.get("id")).thenReturn(roleIdPath);

        Path<String> namePath = mock(Path.class);
        Expression<String> lowerName = mock(Expression.class);
        doReturn(namePath).when(root).get("name");
        when(cb.lower(namePath)).thenReturn(lowerName);

        Path<String> emailPath = mock(Path.class);
        Expression<String> lowerEmail = mock(Expression.class);
        doReturn(emailPath).when(root).get("email");
        when(cb.lower(emailPath)).thenReturn(lowerEmail);

        Path<String> phonePath = mock(Path.class);
        Expression<String> lowerPhone = mock(Expression.class);
        doReturn(phonePath).when(root).get("phone");
        when(cb.lower(phonePath)).thenReturn(lowerPhone);

        Path<Object> activePath = mock(Path.class);
        when(root.get("active")).thenReturn(activePath);

        // Test with full filter
        UserFilter filter = new UserFilter("Jane", "jane@email.com", "88888", false, 2L);
        Specification<User> spec = UserSpecifications.filterUsers(filter);
        spec.toPredicate(root, query, cb);

        verify(cb).notEqual(roleNamePath, "CLIENTE");
        verify(cb).like(lowerName, "%jane%");
        verify(cb).like(lowerEmail, "%jane@email.com%");
        verify(cb).like(lowerPhone, "%88888%");
        verify(cb).equal(activePath, false);
        verify(cb).equal(roleIdPath, 2L);

        // Test with empty/null/blank filter values to trigger other branches
        UserFilter emptyFilter = new UserFilter("", "  ", null, null, null);
        Specification<User> specEmpty = UserSpecifications.filterUsers(emptyFilter);
        specEmpty.toPredicate(root, query, cb);

        // Test with null filter object
        Specification<User> specNull = UserSpecifications.filterUsers(null);
        specNull.toPredicate(root, query, cb);
    }
}
