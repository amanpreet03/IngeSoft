====================================================
  VISITE GUIDATE MANTOVA  –  Versione 2
====================================================

STRUTTURA CARTELLE
  src/
    model/      – classi di dominio
    storage/    – JsonIO (parser JSON) + Persistenza (lettura/scrittura file)
    controller/ – Controller (logica) + Pianificatore (V3)
    ui/         – MenuConfiguratore, MenuVolontario (V2+), MenuFruitore (V4)
    Main.java   – punto di ingresso
  json/         – file dati (creati/aggiornati dal programma)
  out/          – cartella per i .class compilati

FUNZIONALITÀ INCLUSE
  V1: configuratore – gestione luoghi, tipi di visita, volontari, visualizzazioni
  V2: + volontario  – login, dichiarazione disponibilità, cambio password
  V3: + ciclo mensile – pianificazione, rimozioni a cascata, fasi operative
  V4: + fruitore    – iscrizioni, disdette, visualizzazione visite

CREDENZIALI DI DEMO
  Configuratori:  conf1 / conf1pass     conf2 / admin123 (primo accesso)
  Volontari:      volontario1 / vol1pass
                  volontario2 / vol2pass
                  volontario3 / vol3pass
  Fruitori:       fruitore1 / fruitore1
                  fruitore2 / fruitore2
                  fruitore3 / fruitore3

  Credenziali predefinite per nuovo configuratore: admin / admin123

COMPILAZIONE ED ESECUZIONE
  # dalla cartella VisiteGuidate_V2/
  javac -d out -sourcepath src src/Main.java
  java -cp out Main

FORMATO DATE NEI JSON
  GG-MM-AAAA  (es. 25-07-2025)

NOTE
  - I file JSON vengono aggiornati automaticamente dopo ogni operazione.
  - Il salvataggio è immediato: non serve un comando esplicito.
  - Se un file JSON è assente, viene ricreato vuoto al primo avvio.
====================================================
