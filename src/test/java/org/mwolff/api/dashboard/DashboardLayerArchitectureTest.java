package org.mwolff.api.dashboard;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * Hexagonal-Layer-Regeln für die Dashboard-Komponente. Identisches Schema wie {@code
 * ToolsLayerArchitectureTest}, damit Dashboard sich nicht von der Konvention löst.
 */
class DashboardLayerArchitectureTest {

  private static JavaClasses dashboardClasses;

  @BeforeAll
  static void importDashboardClasses() {
    dashboardClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api.dashboard");
  }

  @Test
  void layersShouldRespectHexagonalTopology() {
    final ArchRule rule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("domain")
            .definedBy("org.mwolff.api.dashboard.domain..")
            .layer("application")
            .definedBy("org.mwolff.api.dashboard.application..")
            .layer("web")
            .definedBy("org.mwolff.api.dashboard.web..")
            .layer("infrastructure")
            .definedBy("org.mwolff.api.dashboard.infrastructure..")
            .whereLayer("web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("application")
            .mayOnlyBeAccessedByLayers("web")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("application", "web", "infrastructure");
    rule.check(dashboardClasses);
  }

  @Test
  void domainShouldNotImportSpringJpaOrJakarta() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.dashboard.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
            .as("Domain must stay framework-free (no Spring, Jakarta, Hibernate)")
            .because("the inner hexagon must not know about persistence or transport");
    rule.check(dashboardClasses);
  }

  @Test
  void applicationShouldNotImportSpringWebOrJpa() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.dashboard.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework.web..", "jakarta.servlet..", "jakarta.persistence..")
            .as("Application layer must not see Spring Web or JPA")
            .because("use-cases speak in terms of ports — transport and persistence are adapters");
    rule.check(dashboardClasses);
  }
}
