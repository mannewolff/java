package org.mwolff.api.ingest.application;

import java.util.List;

import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.IngestTokenPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Liefert alle Ingest-Tokens eines Users (inkl. widerrufener). */
@Component
public class ListIngestTokensUseCase {

  private final IngestTokenPort tokens;

  public ListIngestTokensUseCase(IngestTokenPort tokens) {
    this.tokens = tokens;
  }

  @Transactional(readOnly = true)
  public List<IngestToken> execute(String userSub) {
    return tokens.findAllByUser(userSub);
  }
}
