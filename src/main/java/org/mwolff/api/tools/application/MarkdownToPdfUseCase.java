package org.mwolff.api.tools.application;

import org.mwolff.api.tools.domain.PythonToolsPort;
import org.mwolff.api.tools.domain.ToolImageResult;
import org.springframework.stereotype.Component;

/** Use-Case: Markdown-Text an den python-tools-Port zur PDF-Konvertierung weiterreichen (#27). */
@Component
public class MarkdownToPdfUseCase {

  private final PythonToolsPort tools;

  public MarkdownToPdfUseCase(PythonToolsPort tools) {
    this.tools = tools;
  }

  public ToolImageResult execute(String markdown) {
    return tools.convertMarkdownToPdf(markdown);
  }
}
