package org.mwolff.api.timeseries.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.auth.SecurityConfig;
import org.mwolff.api.timeseries.application.AddEntryUseCase;
import org.mwolff.api.timeseries.application.CreateTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.DeleteTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.GetTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.GetTimeSeriesUseCase.TimeSeriesDetail;
import org.mwolff.api.timeseries.application.ListEntriesUseCase;
import org.mwolff.api.timeseries.application.ListTimeSeriesUseCase;
import org.mwolff.api.timeseries.application.ListTimeSeriesUseCase.TimeSeriesWithCount;
import org.mwolff.api.timeseries.application.UpdateTimeSeriesUseCase;
import org.mwolff.api.timeseries.domain.TimeSeries;
import org.mwolff.api.timeseries.domain.TimeSeriesDataType;
import org.mwolff.api.timeseries.domain.TimeSeriesEntry;
import org.mwolff.api.timeseries.domain.TimeSeriesNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TimeSeriesController.class)
@Import({TimeSeriesExceptionHandler.class, SecurityConfig.class})
class TimeSeriesControllerTest {

  private static final String SUB = "user-1";

  private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
    return jwt().jwt(j -> j.subject(SUB)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ListTimeSeriesUseCase listUseCase;
  @MockitoBean private CreateTimeSeriesUseCase createUseCase;
  @MockitoBean private GetTimeSeriesUseCase getUseCase;
  @MockitoBean private UpdateTimeSeriesUseCase updateUseCase;
  @MockitoBean private DeleteTimeSeriesUseCase deleteUseCase;
  @MockitoBean private AddEntryUseCase addEntryUseCase;
  @MockitoBean private ListEntriesUseCase listEntriesUseCase;

  @MockitoBean private JwtDecoder jwtDecoder;

  private static TimeSeries ts(long id) {
    return new TimeSeries(
        id, SUB, "Weight", "desc", "kg", TimeSeriesDataType.DECIMAL, Instant.EPOCH, Instant.EPOCH);
  }

  // ----- GET /api/timeseries -----------------------------------------------

  @Test
  void listShouldReturnSummaries() throws Exception {
    given(listUseCase.execute(SUB))
        .willReturn(
            List.of(new TimeSeriesWithCount(ts(1L), 5L), new TimeSeriesWithCount(ts(2L), 0L)));

    mockMvc
        .perform(get("/api/timeseries").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].entryCount").value(5))
        .andExpect(jsonPath("$[1].entryCount").value(0));
  }

  // ----- POST /api/timeseries ----------------------------------------------

  @Test
  void createShouldReturn201() throws Exception {
    given(createUseCase.execute(SUB, "Weight", "desc", "kg", TimeSeriesDataType.DECIMAL))
        .willReturn(ts(1L));

    mockMvc
        .perform(
            post("/api/timeseries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Weight\",\"description\":\"desc\",\"unit\":\"kg\",\"dataType\":\"DECIMAL\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Weight"))
        .andExpect(jsonPath("$.entryCount").value(0));
  }

  @Test
  void createShouldReturn400WhenNameBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/timeseries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"unit\":\"kg\",\"dataType\":\"DECIMAL\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createShouldReturn400WhenUnitMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/timeseries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Weight\",\"dataType\":\"DECIMAL\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createShouldReturn400WhenDataTypeMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/timeseries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Weight\",\"unit\":\"kg\"}"))
        .andExpect(status().isBadRequest());
  }

  // ----- GET /api/timeseries/{id} ------------------------------------------

  @Test
  void getShouldReturnDetail() throws Exception {
    given(getUseCase.execute(SUB, 1L)).willReturn(new TimeSeriesDetail(ts(1L), 12L));

    mockMvc
        .perform(get("/api/timeseries/1").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.entryCount").value(12));
  }

  @Test
  void getShouldReturn404ForForeign() throws Exception {
    willThrow(new TimeSeriesNotFoundException(99L)).given(getUseCase).execute(SUB, 99L);

    mockMvc.perform(get("/api/timeseries/99").with(userJwt())).andExpect(status().isNotFound());
  }

  // ----- PUT /api/timeseries/{id} ------------------------------------------

  @Test
  void updateShouldReturnUpdatedDetail() throws Exception {
    given(updateUseCase.execute(SUB, 1L, "New", "newDesc", "g", TimeSeriesDataType.INTEGER))
        .willReturn(ts(1L));
    given(getUseCase.execute(SUB, 1L)).willReturn(new TimeSeriesDetail(ts(1L), 3L));

    mockMvc
        .perform(
            put("/api/timeseries/1")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"New\",\"description\":\"newDesc\",\"unit\":\"g\",\"dataType\":\"INTEGER\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.entryCount").value(3));
  }

  @Test
  void updateShouldReturn400WhenNameBlank() throws Exception {
    mockMvc
        .perform(
            put("/api/timeseries/1")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"unit\":\"g\",\"dataType\":\"INTEGER\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateShouldReturn404WhenForeign() throws Exception {
    willThrow(new TimeSeriesNotFoundException(7L))
        .given(updateUseCase)
        .execute(eq(SUB), eq(7L), any(), any(), any(), any());

    mockMvc
        .perform(
            put("/api/timeseries/7")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"unit\":\"g\",\"dataType\":\"INTEGER\"}"))
        .andExpect(status().isNotFound());
  }

  // ----- DELETE /api/timeseries/{id} ---------------------------------------

  @Test
  void deleteShouldReturn204() throws Exception {
    mockMvc.perform(delete("/api/timeseries/1").with(userJwt())).andExpect(status().isNoContent());
  }

  @Test
  void deleteShouldReturn404WhenForeign() throws Exception {
    willThrow(new TimeSeriesNotFoundException(7L)).given(deleteUseCase).execute(SUB, 7L);

    mockMvc.perform(delete("/api/timeseries/7").with(userJwt())).andExpect(status().isNotFound());
  }

  // ----- GET /api/timeseries/{id}/entries ----------------------------------

  @Test
  void listEntriesShouldReturnEntries() throws Exception {
    final TimeSeriesEntry e =
        new TimeSeriesEntry(10L, 1L, Instant.parse("2026-05-27T12:00:00Z"), new BigDecimal("78.5"));
    given(listEntriesUseCase.execute(SUB, 1L, Optional.empty(), Optional.empty(), Optional.empty()))
        .willReturn(List.of(e));

    mockMvc
        .perform(get("/api/timeseries/1/entries").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[0].value").value(78.5));
  }

  @Test
  void listEntriesShouldPassQueryParams() throws Exception {
    given(
            listEntriesUseCase.execute(
                eq(SUB),
                eq(1L),
                eq(Optional.of(Instant.parse("2026-01-01T00:00:00Z"))),
                eq(Optional.of(Instant.parse("2026-12-31T00:00:00Z"))),
                eq(Optional.of(50))))
        .willReturn(List.of());

    mockMvc
        .perform(
            get("/api/timeseries/1/entries")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-12-31T00:00:00Z")
                .param("limit", "50")
                .with(userJwt()))
        .andExpect(status().isOk());
  }

  @Test
  void listEntriesShouldReturn404ForForeign() throws Exception {
    willThrow(new TimeSeriesNotFoundException(99L))
        .given(listEntriesUseCase)
        .execute(eq(SUB), eq(99L), any(), any(), any());

    mockMvc
        .perform(get("/api/timeseries/99/entries").with(userJwt()))
        .andExpect(status().isNotFound());
  }

  // ----- POST /api/timeseries/{id}/entries ---------------------------------

  @Test
  void addEntryShouldReturn201() throws Exception {
    final TimeSeriesEntry created =
        new TimeSeriesEntry(99L, 1L, Instant.parse("2026-05-27T12:00:00Z"), new BigDecimal("78.5"));
    given(
            addEntryUseCase.execute(
                eq(SUB),
                eq(1L),
                eq(Instant.parse("2026-05-27T12:00:00Z")),
                eq(new BigDecimal("78.5"))))
        .willReturn(created);

    mockMvc
        .perform(
            post("/api/timeseries/1/entries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":78.5}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.value").value(78.5));
  }

  @Test
  void addEntryShouldReturn400WhenTimestampMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/timeseries/1/entries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":78.5}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addEntryShouldReturn400WhenValueMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/timeseries/1/entries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timestamp\":\"2026-05-27T12:00:00Z\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addEntryShouldReturn400WhenIntegerSeriesGetsDecimal() throws Exception {
    willThrow(new IllegalArgumentException("value must not have decimals for INTEGER time series"))
        .given(addEntryUseCase)
        .execute(eq(SUB), eq(1L), any(), any());

    mockMvc
        .perform(
            post("/api/timeseries/1/entries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":78.5}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addEntryShouldReturn404WhenForeign() throws Exception {
    willThrow(new TimeSeriesNotFoundException(7L))
        .given(addEntryUseCase)
        .execute(eq(SUB), eq(7L), any(), any());

    mockMvc
        .perform(
            post("/api/timeseries/7/entries")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timestamp\":\"2026-05-27T12:00:00Z\",\"value\":1}"))
        .andExpect(status().isNotFound());
  }
}
