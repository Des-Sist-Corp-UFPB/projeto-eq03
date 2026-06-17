package com.cristiane.salon;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class SalonApplicationTest {

    @Test
    void testMain() {
        try (MockedStatic<SpringApplication> springAppMock = Mockito.mockStatic(SpringApplication.class)) {
            springAppMock.when(() -> SpringApplication.run(eq(SalonApplication.class), any(String[].class)))
                    .thenReturn(null);

            SalonApplication.main(new String[]{});

            springAppMock.verify(() -> SpringApplication.run(eq(SalonApplication.class), any(String[].class)));
        }
    }

    @Test
    void testConstructor() {
        SalonApplication app = new SalonApplication();
        assertNotNull(app);
    }
}
