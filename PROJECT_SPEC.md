# W2Full — Project Specification

> **Fonte di verità corrente.** Restano normativi tutti i requisiti, le decisioni, i vincoli di firma e le evidenze presenti nella cronologia Git fino a `main` `1fbc3debf02e68a5009003f5e0bc7b8d5a06167f`, inclusi i contratti e le evidenze RC3 documentati in quella revisione, salvo quanto esplicitamente sostituito qui.

## Stato progetto

- M0–M5: **chiuse**.
- Distribuzione GitHub Releases: **chiusa**.
- M6 — notifiche soglia: **prossimo checkpoint autorizzato; non ancora implementato**.
- M7.1 — filtri Stazioni: **chiusa e verificata su Galaxy S25**.
- M7.2 RC1/RC2: **FAIL UX**, sostituita da M7.4.
- M7.4 RC1: **FAIL UX su Galaxy S25**.
- M7.4 RC2: **PASS UX parziale / FAIL funzionale**.
- M7.4 RC3 `v0.5.3-m7.4-rc3`: **[x] chiusa — PASS reale Galaxy S25 il 5 settembre 2026**.

# Chiusura M7.4 RC3

## Release verificata

La Release normativa resta `v0.5.3-m7.4-rc3`, costruita dall'esatto sorgente:

`bcb02e9a1e1c1476a26873bf209356a4a721b677`

Evidenze già registrate e confermate:
- Android CI sorgente Release `33961085355`, job `101292966146`: **SUCCESS**;
- Android Release run `33961205244`, job `101293280752`: **SUCCESS**;
- Release ID `383216022`;
- asset ID `545706233`;
- APK `w2full-v0.5.3-m7.4-rc3-debug.apk`;
- APK SHA-256 `53ff5d981ee752930f65edf197e460623c7c16e417914725404cadc08fa6f752`;
- certificato SHA-256 `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- tag lightweight `v0.5.3-m7.4-rc3` direttamente sul commit sorgente sopra indicato;
- workflow Release permanente su `main` resta tag-only `v*`.

## PASS dispositivo

L'utente ha installato e provato RC3 sul Samsung Galaxy S25 e il 5 settembre 2026 ha confermato: **"Adesso va bene"**.

Il PASS chiude i gate dispositivo M7.4 per:
- aggiornamento sopra RC2 senza regressioni bloccanti riportate;
- posizione corrente leggibile e comportamento Stazioni accettato;
- distanze/raggio operativi;
- Storico con assi prezzo/date;
- selezione del giorno e dettaglio prezzi;
- zoom/pan/reset del grafico;
- layout fixed-screen, preferiti e Impostazioni accettati per la fase funzionale corrente.

M7.4 è quindi **chiusa**. Non sono richieste altre RC M7.4 salvo regressioni future.

# Direzione successiva autorizzata

Dopo la chiusura M7.4 si torna al piano funzionale e si procede con **M6 — notifiche soglia**, un checkpoint alla volta e con contratto spec-first prima di codice/config.

L'utente ha inoltre deciso che la **rifinitura finale dell'interfaccia** verrà eseguita alla fine del piano funzionale. Fino a quel checkpoint:
- non ridisegnare le schermate già accettate salvo necessità funzionale/accessibilità o regressione;
- privilegiare implementazione, affidabilità, dati e test;
- mantenere il principio già approvato: nelle schermate principali evitare page-scroll; nelle Stazioni scorre solo la lista risultati.

# Requisito UI differito già approvato

Resta aperta issue #1: pulsante `Indicazioni` su ogni card stazione tramite intent verso Google Maps/app mappe compatibile, usando coordinate MIMIT valide e fallback indirizzo, senza Maps SDK a pagamento.

Questo requisito e l'eventuale polish estetico complessivo verranno affrontati nel checkpoint finale UI, salvo diversa autorizzazione esplicita.

# Vincoli invarianti

- app gratuita, locale, senza account/cloud proprietario o servizi a pagamento;
- Kotlin + Jetpack Compose + Material 3;
- target Samsung Galaxy S25;
- APK distribuiti solo tramite GitHub Releases;
- firma debug persistente e aggiornabile invariata;
- certificato SHA-256 atteso `bd7e570922bbadbe22d553bade91493d6309172a8b8d46e317db98f5f0b66265`;
- public key SHA-256 attesa `90e1ce512cd08a6d177bdb8199d3228ff6fb0e81adb625ad43275fc275963313`;
- `PROJECT_SPEC.md` prima del codice/config per ogni requisito non già coperto;
- un obiettivo alla volta;
- nessun test/build/commit simulato;
- nessuna manutenzione CI non necessaria al checkpoint attivo.

# Regola di avanzamento

Questo commit chiude soltanto M7.4. Deve superare Android CI reale su `main` prima di iniziare modifiche M6. Dopo quel gate, M6 parte con un nuovo contratto spec-first e una propria validazione branch → PR → `main` → Release → Galaxy S25.
