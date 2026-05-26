package org.mwolff.api.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpaForwardingController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpaForwardingControllerTest {

  @Autowired MockMvc mockMvc;

  @Test
  void rootForwardsToIndex() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void spaRouteForwardsToIndex() throws Exception {
    mockMvc
        .perform(get("/books"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void nestedSpaRouteForwardsToIndex() throws Exception {
    mockMvc
        .perform(get("/books/details"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void apiPathIsNotForwarded() throws Exception {
    mockMvc.perform(get("/api/books")).andExpect(status().isNotFound());
  }

  @Test
  void actuatorPathIsNotForwarded() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
  }

  @Test
  void assetsPathIsNotForwarded() throws Exception {
    mockMvc.perform(get("/assets/main.js")).andExpect(status().isNotFound());
  }

  @Test
  void pathWithExtensionIsNotForwarded() throws Exception {
    mockMvc.perform(get("/favicon.ico")).andExpect(status().isNotFound());
  }
}
