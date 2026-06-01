package org.mwolff.api.appversion;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/** Hexagonal-Layer-Regeln fuer das AppVersion-Modul. Identisches Schema wie die anderen Module. */
class AppVersionLayerArchitectureTest {

  private static JavaClasses appVersionClasses;

  @BeforeAll
  static void importAppVersionClasses() {
    appVersionClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api.appversion");
  }

  @Test
  void layersShouldRespectHexagonalTopology() {
    final ArchRule rule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("domain")
            .definedBy("org.mwolff.api.appversion.domain..")
            .layer("application")
            .definedBy("org.mwolff.api.appversion.application..")
            .layer("web")
            .definedBy("org.mwolff.api.appversion.web..")
            .layer("infrastructure")
            .definedBy("org.mwolff.api.appversion.infrastructure..")
            .whereLayer("web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("application")
            .mayOnlyBeAccessedByLayers("web")
            .whereLayer("infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("application", "web", "infrastructure");
    rule.check(appVersionClasses);
  }

  @Test
  void domainShouldNotImportSpringJpaOrJakarta() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.appversion.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
            .as("Domain must stay framework-free (no Spring, Jakarta, Hibernate)");
    rule.check(appVersionClasses);
  }
}
