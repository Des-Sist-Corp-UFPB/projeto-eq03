package com.cristiane.salon.security;

import com.cristiane.salon.models.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private final String SECRET_KEY = "my-super-secret-key-32-chars-long-or-more-for-testing-only";
    private final long ACCESS_EXPIRATION = 3600000; // 1 hour
    private final long REFRESH_EXPIRATION = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", REFRESH_EXPIRATION);
    }

    @Test
    void generateAccessToken_shouldAddCustomClaims_whenUserIsCustomUserEntity() {
        // Arrange
        User user = new User();
        user.setId(42L);
        user.setEmail("admin@salon.com");
        com.cristiane.salon.models.user.entity.Role role = new com.cristiane.salon.models.user.entity.Role();
        role.setName("ADMIN");
        user.setRole(role);
        user.setActive(true);

        // Act
        String token = jwtService.generateAccessToken(user);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@salon.com");
        
        Object roleClaim = jwtService.extractClaim(token, claims -> claims.get("role"));
        Object userId = jwtService.extractClaim(token, claims -> claims.get("userId"));
        
        assertThat(roleClaim).isEqualTo("ADMIN");
        assertThat(userId).isEqualTo(42); // Jackson/JSON parses as Integer/Double in some contexts but JJWT returns Integer/Long depending on size, AssertJ matches conversion
    }

    @Test
    void generateAccessToken_shouldNotAddCustomClaims_whenUserIsGenericUserDetails() {
        // Arrange
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("generic@salon.com");
        org.mockito.Mockito.doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .when(userDetails).getAuthorities();

        // Act
        String token = jwtService.generateAccessToken(userDetails);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("generic@salon.com");
        
        Object role = jwtService.extractClaim(token, claims -> claims.get("role"));
        Object userId = jwtService.extractClaim(token, claims -> claims.get("userId"));
        
        assertThat(role).isNull();
        assertThat(userId).isNull();
    }

    @Test
    void generateRefreshToken_shouldGenerateValidToken() {
        // Arrange
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("refresh@salon.com");

        // Act
        String token = jwtService.generateRefreshToken(userDetails);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("refresh@salon.com");
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {
        // Arrange
        User user = new User();
        user.setEmail("valid@salon.com");
        user.setActive(true);
        String token = jwtService.generateAccessToken(user);

        // Act
        boolean result = jwtService.isTokenValid(token, user);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameMismatches() {
        // Arrange
        User user1 = new User();
        user1.setEmail("user1@salon.com");
        user1.setActive(true);

        User user2 = new User();
        user2.setEmail("user2@salon.com");
        user2.setActive(true);

        String token = jwtService.generateAccessToken(user1);

        // Act
        boolean result = jwtService.isTokenValid(token, user2);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUserIsDisabled() {
        // Arrange
        User user = new User();
        user.setEmail("disabled@salon.com");
        user.setActive(false); // isEnabled returns active

        String token = jwtService.generateAccessToken(user);

        // Act
        boolean result = jwtService.isTokenValid(token, user);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isTokenValid_shouldThrowExpiredJwtException_whenTokenIsExpired() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", -1000L); // Negative expiration to force expired
        User user = new User();
        user.setEmail("expired@salon.com");
        user.setActive(true);

        String token = jwtService.generateAccessToken(user);

        // Reset expiration for parsing check
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", ACCESS_EXPIRATION);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
