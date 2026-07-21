package org.mwolff.api.ingest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/** Hexagonal-Layer-Regeln fuer das Ingest-Modul. Identisches Schema wie Dashboard/TimeSeries. */
class IngestLayerArchitectureTest {

  private static JavaClasses ingestClasses;

  @BeforeAll
  static void importIngestClasses() {
    ingestClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api.ingest");
  }

  @Test
  void layersShouldRespectHexagonalTopology() {
    final ArchRule rule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("domain")
            .definedBy("org.mwolff.api.ingest.domain..")
            .layer("application")
            .definedBy("org.mwolff.api.ingest.application..")
            .layer("web")
            .definedBy("org.mwolff.api.ingest.web..")
            .layer("infrastructure")
            .definedBy("org.mwolff.api.ingest.infrastructure..")
            .whereLayer("web")
            // Web wird von SecurityConfig (org.mwolff.api.auth) referenziert, das
            // ist gewollt — daher kein "mayNotBeAccessedByAnyLayer".
            .mayOnlyBeAccessedByLayers("web")
            .whereLayer("infrastructure")
            // Genauso wird IngestRateLimiter von der SecurityConfig referenziert —
            // Wiring-Spezialfall, kein Bruch des Hexagons.
            .mayOnlyBeAccessedByLayers("infrastructure", "web")
            .whereLayer("application")
            .mayOnlyBeAccessedByLayers("web")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("application", "web", "infrastructure");
    rule.check(ingestClasses);
  }

  @Test
  void domainShouldNotImportSpringJpaOrJakarta() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.ingest.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
            .as("Domain must stay framework-free (no Spring, Jakarta, Hibernate)");
    rule.check(ingestClasses);
  }

  @Test
  void applicationShouldNotImportSpringWebOrJpa() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.ingest.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework.web..", "jakarta.servlet..", "jakarta.persistence..")
            .as("Application layer must not see Spring Web or JPA");
    rule.check(ingestClasses);
  }
}
