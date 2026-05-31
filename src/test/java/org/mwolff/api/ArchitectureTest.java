package org.mwolff.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Project-wide architecture guardrails. The rules describe the minimum every domain has to honour;
 * layered (hexagonal) rules for individual domains land alongside their feature implementations.
 */
class ArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importProductionClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api");
  }

  @Test
  void shouldUseConstructorInjectionOnly() {
    final ArchRule rule =
        fields()
            .should()
            .notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .as("Constructor injection only — never @Autowired on fields")
            .because(
                "field injection hides dependencies, breaks immutability, and is hard to test; "
                    + "use constructor injection (CLAUDE-java.md §6)");
    rule.check(productionClasses);
  }

  @Test
  void controllersShouldNotDependOnSpringDataRepositoriesDirectly() {
    final ArchRule rule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .areAssignableTo("org.springframework.data.repository.Repository")
            .as("Controllers must not depend on Spring Data repositories directly")
            .because(
                "controllers belong in the web adapter and should only call application services");
    rule.check(productionClasses);
  }

  @Test
  void productionCodeShouldNotUseSystemOutOrErr() {
    final ArchRule rule =
        noClasses()
            .should()
            .accessField(System.class, "out")
            .orShould()
            .accessField(System.class, "err")
            .as("Production code must not use System.out / System.err — use SLF4J logging instead")
            .because("CLAUDE-security.md forbids ad hoc stdout logging; loggers are auditable");
    rule.check(productionClasses);
  }

  @Test
  void requestAndResponseDtosShouldLiveInWebOrDtoPackages() {
    final ArchRule rule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Request")
            .or()
            .haveSimpleNameEndingWith("Response")
            .should()
            .resideOutsideOfPackages("..dto..", "..web..")
            .as("DTOs (*Request, *Response) belong in a *.dto or *.web package only")
            .because(
                "HTTP request/response DTOs are web adapters; they must not spread into domain "
                    + "or application layers (CLAUDE-java.md §3.1)");
    rule.check(productionClasses);
  }
}
