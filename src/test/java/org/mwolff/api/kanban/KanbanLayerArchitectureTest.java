package org.mwolff.api.kanban;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/** Hexagonal-Layer-Regeln für die Kanban-Komponente, identisch zur Dashboard-Konvention. */
class KanbanLayerArchitectureTest {

  private static JavaClasses kanbanClasses;

  @BeforeAll
  static void importKanbanClasses() {
    kanbanClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api.kanban");
  }

  @Test
  void layersShouldRespectHexagonalTopology() {
    final ArchRule rule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("domain")
            .definedBy("org.mwolff.api.kanban.domain..")
            .layer("application")
            .definedBy("org.mwolff.api.kanban.application..")
            .layer("web")
            .definedBy("org.mwolff.api.kanban.web..")
            .layer("infrastructure")
            .definedBy("org.mwolff.api.kanban.infrastructure..")
            .whereLayer("web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("application")
            .mayOnlyBeAccessedByLayers("web", "infrastructure")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("application", "web", "infrastructure");
    rule.check(kanbanClasses);
  }

  @Test
  void domainShouldNotImportSpringJpaOrJakarta() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.kanban.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..")
            .as("Domain must stay framework-free (no Spring, Jakarta, Hibernate)")
            .because("the inner hexagon must not know about persistence or transport");
    rule.check(kanbanClasses);
  }

  @Test
  void applicationShouldNotImportSpringWebOrJpa() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("org.mwolff.api.kanban.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework.web..", "jakarta.servlet..", "jakarta.persistence..")
            .as("Application layer must not see Spring Web or JPA")
            .because("use-cases speak in terms of ports — transport and persistence are adapters");
    rule.check(kanbanClasses);
  }
}
