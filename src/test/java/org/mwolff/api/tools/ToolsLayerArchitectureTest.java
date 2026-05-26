package org.mwolff.api.tools;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * Hexagonal-Layer-Regeln für die tools-Komponente.
 *
 * <p>Erzwingt die strikten Abhängigkeitsrichtungen aus Review #58 P1.4: Domain hat nichts
 * Spring-/JPA-/Jakarta-Frameworks, Application kennt kein Web/Servlet, Web hängt nicht an
 * Infrastructure, Infrastructure hängt nicht an Web/Application. Layer-Topology:
 *
 * <pre>
 *   web ─┐
 *        ├─► application ─► domain ◄─── infrastructure
 *   web ─┘
 * </pre>
 */
class ToolsLayerArchitectureTest {

  private static JavaClasses toolsClasses;

  @BeforeAll
  static void importToolsClasses() {
    toolsClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api.tools");
  }

  @Test
  void layersShouldRespectHexagonalTopology() {
    final ArchRule rule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("domain")
            .definedBy("org.mwolff.api.tools.domain..")
            .layer("application")
            .definedBy("org.mwolff.api.tools.application..")
            .layer("web")
            .definedBy("org.mwolff.api.tools.web..")
            .layer("infrastructure")
            .definedBy("org.mwolff.api.tools.infrastructure..")
            .whereLayer("web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("application")
            .mayOnlyBeAccessedByLayers("web")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("application", "web", "infrastructure");
    rule.check(toolsClasses);
  }

  @Test
  void domainShouldNotImportSpringOrJakarta() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.tools.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..", "jakarta..", "org.hibernate..", "org.apache.tika..")
            .as("Domain must stay framework-free (no Spring, Jakarta, Hibernate, Tika)")
            .because(
                "the inner hexagon must not know about transport, persistence or upload tooling");
    rule.check(toolsClasses);
  }

  @Test
  void applicationShouldNotImportSpringWebOrServlet() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.tools.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework.web..", "jakarta.servlet..", "org.apache.tika..")
            .as("Application layer must not see Spring Web / Servlet API / Tika")
            .because("use-cases speak in terms of domain ports, not transport details");
    rule.check(toolsClasses);
  }

  @Test
  void domainAndApplicationShouldNotReferenceMultipartFile() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage(
                "org.mwolff.api.tools.domain..", "org.mwolff.api.tools.application..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.web.multipart.MultipartFile")
            .as("MultipartFile must stay inside the web adapter")
            .because(
                "tying inner hexagon to Spring's MultipartFile defeats the port abstraction "
                    + "(Review #58 P1.3)");
    rule.check(toolsClasses);
  }
}
