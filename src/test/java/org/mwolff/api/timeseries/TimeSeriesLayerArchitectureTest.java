package org.mwolff.api.timeseries;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * Hexagonal-Layer-Regeln fuer die TimeSeries-Komponente. Identisches Schema wie {@code
 * DashboardLayerArchitectureTest}, damit TimeSeries sich nicht von der Konvention loest.
 */
class TimeSeriesLayerArchitectureTest {

  private static JavaClasses timeSeriesClasses;

  @BeforeAll
  static void importTimeSeriesClasses() {
    timeSeriesClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api.timeseries");
  }

  @Test
  void layersShouldRespectHexagonalTopology() {
    final ArchRule rule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("domain")
            .definedBy("org.mwolff.api.timeseries.domain..")
            .layer("application")
            .definedBy("org.mwolff.api.timeseries.application..")
            .layer("web")
            .definedBy("org.mwolff.api.timeseries.web..")
            .layer("infrastructure")
            .definedBy("org.mwolff.api.timeseries.infrastructure..")
            .whereLayer("web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("application")
            .mayOnlyBeAccessedByLayers("web")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("application", "web", "infrastructure");
    rule.check(timeSeriesClasses);
  }

  @Test
  void domainShouldNotImportSpringJpaOrJakarta() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.timeseries.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
            .as("Domain must stay framework-free (no Spring, Jakarta, Hibernate)")
            .because("the inner hexagon must not know about persistence or transport");
    rule.check(timeSeriesClasses);
  }

  @Test
  void applicationShouldNotImportSpringWebOrJpa() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.timeseries.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework.web..", "jakarta.servlet..", "jakarta.persistence..")
            .as("Application layer must not see Spring Web or JPA")
            .because("use-cases speak in terms of ports — transport and persistence are adapters");
    rule.check(timeSeriesClasses);
  }
}
