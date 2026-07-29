package org.xian.protfoliomanage.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerTest {

    @Test
    void meReturnsAuthenticatedUsername() {
        AuthController controller = new AuthController();

        Map<String, String> response = controller.me(new TestingAuthenticationToken("tester", "pw"));

        assertEquals("tester", response.get("username"));
    }
}

