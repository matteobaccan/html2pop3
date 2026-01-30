# html2pop3

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0ab2f441777d4160bfc2f6829bdcfc8c)](https://www.codacy.com/app/matteobaccan/html2pop3?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=matteobaccan/html2pop3&amp;utm_campaign=Badge_Grade)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/matteobaccan/html2pop3.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/matteobaccan/html2pop3/context:java)
[![Coverage Status](https://coveralls.io/repos/github/matteobaccan/html2pop3/badge.svg?branch=master)](https://coveralls.io/github/matteobaccan/html2pop3?branch=master)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/matteobaccan/html2pop3.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/matteobaccan/html2pop3/alerts/)
[![Build Status](https://travis-ci.org/matteobaccan/html2pop3.svg?branch=master)](https://travis-ci.org/matteobaccan/html2pop3)
[![security status](https://www.meterian.io/badge/gh/matteobaccan/html2pop3/security)](https://www.meterian.io/report/gh/matteobaccan/html2pop3)
[![stability status](https://www.meterian.io/badge/gh/matteobaccan/html2pop3/stability)](https://www.meterian.io/report/gh/matteobaccan/html2pop3)
[![DepShield Badge](https://depshield.sonatype.org/badges/matteobaccan/html2pop3/depshield.svg)](https://depshield.github.io)
![Meterian vulnerability scan workflow](https://github.com/matteobaccan/html2pop3/workflows/Meterian%20vulnerability%20scan%20workflow/badge.svg)
[![CodeQL](https://github.com/matteobaccan/html2pop3/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/matteobaccan/html2pop3/actions/workflows/codeql-analysis.yml)
[![StyleCI](https://github.styleci.io/repos/179081117/shield?branch=master)](https://github.styleci.io/repos/179081117?branch=master)

[![Email.it plugin check](https://github.com/matteobaccan/html2pop3/actions/workflows/maven.yml/badge.svg)](https://github.com/matteobaccan/html2pop3/actions/workflows/maven.yml)

<a href="https://github.com/matteobaccan/html2pop3/stargazers"><img src="https://img.shields.io/github/stars/matteobaccan/html2pop3" alt="Stars Badge"/></a>
<a href="https://github.com/matteobaccan/html2pop3/network/members"><img src="https://img.shields.io/github/forks/matteobaccan/html2pop3" alt="Forks Badge"/></a>
<a href="https://github.com/matteobaccan/html2pop3/pulls"><img src="https://img.shields.io/github/issues-pr/matteobaccan/html2pop3" alt="Pull Requests Badge"/></a>
<a href="https://github.com/matteobaccan/html2pop3/issues"><img src="https://img.shields.io/github/issues/matteobaccan/html2pop3" alt="Issues Badge"/></a>
<a href="https://github.com/matteobaccan/html2pop3/graphs/contributors"><img alt="GitHub contributors" src="https://img.shields.io/github/contributors/matteobaccan/html2pop3?color=2b9348"></a>
<a href="https://github.com/matteobaccan/html2pop3/blob/master/LICENSE"><img src="https://img.shields.io/github/license/matteobaccan/html2pop3?color=2b9348" alt="License Badge"/></a>

## Descrizione

HTML2POP3 è un server virtuale POP3, SMTP e NNTP scritto in Java.
Questo progetto nasce con l'obiettivo di permettere l'accesso a caselle di posta elettronica (che normalmente offrono solo accesso webmail o proprietario) tramite client di posta standard che utilizzano i protocolli POP3 e SMTP (come Outlook, Thunderbird, Apple Mail, ecc.).

In pratica, HTML2POP3 agisce come un gateway: il tuo client di posta si connette a HTML2POP3 (che gira localmente sulla tua macchina), e HTML2POP3 si occupa di collegarsi al sito web del provider, scaricare le email, e fornirle al tuo client tramite protocollo standard.

## Funzionalità Principali

*   **Server POP3**: Scarica le email da diversi provider webmail.
*   **Server SMTP**: Invia email tramite i provider supportati.
*   **Server NNTP**: Supporto base per newsgroup.
*   **Gestione RSS**: Possibilità di leggere feed RSS come se fossero email.
*   **Filtri Avanzati**: Configurazione di filtri per IP, Plugin e Utente per gestire l'accesso e la sicurezza.
*   **Supporto Proxy**: Configurabile per funzionare dietro un proxy.
*   **Tunneling**: Supporto per HTTP Tunneling.
*   **Multi-piattaforma**: Essendo scritto in Java, funziona su Windows, Linux e macOS.

## Provider Supportati (Plugin)

Il supporto per i vari provider è implementato tramite plugin. Tra i servizi supportati troviamo:

*   Email.it
*   Fastwebnet
*   Gmail
*   Hotmail / Outlook
*   Infinito
*   Libero
*   Linux.it
*   Supereva
*   Tele2
*   TIM
*   Tin.it
*   Tiscali
*   Virgilio
*   RSS Feed

## Installazione e Utilizzo

### Prerequisiti
*   Java Runtime Environment (JRE) 11 o superiore installato.

### Avvio
1.  Scarica l'ultima [release](https://github.com/matteobaccan/html2pop3/releases) o compila il progetto dai sorgenti.
2.  Esegui il file `html2pop3.jar` (o utilizza gli script `.bat` / `.sh` forniti).
    ```bash
    java -jar target/HTML2POP3-3.1.0.jar
    ```

### Configurazione del Client di Posta
Per utilizzare HTML2POP3, configura il tuo client di posta elettronica come segue:
*   **Server POP3**: `127.0.0.1` (o `localhost`)
*   **Porta POP3**: `110` (default)
*   **Server SMTP**: `127.0.0.1` (o `localhost`)
*   **Porta SMTP**: `25` (default)
*   **Nome Utente**: Il tuo indirizzo email completo (es. `nome.cognome@libero.it`). HTML2POP3 userà il dominio per selezionare il plugin corretto.
*   **Password**: La password della tua casella email.
*   **Sicurezza connessione**: Nessuna (HTML2POP3 gestisce la connessione sicura con il provider, la connessione locale può essere in chiaro).

## Configurazione Avanzata

Il file `config.cfg` permette di personalizzare il comportamento del server. Alcune opzioni principali:

*   `host`: Indirizzo di ascolto (default `127.0.0.1`).
*   `port`: Porta POP3 (default `110`).
*   `portsmtp`: Porta SMTP (default `25`).
*   `portnntp`: Porta NNTP (default `119`).
*   `delete`: Se impostato a `true`, cancella le email dal server remoto dopo averle scaricate (comportamento standard POP3).
*   `debug`: Abilita il log di debug per la risoluzione dei problemi.
*   `proxyhost`, `proxyport`: Configurazione dell'eventuale proxy in uscita.

## Licenza

Questo progetto è distribuito sotto licenza **GPL v3**. Vedi il file [LICENSE](LICENSE) per maggiori dettagli.

## Autore

Matteo Baccan - [www.baccan.it](https://www.baccan.it)
