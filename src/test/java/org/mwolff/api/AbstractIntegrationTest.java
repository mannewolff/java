package org.mwolff.api;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container @ServiceConnection
  protected static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");
}
