package org.mwolff.api;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * Projektweite Cross-Module-Architekturregel. Die per-Modul-Layer-Tests (Tools, TimeSeries, Ingest,
 * Dashboard, Kanban, Auth) prüfen nur die interne Hexagonal-Topologie eines Moduls — sie sehen
 * modulübergreifende Zugriffe nicht, weil sie jeweils nur ihr eigenes Package importieren.
 *
 * <p>Dieser Test schließt die Lücke: Jedes Top-Level-Package unter {@code org.mwolff.api} ist ein
 * eigener Modul-Slice. Module dürfen sich grundsätzlich <strong>nicht</strong> gegenseitig
 * referenzieren — jede neue Kopplung schlägt hier fehl, bis sie als bewusste Ausnahme dokumentiert
 * ist.
 *
 * <p>Stand 2026-06-01 existiert genau eine bewusste Cross-Modul-Abhängigkeit: {@code ingest →
 * timeseries}. Das Ingest-Modul ist ein token-authentifizierter Schreibpfad, der über {@code
 * AddEntryUseCase} Zeitreihen-Einträge anlegt. Diese Kante ist unten explizit freigegeben; alles
 * andere ist verboten.
 */
class CrossModuleArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importProductionClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("org.mwolff.api");
  }

  @Test
  void modulesShouldNotDependOnEachOtherExceptDocumentedEdges() {
    final ArchRule rule =
        SlicesRuleDefinition.slices()
            .matching("org.mwolff.api.(*)..")
            .namingSlices("Modul $1")
            .should()
            .notDependOnEachOther()
            // Bewusste, dokumentierte Ausnahme: Ingest schreibt Zeitreihen-Einträge über den
            // TimeSeries-AddEntryUseCase (token-authentifizierter Ingest-Schreibpfad).
            .ignoreDependency(
                resideInAPackage("org.mwolff.api.ingest.."),
                resideInAPackage("org.mwolff.api.timeseries.."))
            .as(
                "Top-Level-Module unter org.mwolff.api dürfen sich nicht gegenseitig referenzieren "
                    + "(Ausnahme: dokumentierte Kante ingest → timeseries)")
            .because(
                "modulübergreifende Zugriffe ohne definierten Port koppeln Features hart aneinander "
                    + "und unterlaufen die Hexagonal-Struktur (Issue #145)");
    rule.check(productionClasses);
  }
}
