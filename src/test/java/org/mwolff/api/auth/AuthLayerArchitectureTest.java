package org.mwolff.api.auth;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * Hexagonal-Layer-Regeln fuer die auth-Komponente (#133, vormals flach unter {@code
 * org.mwolff.api.auth}).
 *
 * <p>Die Komponente besitzt bewusst <strong>keine</strong> domain-/application-Schicht: sie ist ein
 * reiner Sicherheits-Adapter. Es bleiben zwei unabhaengige Schichten — der REST-Adapter {@code
 * auth.web} ({@code MeController}, {@code MeResponse}) und das Security-Wiring {@code
 * auth.infrastructure} ({@code SecurityConfig}, {@code JwtAuthoritiesConverter}). Keine der beiden
 * darf von der anderen abhaengen: das Security-Wiring koppelt nicht an den Controller, und der
 * Controller kennt das Security-Wiring nicht.
 */
class AuthLayerArchitectureTest {

  private static JavaClasses authClasses;

  @BeforeAll
  static void importAuthClasses() {
    authClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api.auth");
  }

  @Test
  void layersShouldRespectHexagonalTopology() {
    final ArchRule rule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("web")
            .definedBy("org.mwolff.api.auth.web..")
            .layer("infrastructure")
            .definedBy("org.mwolff.api.auth.infrastructure..")
            .whereLayer("web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure")
            .mayNotBeAccessedByAnyLayer();
    rule.check(authClasses);
  }

  @Test
  void webAdapterShouldNotWireSpringSecurity() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.auth.web..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.security.config..")
            .as("Security-Wiring darf nicht im Web-Adapter (MeController) liegen")
            .because(
                "die Filter-Chain-Konfiguration gehoert in auth.infrastructure, nicht in den "
                    + "REST-Controller (#133)");
    rule.check(authClasses);
  }
}
