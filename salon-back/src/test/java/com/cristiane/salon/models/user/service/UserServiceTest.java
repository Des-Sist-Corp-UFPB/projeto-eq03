package com.cristiane.salon.models.user.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.exception.UnauthorizedException;
import com.cristiane.salon.models.employee.entity.Employee;
import com.cristiane.salon.models.employee.repository.EmployeeRepository;
import com.cristiane.salon.models.user.dto.UserCreateRequest;
import com.cristiane.salon.models.user.dto.UserResponse;
import com.cristiane.salon.models.user.dto.UserUpdateRequest;
import com.cristiane.salon.models.user.entity.Role;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.RoleRepository;
import com.cristiane.salon.models.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private UserService userService;

    private User activeUser;
    private User inactiveUser;
    private Role clientRole;
    private Role staffRole;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(User user) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(user.getEmail());
        SecurityContext secCtx = mock(SecurityContext.class);
        lenient().when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }


    @BeforeEach
    void setUp() {
        clientRole = new Role(1L, "CLIENTE", null);

        staffRole = new Role(2L, "FUNCIONARIA", null);

        activeUser = new User();
        activeUser.setId(10L);
        activeUser.setName("Maria Silva");
        activeUser.setEmail("maria@example.com");
        activeUser.setPassword("encodedPassword");
        activeUser.setPhone("81999999999");
        activeUser.setActive(true);
        activeUser.setRole(clientRole);
        activeUser.setCreatedAt(LocalDateTime.now());

        inactiveUser = new User();
        inactiveUser.setId(11L);
        inactiveUser.setName("Joana Inativa");
        inactiveUser.setEmail("joana@example.com");
        inactiveUser.setPassword("encodedPassword");
        inactiveUser.setPhone("81888888888");
        inactiveUser.setActive(false);
        inactiveUser.setRole(clientRole);
        inactiveUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void findAll_whenIncludeInactiveIsTrue_shouldReturnAllUsers() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(activeUser, inactiveUser));

        // Act
        List<UserResponse> result = userService.findAll(true);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).email()).isEqualTo("maria@example.com");
        assertThat(result.get(1).email()).isEqualTo("joana@example.com");
        verify(userRepository).findAll();
        verify(userRepository, never()).findByActiveTrue();
    }

    @Test
    void findAll_whenIncludeInactiveIsFalse_shouldReturnActiveUsersOnly() {
        // Arrange
        when(userRepository.findByActiveTrue()).thenReturn(List.of(activeUser));

        // Act
        List<UserResponse> result = userService.findAll(false);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("maria@example.com");
        verify(userRepository).findByActiveTrue();
        verify(userRepository, never()).findAll();
    }

    @Test
    void findAll_whenIncludeInactiveIsNull_shouldReturnActiveUsersOnly() {
        // Arrange
        when(userRepository.findByActiveTrue()).thenReturn(List.of(activeUser));

        // Act
        List<UserResponse> result = userService.findAll(null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("maria@example.com");
        verify(userRepository).findByActiveTrue();
        verify(userRepository, never()).findAll();
    }

    @Test
    void findById_whenUserExists_shouldReturnUserResponse() {
        // Arrange
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));

        // Act
        UserResponse result = userService.findById(10L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.email()).isEqualTo("maria@example.com");
        verify(userRepository).findById(10L);
    }

    @Test
    void findById_whenUserDoesNotExist_shouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void create_whenEmailAlreadyInUse_shouldThrowBadRequestException() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest("New User", "maria@example.com", "password", "123", true, 1L);
        when(userRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(activeUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email já está em uso");
    }

    @Test
    void create_whenRoleNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest("New User", "new@example.com", "password", "123", true, 9L);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(9L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Role não encontrada");
    }

    @Test
    void create_whenClientRoleAndActiveTrue_shouldCreateUserWithoutEmployee() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest("New Client", "client@example.com", "password", "123", true, 1L);
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded_pass");

        User savedUser = new User();
        savedUser.setId(12L);
        savedUser.setName(request.name());
        savedUser.setEmail(request.email());
        savedUser.setPassword("encoded_pass");
        savedUser.setPhone(request.phone());
        savedUser.setRole(clientRole);
        savedUser.setActive(true);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse result = userService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(12L);
        assertThat(result.active()).isTrue();
        assertThat(result.role()).isEqualTo("CLIENTE");

        verify(userRepository).save(any(User.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void create_whenActiveFieldIsNull_shouldDefaultToTrue() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest("New Client", "client@example.com", "password", "123", null, 1L);
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded_pass");

        User savedUser = new User();
        savedUser.setId(12L);
        savedUser.setName(request.name());
        savedUser.setEmail(request.email());
        savedUser.setPassword("encoded_pass");
        savedUser.setPhone(request.phone());
        savedUser.setRole(clientRole);
        savedUser.setActive(true);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse result = userService.create(request);

        // Assert
        assertThat(result.active()).isTrue();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getActive()).isTrue();
    }

    @Test
    void create_whenStaffRole_shouldCreateUserAndAutoCreateEmployee() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest("New Staff", "staff@example.com", "password", "123", true, 2L);
        when(userRepository.findByEmail("staff@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findById(2L)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("password")).thenReturn("encoded_pass");

        User savedUser = new User();
        savedUser.setId(13L);
        savedUser.setName(request.name());
        savedUser.setEmail(request.email());
        savedUser.setPassword("encoded_pass");
        savedUser.setPhone(request.phone());
        savedUser.setRole(staffRole);
        savedUser.setActive(true);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse result = userService.create(request);

        // Assert
        assertThat(result.role()).isEqualTo("FUNCIONARIA");
        verify(userRepository).save(any(User.class));
        
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee savedEmployee = employeeCaptor.getValue();
        assertThat(savedEmployee.getUser()).isEqualTo(savedUser);
        assertThat(savedEmployee.getBio()).isEqualTo("Profissional especialista");
    }

    @Test
    void update_whenUserNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("Updated Name", "email@example.com", "pass", "123", null, false, 1L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void update_whenEmailChangedAndAlreadyInUse_shouldThrowBadRequestException() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, "other@example.com", null, null, null, null, null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.update(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email já está em uso por outro usuário");
    }

    @Test
    void update_whenRoleChangedAndRoleNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, null, null, null, 99L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.update(10L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Role não encontrada");
    }

    @Test
    void update_withAllFieldsAndChangeRoleToStaff_whenEmployeeDoesNotExist_shouldCreateEmployee() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest("Updated Name", "maria@example.com", "new_password", "8177777777", null, false, 2L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded_pass");
        
        // Mock saved state
        User updatedUser = new User();
        updatedUser.setId(10L);
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("maria@example.com");
        updatedUser.setPassword("new_encoded_pass");
        updatedUser.setPhone("8177777777");
        updatedUser.setActive(false);
        updatedUser.setRole(staffRole); // Updated to staff

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.empty());

        // Act
        UserResponse result = userService.update(10L, request);

        // Assert
        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(result.phone()).isEqualTo("8177777777");
        assertThat(result.active()).isFalse();
        assertThat(result.role()).isEqualTo("FUNCIONARIA");

        verify(passwordEncoder).encode("new_password");
        verify(userRepository).save(activeUser);
        
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        assertThat(employeeCaptor.getValue().getUser()).isEqualTo(updatedUser);
    }

    @Test
    void update_withChangeRoleToStaff_whenEmployeeAlreadyExists_shouldNotDuplicateEmployee() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, null, null, null, 2L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(staffRole));

        User updatedUser = new User();
        updatedUser.setId(10L);
        updatedUser.setName("Maria Silva");
        updatedUser.setEmail("maria@example.com");
        updatedUser.setPassword("encodedPassword");
        updatedUser.setRole(staffRole); // Updated to staff
        updatedUser.setActive(true);

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(new Employee()));

        // Act
        UserResponse result = userService.update(10L, request);

        // Assert
        assertThat(result.role()).isEqualTo("FUNCIONARIA");
        verify(userRepository).save(activeUser);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void update_whenPasswordIsBlankOrNull_shouldNotEncodePassword() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(null, null, "  ", null, null, null, null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        // Act
        userService.update(10L, request);

        // Assert
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void delete_whenUserExists_shouldSetActiveToFalse() {
        // Arrange
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        // Act
        userService.delete(10L);

        // Assert
        assertThat(activeUser.getActive()).isFalse();
        verify(userRepository).save(activeUser);
    }

    @Test
    void delete_whenUserNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void restore_whenUserExists_shouldSetActiveToTrue() {
        // Arrange
        when(userRepository.findById(11L)).thenReturn(Optional.of(inactiveUser));
        
        User savedUser = new User();
        savedUser.setId(11L);
        savedUser.setEmail("joana@example.com");
        savedUser.setActive(true);
        savedUser.setRole(clientRole);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse result = userService.restore(11L);

        // Assert
        assertThat(inactiveUser.getActive()).isTrue();
        assertThat(result.active()).isTrue();
        verify(userRepository).save(inactiveUser);
    }

    @Test
    void restore_whenUserNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.restore(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    // --- updateMyCpf tests ---

    @Test
    void updateMyCpf_whenSuccess_shouldSaveCpfForAuthenticatedUser() {
        // Arrange
        mockAuthenticatedUser(activeUser);
        when(userRepository.findByCpf("09123456752")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        // Act
        UserResponse result = userService.updateMyCpf("09123456752");

        // Assert
        assertThat(activeUser.getCpf()).isEqualTo("09123456752");
        assertThat(result).isNotNull();
        verify(userRepository).save(activeUser);
    }

    @Test
    void updateMyCpf_whenCpfAlreadyInUseByAnotherUser_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(activeUser);

        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setCpf("09123456752");
        when(userRepository.findByCpf("09123456752")).thenReturn(Optional.of(otherUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateMyCpf("09123456752"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Este CPF já está cadastrado em outra conta.");
    }

    @Test
    void updateMyCpf_whenSameUserAlreadyHasCpf_shouldUpdateSuccessfully() {
        // Arrange — user 10 already owns this CPF
        activeUser.setCpf("09123456752");
        mockAuthenticatedUser(activeUser);
        when(userRepository.findByCpf("09123456752")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        // Act
        UserResponse result = userService.updateMyCpf("09123456752");

        // Assert — should NOT throw, because it's the same user
        assertThat(result).isNotNull();
        verify(userRepository).save(activeUser);
    }

    @Test
    void updateMyCpf_whenUserNotAuthenticated_shouldThrowUnauthorizedException() {
        // Arrange — configure SecurityContext with unknown email
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("unknown@ghost.com");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
        when(userRepository.findByEmail("unknown@ghost.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateMyCpf("09123456752"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Usuário não autenticado");
    }

    @Test
    void updateMyCpf_whenCpfIsInvalid_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(activeUser);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateMyCpf("11111111111"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("CPF inválido. Por favor, insira um CPF válido.");
    }
}
