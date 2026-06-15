package org.mwolff.api.tools.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.springframework.http.MediaType;

class MarkdownToPdfUseCaseTest {

  @Test
  void shouldDelegateToPort() {
    final PythonToolsPort tools = Mockito.mock(PythonToolsPort.class);
    final ToolImageResult expected =
        new ToolImageResult(new byte[] {1, 2}, MediaType.APPLICATION_PDF_VALUE);
    given(tools.convertMarkdownToPdf("# Hi")).willReturn(expected);

    final MarkdownToPdfUseCase useCase = new MarkdownToPdfUseCase(tools);

    assertThat(useCase.execute("# Hi")).isSameAs(expected);
  }
}
