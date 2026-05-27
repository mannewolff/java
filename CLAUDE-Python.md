# Python Entwicklungsregeln

Dieses Dokument definiert die verbindlichen Regeln für Python-Entwicklung. Die Regeln sind bewusst streng formuliert. Abweichungen müssen im Code dokumentiert und im Code-Review begründet werden.

## Grundprinzipien

1. **Lesbarkeit vor Cleverness.** Code wird häufiger gelesen als geschrieben. Expliziter Code schlägt impliziten Code, immer.
2. **Typsicher von Anfang an.** Jede Funktion, jede Methode, jedes Attribut wird typisiert. Dynamik ist die Ausnahme, nicht die Regel.
3. **Tests sind Teil des Features.** Code ohne Tests ist unvollständig und wird nicht gemerged.
4. **Fehler laut, nicht leise.** Warnungen sind Fehler. Stille Fehlerunterdrückung ist verboten.
5. **Automatisierung statt Disziplin.** Was ein Tool prüfen kann, prüft ein Tool. Keine manuellen Style-Diskussionen im Review.

## Python-Version und Projektsetup

* Minimale unterstützte Python-Version: **3.12** (für neue Projekte 3.13).
* Projektkonfiguration ausschließlich in `pyproject.toml`. Kein `setup.py`, kein `setup.cfg`, keine `requirements.txt` als Single Source of Truth.
* Paketmanager: **uv**. Lockfile (`uv.lock`) wird ins Repository eingecheckt.
* Virtuelle Umgebungen sind Pflicht. Niemals systemweit installieren.

## Typisierung

* `mypy --strict` (oder `pyright` im strict-Modus) ist Pflicht. Das Projekt muss ohne Typfehler durchlaufen.
* Jede öffentliche Funktion und Methode hat vollständige Typannotationen für Parameter und Rückgabewert.
* `Any` ist verboten, außer in begründeten Ausnahmen mit Kommentar (`# type: ignore[...]` mit Begründung).
* `# type: ignore` ohne Fehlercode ist nicht erlaubt. Immer den spezifischen Code angeben.
* Moderne Syntax verwenden: `list[int]` statt `List[int]`, `dict[str, int]` statt `Dict`, `X | None` statt `Optional[X]`.
* Für strukturelle Typen `Protocol` verwenden, keine Duck-Typing-Konventionen.
* Generics werden mit der neuen Syntax (PEP 695) deklariert: `def first[T](items: list[T]) -> T`.
* Pydantic-Modelle oder `dataclass(frozen=True, slots=True)` für Datenstrukturen, keine losen Dictionaries als Datenträger.

## Tests und Testabdeckung

* Testframework: **pytest**. Keine `unittest`-Klassen in neuen Projekten.
* **Minimale Testabdeckung: 90 Prozent Zeilen- und 85 Prozent Branch-Coverage.** Unterschreitung führt zum CI-Fail.
* Kritische Module (Geschäftslogik, Sicherheit, Datenzugriff) verlangen **100 Prozent Coverage**.
* Jede Bugfix-Pull-Request enthält einen Test, der den Bug reproduziert hätte.
* Tests sind deterministisch. Keine Abhängigkeit von Reihenfolge, Uhrzeit oder externen Diensten ohne Mock.
* `pytest`-Konfiguration zwingend mit:
  * `--strict-markers`, `--strict-config`
  * `xfail_strict = true`
  * `filterwarnings = ["error"]` (Warnungen werden zu Fehlern)
* Property-based Testing mit **Hypothesis** für Funktionen mit klar definiertem Wertebereich.
* Tests liegen in `tests/`, parallel zur Paketstruktur. Eine Testdatei pro Modul.
* Fixtures sind explizit, keine impliziten Konstanten. Keine globalen Testzustände.

## Linting und Formatierung

* **Ruff** für Linting und Formatierung. Konfiguration in `pyproject.toml`.
* Aktivierte Regelgruppen mindestens: `E, F, W, I, N, UP, B, C4, SIM, RUF, S, A, ARG, PTH, RET, TRY`.
* `line-length = 100`. Keine längeren Zeilen, auch nicht in Kommentaren.
* Imports werden von Ruff sortiert. Manuelle Sortierung ist verboten.
* Keine ungenutzten Imports, keine ungenutzten Variablen, keine toten Code-Pfade.
* `noqa`-Kommentare verlangen einen Regelcode und eine Begründung.

## Code-Struktur

* Funktionen maximal 50 Zeilen, Klassen maximal 200 Zeilen. Überschreitung ist ein Refactoring-Signal.
* Zyklomatische Komplexität pro Funktion maximal 10 (durch Ruff `C901` geprüft).
* Maximal drei Ebenen Verschachtelung. Tiefer bedeutet: extrahieren.
* Eine Funktion macht eine Sache. Wenn der Name ein "und" enthält, ist es zu viel.
* Pfade ausschließlich mit `pathlib.Path`. Kein `os.path`.
* Kontextmanager (`with`) für jede Ressource, die geöffnet wird (Dateien, Verbindungen, Locks).

## Fehlerbehandlung

* Niemals nacktes `except:`. Niemals `except Exception:` ohne sehr guten Grund.
* Spezifische Exceptions fangen, spezifische Exceptions werfen.
* Eigene Exception-Klassen für Domänenfehler. Niemals `ValueError` für Geschäftslogik missbrauchen.
* Exceptions werden geloggt, bevor sie reichend behandelt werden. Stille `pass`-Blöcke sind verboten.
* Keine Kontrollflusslogik über Exceptions (außer dort, wo Python es vorgibt, etwa `StopIteration`).
* `raise X from Y` verwenden, um Ursprungs-Exceptions zu erhalten.

