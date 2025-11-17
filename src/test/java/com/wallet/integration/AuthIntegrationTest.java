package com.wallet.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void registerThenLogin() throws Exception {
    String tckn = generateTckn();
    Map<String, Object> registerBody = new HashMap<>();
    registerBody.put("name", "Integration");
    registerBody.put("surname", "Test");
    registerBody.put("tckn", tckn);
    registerBody.put("password", "Password123!");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tckn").value(tckn));

    Map<String, Object> loginBody = new HashMap<>();
    loginBody.put("tckn", tckn);
    loginBody.put("password", "Password123!");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginBody)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }

  private String generateTckn() {
    String base = String.valueOf(System.currentTimeMillis());
    return base.substring(base.length() - 11);
  }
}
