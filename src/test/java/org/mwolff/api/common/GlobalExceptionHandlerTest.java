package org.mwolff.api.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void shouldMapMethodArgumentNotValidExceptionToBadRequestWithFieldErrors() throws Exception {
    // given — ein Beispiel-Target mit Field-Error
    final BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new ExampleTarget(), "exampleTarget");
    bindingResult.addError(new FieldError("exampleTarget", "name", "must not be blank"));
    final Method method = ExampleTarget.class.getDeclaredMethod("setName", String.class);
    final MethodParameter param = new MethodParameter(method, 0);
    final MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(param, bindingResult);

    // when
    final ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    @SuppressWarnings("unchecked")
    final Map<String, String> fields = (Map<String, String>) response.getBody().get("fieldErrors");
    assertThat(fields).containsEntry("name", "must not be blank");
  }

  @Test
  void shouldMapConstraintViolationWithDottedPathToShortFieldName() {
    // given — Path enthaelt einen Punkt; Handler darf nur den Tail als Feldname nehmen.
    final ConstraintViolation<?> violation = violationWithPath("crop.yOffset", "must be >= 0");
    final ConstraintViolationException ex =
        new ConstraintViolationException("validation failed", Set.of(violation));

    final ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    @SuppressWarnings("unchecked")
    final Map<String, String> fields = (Map<String, String>) response.getBody().get("fieldErrors");
    assertThat(fields).containsEntry("yOffset", "must be >= 0");
  }

  @Test
  void shouldMapConstraintViolationWithSinglePathToFullName() {
    // given — Path ohne Punkt → kompletter Path wird Feldname (dot < 0).
    final ConstraintViolation<?> violation = violationWithPath("quality", "must be >= 50");
    final ConstraintViolationException ex =
        new ConstraintViolationException("validation failed", Set.of(violation));

    final ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);

    @SuppressWarnings("unchecked")
    final Map<String, String> fields = (Map<String, String>) response.getBody().get("fieldErrors");
    assertThat(fields).containsEntry("quality", "must be >= 50");
  }

  @Test
  void shouldMapConstraintViolationWithLeadingDotPathToTail() {
    // given — lastIndexOf('.') == 0: Grenzfall fuer dot >= 0 (killt ConditionalsBoundary,
    // PIT #207). Der Mutant dot > 0 wuerde ".x" statt "x" als Feldname liefern.
    final ConstraintViolation<?> violation = violationWithPath(".x", "must be set");
    final ConstraintViolationException ex =
        new ConstraintViolationException("validation failed", Set.of(violation));

    final ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);

    @SuppressWarnings("unchecked")
    final Map<String, String> fields = (Map<String, String>) response.getBody().get("fieldErrors");
    assertThat(fields).containsEntry("x", "must be set").doesNotContainKey(".x");
  }

  @Test
  void shouldHandleEmptyConstraintViolations() {
    final ConstraintViolationException ex =
        new ConstraintViolationException("validation failed", new HashSet<>());

    final ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    @SuppressWarnings("unchecked")
    final Map<String, String> fields = (Map<String, String>) response.getBody().get("fieldErrors");
    assertThat(fields).isEmpty();
  }

  @Test
  void shouldMapMethodArgumentTypeMismatchToConsistentBadRequestFormat() throws Exception {
    // given — z. B. ein GET mit ?includeArchived=yes auf einen boolean-Query-Parameter: Spring
    // lehnt die Bindung des primitiven booleans ab, bevor der Controller ueberhaupt laeuft.
    final Method method = ExampleTarget.class.getDeclaredMethod("setName", String.class);
    final MethodParameter param = new MethodParameter(method, 0);
    final MethodArgumentTypeMismatchException ex =
        new MethodArgumentTypeMismatchException(
            "yes", Boolean.class, "includeArchived", param, null);

    final ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().get("message").toString()).contains("includeArchived");
    assertThat(response.getBody()).containsKeys("timestamp", "status", "error", "message");
  }

  private static ConstraintViolation<?> violationWithPath(String pathStr, String message) {
    final Path path = Mockito.mock(Path.class);
    Mockito.when(path.toString()).thenReturn(pathStr);
    final ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
    Mockito.when(violation.getPropertyPath()).thenReturn(path);
    Mockito.when(violation.getMessage()).thenReturn(message);
    return violation;
  }

  /** Reines Test-Target — irrelevant, dient nur als Reflection-Quelle für MethodParameter. */
  static final class ExampleTarget {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
