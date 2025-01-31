/*
 * Copyright 2025 IQKV Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.sample.webmvc.dashboard.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRegistration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.iqkv.boot.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for the {@link WebConfigurer} class.
 */
class WebConfigurerTest {

  private WebConfigurer webConfigurer;

  private MockServletContext servletContext;

  private MockEnvironment env;

  private SecurityProperties props;

  @BeforeEach
  public void setup() {
    servletContext = spy(new MockServletContext());
    doReturn(mock(FilterRegistration.Dynamic.class)).when(servletContext).addFilter(anyString(), any(Filter.class));
    doReturn(mock(ServletRegistration.Dynamic.class)).when(servletContext).addServlet(anyString(), any(Servlet.class));

    env = new MockEnvironment();
    props = new SecurityProperties();

    webConfigurer = new WebConfigurer(env, props);
  }

  @Test
  void shouldCorsFilterOnApiPath() throws Exception {
    props.getCors().setAllowedOrigins(Collections.singletonList("other.domain.com"));
    props.getCors().setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    props.getCors().setAllowedHeaders(Collections.singletonList("*"));
    props.getCors().setMaxAge(1800L);
    props.getCors().setAllowCredentials(true);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebConfigurerTestController()).addFilters(webConfigurer.corsFilter()).build();

    mockMvc
        .perform(
            options("/api/test-cors")
                .header(HttpHeaders.ORIGIN, "other.domain.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        )
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "other.domain.com"))
        .andExpect(header().string(HttpHeaders.VARY, "Origin"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800"));

    mockMvc
        .perform(get("/api/test-cors").header(HttpHeaders.ORIGIN, "other.domain.com"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "other.domain.com"));
  }

  @Test
  void shouldCorsFilterOnOtherPath() throws Exception {
    props.getCors().setAllowedOrigins(Collections.singletonList("*"));
    props.getCors().setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    props.getCors().setAllowedHeaders(Collections.singletonList("*"));
    props.getCors().setMaxAge(1800L);
    props.getCors().setAllowCredentials(true);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebConfigurerTestController()).addFilters(webConfigurer.corsFilter()).build();

    mockMvc
        .perform(get("/test/test-cors").header(HttpHeaders.ORIGIN, "other.domain.com"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void shouldCorsFilterDeactivatedForNullAllowedOrigins() throws Exception {
    props.getCors().setAllowedOrigins(null);

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebConfigurerTestController()).addFilters(webConfigurer.corsFilter()).build();

    mockMvc
        .perform(get("/api/test-cors").header(HttpHeaders.ORIGIN, "other.domain.com"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void shouldCorsFilterDeactivatedForEmptyAllowedOrigins() throws Exception {
    props.getCors().setAllowedOrigins(new ArrayList<>());

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebConfigurerTestController()).addFilters(webConfigurer.corsFilter()).build();

    mockMvc
        .perform(get("/api/test-cors").header(HttpHeaders.ORIGIN, "other.domain.com"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }
}
