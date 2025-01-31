# Comunicazione Chat-Server

Un sistema di chat client-server sicuro implementato in Java.

*Autori: Giovanni Battistelli - Enrico Negretto*

## Panoramica

Questo progetto implementa un sistema di comunicazione client-server sicuro utilizzando Java, con focus particolare sulla sicurezza e sulla gestione efficiente delle connessioni multiple.

## Requisiti
- Java Development Kit (JDK) 8+
- Maven per la gestione delle dipendenze

## Scelta Tecnologica e Motivazioni

La scelta di Java come linguaggio di programmazione è stata guidata da diverse caratteristiche chiave:

- Affidabilità e sicurezza integrate
- Eccellente gestione della concorrenza
- Robusto supporto per la crittografia
- Ricco ecosistema di librerie
- Gestione efficiente delle connessioni multiple

## Architettura del Sistema

### Design Generale
Il sistema utilizza un'architettura client-server con gestione centralizzata delle connessioni. Il server implementa un modello multithread, dove ogni client viene gestito da un thread dedicato, permettendo l'elaborazione parallela delle richieste senza blocchi reciproci.

### Componenti Server

Il server gestisce tre funzioni principali:
1. Accettazione delle connessioni in entrata
2. Gestione indipendente della comunicazione
3. Elaborazione e risposta ai messaggi

### Componenti Client

Il client si occupa di:
- Stabilire e mantenere la connessione con il server
- Implementare la crittografia per la protezione dei dati
- Gestire l'invio e la ricezione di messaggi e/o file

## Implementazione Tecnica

### Librerie Utilizzate

Il progetto fa uso delle seguenti librerie Java:
- `java.net.Socket` e `java.net.ServerSocket` per le connessioni TCP
- `javax.crypto` per la crittografia AES
- `Log4j` per logging

### Protocollo di Comunicazione e Sicurezza

La sicurezza è implementata attraverso diversi livelli:

1. Protocollo base:
    - Comunicazione basata su TCP per affidabilità e gestione errori

2. Misure di sicurezza:
    - Crittografia AES-GCM con chiave a 32 byte
    - Protocollo Diffie-Hellman per lo scambio sicuro delle chiavi
    - Derivazione della chiave AES dalla chiave segreta condivisa

## Funzionalità

### Core Features

- Connessione stabile e affidabile client-server
- Sistema di messaggistica con crittografia
- Logging avanzato tramite Log4j
- Gestione concorrente delle connessioni multiple

### Roadmap Futura

Il progetto prevede l'implementazione delle seguenti funzionalità:

- Sistema di trasferimento file
- Interfaccia grafica utente
- Sistema di gestione utenti con database SQL
- Gestione dei permessi utente