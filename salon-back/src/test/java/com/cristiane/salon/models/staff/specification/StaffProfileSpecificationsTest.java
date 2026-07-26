package com.cristiane.salon.models.staff.specification;

import com.cristiane.salon.models.staff.dto.StaffFilter;
import com.cristiane.salon.models.staff.entity.StaffProfile;
import com.cristiane.salon.models.staff.enums.BrazilianState;
import com.cristiane.salon.models.staff.repository.StaffProfileRepository;
import com.cristiane.salon.models.user.entity.Role;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.RoleRepository;
import com.cristiane.salon.models.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a Specification com um Postgres real (Testcontainers). Existe especificamente porque
 * uma versão anterior desta busca (JPQL cru com {@code :name IS NULL OR LOWER(...) LIKE ...})
 * quebrava em produção com "function lower(bytea) does not exist": quando o parâmetro nomeado
 * é usado só dentro de comparações condicionais sem nenhuma âncora de tipo, o driver não
 * consegue inferir o tipo do bind e falha — um teste com repositório mockado não pega isso,
 * só uma query executada de verdade contra o banco.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class StaffProfileSpecificationsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private StaffProfileRepository staffProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User funcionariaUser;
    private User gerenteUser;

    @BeforeEach
    void setUp() {
        staffProfileRepository.deleteAll();
        userRepository.deleteAll();

        Role funcionariaRole = roleRepository.findAll().stream()
                .filter(r -> "FUNCIONARIA".equals(r.getName()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(new Role(null, "FUNCIONARIA", null)));
        Role gerenteRole = roleRepository.findAll().stream()
                .filter(r -> "GERENTE_DE_ATENDIMENTO".equals(r.getName()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(new Role(null, "GERENTE_DE_ATENDIMENTO", null)));

        funcionariaUser = new User();
        funcionariaUser.setName("Maria");
        funcionariaUser.setEmail("maria@teste.com");
        funcionariaUser.setPassword("senha123");
        funcionariaUser.setRole(funcionariaRole);
        funcionariaUser.setActive(true);
        funcionariaUser = userRepository.save(funcionariaUser);

        gerenteUser = new User();
        gerenteUser.setName("Ana");
        gerenteUser.setEmail("ana@teste.com");
        gerenteUser.setPassword("senha123");
        gerenteUser.setRole(gerenteRole);
        gerenteUser.setActive(false);
        gerenteUser = userRepository.save(gerenteUser);

        staffProfileRepository.save(buildProfile(funcionariaUser, "Maria Silva", null, "Recife"));
        staffProfileRepository.save(buildProfile(gerenteUser, "Ana Souza", "Aninha", "Olinda"));
    }

    private StaffProfile buildProfile(User user, String fullName, String socialName, String city) {
        StaffProfile profile = new StaffProfile();
        profile.setUser(user);
        profile.setFullName(fullName);
        profile.setSocialName(socialName);
        profile.setCpf("11144477735");
        profile.setCpfHash("hash-" + user.getId());
        profile.setBirthDate(LocalDate.of(1990, 1, 1));
        profile.setPhone("81999999999");
        profile.setZipCode("50000-000");
        profile.setStreet("Rua A");
        profile.setStreetNumber("10");
        profile.setDistrict("Centro");
        profile.setCity(city);
        profile.setStateUf(BrazilianState.PE);
        return profile;
    }

    @Test
    void filter_withNoFilters_returnsAllProfiles() {
        Page<StaffProfile> result = staffProfileRepository.findAll(
                StaffProfileSpecifications.filter(new StaffFilter(null, null, null)), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void filter_byNamePartialCaseInsensitive_matchesFullName() {
        Page<StaffProfile> result = staffProfileRepository.findAll(
                StaffProfileSpecifications.filter(new StaffFilter("mar", null, null)), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Maria Silva");
    }

    @Test
    void filter_byName_alsoMatchesSocialName() {
        Page<StaffProfile> result = staffProfileRepository.findAll(
                StaffProfileSpecifications.filter(new StaffFilter("aninha", null, null)), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Ana Souza");
    }

    @Test
    void filter_whenNameMatchesNothing_returnsEmpty() {
        Page<StaffProfile> result = staffProfileRepository.findAll(
                StaffProfileSpecifications.filter(new StaffFilter("zzz", null, null)), PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void filter_byRoleName_returnsOnlyMatchingRole() {
        Page<StaffProfile> result = staffProfileRepository.findAll(
                StaffProfileSpecifications.filter(new StaffFilter(null, "FUNCIONARIA", null)),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Maria Silva");
    }

    @Test
    void filter_byActive_returnsOnlyMatchingUsers() {
        Page<StaffProfile> result = staffProfileRepository.findAll(
                StaffProfileSpecifications.filter(new StaffFilter(null, null, false)), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Ana Souza");
    }

    @Test
    void filter_combiningNameAndRole_appliesBoth() {
        Page<StaffProfile> result = staffProfileRepository.findAll(
                StaffProfileSpecifications.filter(new StaffFilter("Maria", "FUNCIONARIA", true)),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }
}
