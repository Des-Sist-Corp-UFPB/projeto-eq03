package com.cristiane.salon.models.user.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.user.dto.UserCreateRequest;
import com.cristiane.salon.models.user.dto.UserResponse;
import com.cristiane.salon.models.user.dto.UserUpdateRequest;
import com.cristiane.salon.models.user.entity.Role;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.RoleRepository;
import com.cristiane.salon.models.user.repository.UserRepository;
import com.cristiane.salon.models.employee.entity.Employee;
import com.cristiane.salon.models.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll(Boolean includeInactive) {
        List<User> users;
        if (Boolean.TRUE.equals(includeInactive)) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findByActiveTrue();
        }
        return users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BadRequestException("Email já está em uso");
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(role);
        
        if (request.active() != null) {
            user.setActive(request.active());
        } else {
            user.setActive(true);
        }

        User savedUser = userRepository.save(user);

        if ("FUNCIONARIA".equals(savedUser.getRoleName())) {
            Employee employee = new Employee();
            employee.setUser(savedUser);
            employee.setBio("Profissional especialista");
            employeeRepository.save(employee);
        }

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                throw new BadRequestException("Email já está em uso por outro usuário");
            }
            user.setEmail(request.email());
        }

        if (request.name() != null) user.setName(request.name());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.active() != null) user.setActive(request.active());
        
        if (request.roleId() != null) {
            Role role = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));
            user.setRole(role);
        }

        User savedUser = userRepository.save(user);

        if ("FUNCIONARIA".equals(savedUser.getRoleName())) {
            if (employeeRepository.findByUserId(savedUser.getId()).isEmpty()) {
                Employee employee = new Employee();
                employee.setUser(savedUser);
                employee.setBio("Profissional especialista");
                employeeRepository.save(employee);
            }
        }

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse restore(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        user.setActive(true);
        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }
}
