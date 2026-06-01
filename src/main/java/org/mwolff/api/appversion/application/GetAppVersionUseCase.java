package org.mwolff.api.appversion.application;

import org.mwolff.api.appversion.domain.AppVersion;
import org.mwolff.api.appversion.domain.AppVersionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Liest die aktuelle Anwendungsversion. */
@Component
public class GetAppVersionUseCase {

  private final AppVersionPort port;

  public GetAppVersionUseCase(final AppVersionPort port) {
    this.port = port;
  }

  @Transactional(readOnly = true)
  public AppVersion execute() {
    return port.getCurrent();
  }
}