## Logging

* Modul `logging` verwenden, nie `print` für Diagnose.
* Logger pro Modul: `logger = logging.getLogger(__name__)`.
* Keine f-Strings im Log-Call: `logger.info("user %s logged in", user_id)`, nicht `logger.info(f"user {user_id} logged in")`.
* Keine sensiblen Daten loggen (Passwörter, Tokens, personenbezogene Daten ohne Maskierung).

## Dependency-Management

* Jede Abhängigkeit braucht eine Begründung. Lieber Standardbibliothek als Drittabhängigkeit.
* Versionen werden im Lockfile fixiert, nicht in `pyproject.toml`.
* Sicherheitsscans (`pip-audit` oder `uv audit`) laufen in der CI.
* Veraltete Abhängigkeiten werden quartalsweise aktualisiert, kritische Sicherheitsupdates sofort.

## Dokumentation

* Docstrings für jedes öffentliche Modul, jede öffentliche Funktion, jede öffentliche Klasse. Pflicht, nicht optional.
* Stil: **Google** oder **NumPy**, projektweit einheitlich. Wird per Ruff (`D`-Regelgruppe) durchgesetzt.
* Docstrings beschreiben **was** und **warum**, nicht **wie**. Das *Wie* steht im Code.
* Beispiele in Docstrings sind als Doctests ausführbar.
* README enthält: Setup, Tests laufen lassen, Lint laufen lassen, Architektur in einem Absatz.

## Sicherheit

* `bandit` läuft in der CI (oder Ruff mit `S`-Regelgruppe).
* Keine Secrets im Code, keine Secrets in Tests, keine Secrets in Beispielen. Tools wie `gitleaks` in der CI.
* `subprocess`-Aufrufe niemals mit `shell=True` und Benutzereingaben.
* Eingaben aus externen Quellen werden validiert, bevor sie weiterverarbeitet werden. Pydantic für API-Grenzen.
* Kryptografie ausschließlich über `cryptography` oder Standardbibliothek, niemals selbst implementieren.

## Performance und Concurrency

* Profilen vor Optimieren. Mikrooptimierungen ohne Messung sind verboten.
* `asyncio` konsequent oder gar nicht. Kein Mischen von synchronem und asynchronem Code im selben Modul ohne klare Schnittstelle.
* CPU-bound Workload: ab Python 3.13 free-threaded Builds prüfen, sonst `multiprocessing` oder C-Extensions.
* Generators und Iteratoren für große Datenmengen, keine vollständige Materialisierung in Listen.

## CI/CD

* Pre-Commit-Hooks zwingend installiert. Mindestens: ruff, ruff-format, mypy, trailing-whitespace, end-of-file-fixer.
* CI-Pipeline führt aus: Format-Check, Lint, Typecheck, Tests mit Coverage, Security-Scan, Dependency-Audit.
* Pipeline ist rot bei jedem Fehler. Keine "ignorierten" oder "manuell überspringbaren" Checks.
* Pull-Requests werden nicht gemerged, solange die Pipeline rot ist.

## Code-Review

* Jeder Code wird von mindestens einer anderen Person reviewed.
* Reviewer prüft: Lesbarkeit, Testabdeckung, Sicherheit, API-Design. Style prüft das Tool.
* Diskussionen über Stil im Review sind ein Bug-Signal für die Tool-Konfiguration, nicht für den Autor.
* Keine "Nice-to-have"-Kommentare als Blocker. Trennung zwischen "muss" und "Vorschlag".

## Beispielkonfiguration `pyproject.toml`

```toml
[tool.ruff]
line-length = 100
target-version = "py312"

[tool.ruff.lint]
select = ["E", "F", "W", "I", "N", "UP", "B", "C4", "SIM", "RUF", "S", "A", "ARG", "PTH", "RET", "TRY", "D"]
ignore = ["D203", "D213"]

[tool.ruff.lint.per-file-ignores]
"tests/*" = ["S101", "D"]

[tool.mypy]
python_version = "3.12"
strict = true
warn_unused_ignores = true
warn_redundant_casts = true
warn_return_any = true
disallow_any_generics = true
check_untyped_defs = true
no_implicit_reexport = true

[tool.pytest.ini_options]
minversion = "8.0"
addopts = ["-ra", "--strict-markers", "--strict-config", "--cov", "--cov-branch", "--cov-fail-under=90"]
testpaths = ["tests"]
xfail_strict = true
filterwarnings = ["error"]

[tool.coverage.run]
branch = true
source = ["src"]

[tool.coverage.report]
show_missing = true
skip_covered = false
fail_under = 90
exclude_lines = [
    "pragma: no cover",
    "raise NotImplementedError",
    "if TYPE_CHECKING:",
    "if __name__ == .__main__.:",
]
```

## Schlusswort

Diese Regeln sind kein Selbstzweck. Sie existieren, damit Code in einem Jahr noch verständlich ist, damit Bugs früh auffallen und damit niemand Stil in einem Review diskutieren muss. Wer eine Regel für falsch hält, schlägt eine Änderung dieses Dokuments vor, nicht eine Ausnahme im Code.
