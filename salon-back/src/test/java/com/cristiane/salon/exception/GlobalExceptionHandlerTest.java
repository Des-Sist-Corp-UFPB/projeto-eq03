package com.cristiane.salon.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleResourceNotFoundException_shouldReturnNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
        assertThat(response.getBody().getError()).isEqualTo("Não Encontrado");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleBadRequestException_shouldReturnBadRequest() {
        BadRequestException ex = new BadRequestException("Bad request");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequestException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Bad request");
        assertThat(response.getBody().getError()).isEqualTo("Requisição Inválida");
    }

    @Test
    void handleUnauthorizedException_shouldReturnUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Unauthorized access");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Unauthorized access");
        assertThat(response.getBody().getError()).isEqualTo("Não Autorizado");
    }

    @Test
    void handleAuthenticationException_shouldReturnUnauthorized() {
        AuthenticationException ex = mock(AuthenticationException.class);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Email ou senha incorretos");
        assertThat(response.getBody().getError()).isEqualTo("Não Autorizado");
    }

    @Test
    void handleAccessDeniedException_withDefaultMessage_shouldReturnForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Você não possui permissão para acessar este recurso.");
        assertThat(response.getBody().getError()).isEqualTo("Acesso Negado");
    }

    @Test
    void handleAccessDeniedException_withCustomMessage_shouldReturnForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Custom denied msg");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Custom denied msg");
        assertThat(response.getBody().getError()).isEqualTo("Acesso Negado");
    }

    @Test
    void handleValidationExceptions_shouldReturnValidationError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("obj", "field1", "Field 1 invalid");
        FieldError error2 = new FieldError("obj", "field2", "Field 2 invalid");
        
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Field 1 invalid", "Field 2 invalid");
        assertThat(response.getBody().getError()).isEqualTo("Erro de Validação");
    }

    @Test
    void handleConstraintViolation_shouldReturnValidationError() {
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("field");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("Constraint violation msg");
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Constraint violation msg");
        assertThat(response.getBody().getError()).isEqualTo("Erro de Validação");
    }

    @Test
    void handleUnreadable_shouldReturnBadRequest() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Unreadable", mock(HttpInputMessage.class));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnreadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Corpo da requisição inválido. Verifique datas e números no formulário.");
        assertThat(response.getBody().getError()).isEqualTo("Requisição Inválida");
    }

    @Test
    void handleMissingParams_shouldReturnBadRequest() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("userId", "Long");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingParams(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Parâmetro obrigatório 'userId' ausente");
        assertThat(response.getBody().getError()).isEqualTo("Parâmetro Obrigatório");
    }

    @Test
    void handleDataIntegrity_withScheduledAtMessage_shouldReturnHint() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        Throwable cause = new Throwable("scheduled_at cannot be null");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Flyway");
        assertThat(response.getBody().getError()).isEqualTo("Dados Incompatíveis");
    }

    @Test
    void handleDataIntegrity_withOtherMessage_shouldReturnDefaultHint() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        Throwable cause = new Throwable("other database error");
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Não foi possível concluir a operação no banco de dados.");
        assertThat(response.getBody().getError()).isEqualTo("Dados Incompatíveis");
    }

    @Test
    void handleDataIntegrity_withNullCauseMessage_shouldReturnDefaultHint() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        Throwable cause = mock(Throwable.class);
        when(cause.getMessage()).thenReturn(null);
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Não foi possível concluir a operação no banco de dados.");
        assertThat(response.getBody().getError()).isEqualTo("Dados Incompatíveis");
    }

    @Test
    void handleMethodNotSupported_shouldReturnMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().getMessage()).isEqualTo("Request method 'POST' is not supported");
        assertThat(response.getBody().getError()).isEqualTo("Método Não Suportado");
    }

    @Test
    void handleNotFound_shouldReturnNotFound() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/uri", null);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Recurso não encontrado");
        assertThat(response.getBody().getError()).isEqualTo("Não Encontrado");
    }

    @Test
    void handleNoResourceFound_shouldReturnNotFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Recurso não encontrado");
        assertThat(response.getBody().getError()).isEqualTo("Não Encontrado");
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new Exception("Generic error");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Ocorreu um erro inesperado no servidor.");
        assertThat(response.getBody().getError()).isEqualTo("Erro Interno do Servidor");
    }

    @Test
    void handleIllegalArgument_shouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument value");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument value");
        assertThat(response.getBody().getError()).isEqualTo("Argumento Inválido");
    }
}
