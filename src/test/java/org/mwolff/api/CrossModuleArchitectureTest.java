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
 * <p>Es existieren zwei bewusste Cross-Modul-Abhängigkeiten: {@code ingest → timeseries} (das
 * Ingest-Modul ist ein token-authentifizierter Schreibpfad, der über {@code AddEntryUseCase}
 * Zeitreihen-Einträge anlegt) und {@code image → dashboard} (#202: der Image-Manager fragt über den
 * {@code WidgetImageUsagePort} lesend ab, ob ein Bild von Widgets referenziert wird). Beide Kanten
 * sind unten explizit freigegeben; alles andere ist verboten.
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
            // Bewusste, dokumentierte Ausnahme: Der Image-Manager (#202) fragt über den
            // Dashboard-WidgetImageUsagePort ab, ob ein Bild von Widgets referenziert wird
            // (Lösch-Schutz + „Benutzt in X Widgets"). Reine Lese-Kante image → dashboard.
            .ignoreDependency(
                resideInAPackage("org.mwolff.api.image.."),
                resideInAPackage("org.mwolff.api.dashboard.."))
            .as(
                "Top-Level-Module unter org.mwolff.api dürfen sich nicht gegenseitig referenzieren "
                    + "(Ausnahmen: dokumentierte Kanten ingest → timeseries, image → dashboard)")
            .because(
                "modulübergreifende Zugriffe ohne definierten Port koppeln Features hart aneinander "
                    + "und unterlaufen die Hexagonal-Struktur (Issue #145)");
    rule.check(productionClasses);
  }
}
