# SEW5 Jahresprojekt - Buchanalyse


![Java CI with Maven](https://github.com/078071/SEWProjektOzea/actions/workflows/maven.yml/badge.svg)

##  Projektbeschreibung
Dieses Projekt analysiert Buchdaten aus einer REST-API, verarbeitet sie parallel, speichert die Ergebnisse in einer PostgreSQL-Datenbank und sendet sie anschließend an eine API. Die Verarbeitung beinhaltet:
- **Wortzählung** mit und ohne Stoppwörter
- **Regex-Analyse** für lange Wörter und das Wort "Mensch"
- **Datenbank-Speicherung** in PostgreSQL (Supabase)
- **REST-API Kommunikation** (GET + POST mit Basic Auth)
- **Multithreading** zur parallelen Verarbeitung
- **Logging & Fehlerbehandlung** mit `java.util.logging`
- **CSV-Export** der Ergebnisse

## 📂 Projektstruktur
```
📺 SEWProjektOzea
 ├📄 LICENSE.txt
 ├📄 README.md
 ├🗂 src/main/java/org/example
 │ ├📄 Main.java          # Hauptprogramm (Datenverarbeitung)
 │ ├📄 BookAnalysis.java  # Analysiert die Buchtexte
 │ ├📄 BookAnalysisTask.java # Multithreading mit Callables
 │ ├📄 BookAnalysisTest.java # JUnit-Tests
 │ └📄 pom.xml            # Maven-Build
 ├🗂 target               # Kompilierte Dateien
 └📄 results.csv          # Generierte CSV-Datei mit Ergebnissen
```

---

## 🛠 Installation & Setup
1. **Repository klonen**
   ```sh
   git clone https://github.com/078071/SEWProjektOzea.git
   cd SEWProjektOzea
   ```
2. **Abhängigkeiten installieren** (Maven)
   ```sh
   mvn clean install
   ```
3. **Datenbankverbindung setzen**  
   Stelle sicher, dass deine Supabase-Datenbank in `Main.java` korrekt konfiguriert ist.

4. **Programm ausführen**
   ```sh
   mvn exec:java -Dexec.mainClass="org.example.Main"
   ```

---

##  Funktionalität
###  Daten aus der API abrufen
- API-Endpunkt: [`https://htl-assistant.vercel.app/api/projects/sew5`](https://htl-assistant.vercel.app/api/projects/sew5)
- Die API liefert JSON-Daten mit **id**, **title** und **text** eines Buches.

###  Datenanalyse mit `BookAnalysis`
- **Gesamtwortanzahl**
- **Wortanzahl ohne Stoppwörter** (`["und", "oder", "der", "die", "das", "ein", "eine"]`)
- **Anzahl des Wortes "Mensch"**
- **Lange Wörter (≥ 19 Zeichen)**

###  Speicherung in PostgreSQL
- Ergebnisse werden in die Tabelle `results` geschrieben.
- Duplicate-Einträge werden vermieden.

###  REST-API Verarbeitung
- **GET**: Holt Buchdaten.
- **POST**: Sendet Ergebnisse zur API (`Basic Auth` erforderlich).
- **Antwort**: `200 OK` bei Erfolg, `400` bei fehlerhaften Daten.

---

##  Testing & Qualitätssicherung
- **JUnit-Tests** (`BookAnalysisTest.java`)
- **Automatische Tests** via GitHub Actions (`maven.yml` im `.github/workflows`-Ordner)
- **Fehlermeldungen** im Logging (INFO, WARNING, SEVERE)
- **Code-Dokumentation** mit JavaDoc

---

##  Lizenz
Dieses Projekt ist unter der **MIT-Lizenz** lizenziert. Siehe [LICENSE.txt](LICENSE.txt) für Details.

