````markdown
# FPP-Chatroom – Meilenstein 1

Fortgeschrittenes Programmierpraktikum – Meilenstein 1  
Thema: Entwicklung eines einfachen Kommunikationsprogramms (Client–Server-Chat) in Java.

Dieses Projekt implementiert ein textbasiertes Chat-System mit einem zentralen Server und mehreren Konsolen-Clients.  
Es erfüllt die Anforderungen des 1. Meilensteins:

- Multi-Client-Server mit Java-Sockets
- Registrierung und Login mit Benutzername/Passwort
- Verwaltung aktuell eingeloggter Benutzer
- Broadcast von Chat-Nachrichten
- Benachrichtigungen beim Beitreten/Verlassen von Benutzern
- reine Konsoleninteraktion (kein GUI)
````

## 1. Projektstruktur

Maven-Projekt mit einem Package:

```text
src/
 └─ main/
    ├─ java/
    │   └─ de/uni_jena/fpp/chatroom/
    │       ├─ ChatServerMain.java
    │       ├─ ChatServer.java
    │       ├─ ClientHandler.java
    │       ├─ ChatClientMain.java
    │       ├─ ChatClient.java
    │       ├─ Config.java
    │       ├─ Protocol.java
    │       └─ User.java
    └─ resources/
        └─ chatroom.properties
```

**Wichtige Klassen in Kürze:**

* **`ChatServerMain`**
  Einstiegspunkt für den Server.

* **`ChatServer`**

  * Verwaltet `ServerSocket` und akzeptiert neue Verbindungen.
  * Hält:

    * Liste aller aktiven Verbindungen (`ClientHandler`)
    * `registeredUsers` (registrierte Nutzer, im Speicher)
    * `loggedInClients` (aktuell eingeloggte Nutzer)
  * Stellt Broadcast-Funktionen bereit:

    * `broadcastChat(from, text)`
    * `broadcastUserJoined(username, except)`
    * `broadcastUserLeft(username)`
    * `broadcastInfo(text)`

* **`ClientHandler`** (`extends Thread`)

  * Wird pro Client-Verbindung erzeugt.
  * Liest Befehle des Clients und verarbeitet:

    * `REGISTER`, `LOGIN`, `WHO`, `MSG`, `LOGOUT`
  * Hält den Login-Zustand:

    * `User user` (null, solange nicht eingeloggt)
    * `displayName` (standardmäßig `client-<id>`, nach Login = Username)
  * Informiert beim Verbindungsende den Server, entfernt sich aus den Listen und löst `USER_LEFT`-Broadcast aus.

* **`ChatClientMain`**
  Einstiegspunkt für den Konsolen-Client.

* **`ChatClient`**

  * Baut eine Verbindung zum Server auf.
  * Startet einen **Listener-Thread**, der Servernachrichten liest und auf der Konsole ausgibt.
  * Liest Benutzereingaben von `System.in` und setzt diese in Protokoll-Befehle um.

* **`Protocol`**

  * Zentrale Definition aller Protokoll-Kommandos und Hilfsmethoden zum Bau und Parsen von Nachrichten.
  * Vermeidet „magische Strings“ im Code.

* **`User`**

  * Einfache Datenklasse mit `username` und `password` (im Klartext, da Persistenz erst in späteren Meilensteinen relevant ist).

* **`Config`**

  * Lädt die Konfiguration aus `chatroom.properties` (Host & Port).
  * Stellt `Config.getServerHost()` und `Config.getServerPort()` bereit.

---

## 2. Konfiguration

Datei: `src/main/resources/chatroom.properties`

```properties
server.host=localhost
server.port=5001
```

* `server.host` – Hostname, den der Client standardmäßig verwendet
* `server.port` – Port, auf dem der Server lauscht

Der Port kann hier unkompliziert angepasst werden (z. B. falls `5000` bereits belegt ist).

---

## 3. Kommunikationsprotokoll

Der Nachrichtenaustausch erfolgt textbasiert über `DataInputStream.readUTF()` / `DataOutputStream.writeUTF()`.

Allgemeines Format:

```text
COMMAND [PARAM1] [PARAM2] [REST_TEXT...]
```

### 3.1 Client → Server

**Registrierung**

```text
REGISTER <username> <password>
```

Antworten:

```text
REGISTER_OK
REGISTER_FAILED <reason>   # z.B. USERNAME_TAKEN
```

---

**Login**

```text
LOGIN <username> <password>
```

Mögliche Antworten:

```text
LOGIN_OK
LOGIN_FAILED <reason>      # z.B. INVALID_CREDENTIALS, ALREADY_LOGGED_IN
USER_LIST <user1,user2,...>
INFO Willkommen, <username>!
```

Zusätzlich erhalten alle anderen eingeloggten Clients:

```text
USER_JOINED <username>
```

---

**Nachricht an alle (Broadcast)**

```text
MSG <text...>
```

* Nur möglich, wenn der Client eingeloggt ist.
* Server verteilt:

```text
CHAT <from> <text...>
```

---

**Liste eingeloggter Benutzer**

```text
WHO
```

Antwort:

```text
USER_LIST <user1,user2,...>
```

---

**Logout / Verbindung beenden**

```text
LOGOUT
```

Antwort:

```text
INFO Bye.
```

Server entfernt den Benutzer aus `loggedInClients` und informiert andere:

```text
USER_LEFT <username>
```

---

### 3.2 Server → Client

* `REGISTER_OK`

* `REGISTER_FAILED <reason>`

* `LOGIN_OK`

* `LOGIN_FAILED <reason>`

* `USER_LIST <user1,user2,...>`

* `USER_JOINED <username>`

* `USER_LEFT <username>`

* `CHAT <from> <text...>` – Chat-Nachricht eines anderen/gleichen Benutzers

* `INFO <text...>` – allgemeine Informationsnachricht

* `ERROR <text...>` – Fehlermeldung bei falscher Verwendung des Protokolls

---

## 4. Bedienung – Konsolenclient

Der `ChatClient` unterstützt einfache Befehle, die alle mit `/` beginnen:

```text
/register <username> <password>
/login <username> <password>
/who
/msg <text>
/logout
/quit
```

* **`/register`** – legt einen neuen Benutzer an (nur im Serverspeicher für die Dauer der Laufzeit)
* **`/login`** – meldet sich mit Benutzernamen/Passwort an
* **`/who`** – zeigt alle aktuell eingeloggten Benutzer
* **`/msg`** – sendet eine Nachricht an alle eingeloggten Benutzer
* **`/logout`** – meldet sich vom Server ab
* **`/quit`** – beendet den Clientprozess

Eingaben **ohne `/`** werden automatisch als Chat-Nachricht gesendet (`MSG <text>`).

---

## 5. Build & Ausführung

### Voraussetzungen

* Java 21 (oder kompatible Version, passend zu `maven.compiler.source/target`)
* Maven

### 5.1 Build mit Maven

Im Projektordner:

```bash
mvn clean package
```

Dadurch entstehen:

* kompillierte Klassen in `target/classes`
* Jar: `target/fpp-chatroom-1.0-SNAPSHOT.jar`

---

### 5.2 Start über Terminal (mit Jar)

**Server starten:** 
mvn clean compile

```bash
java -cp target/fpp-chatroom-1.0-SNAPSHOT.jar de.uni_jena.fpp.chatroom.ChatServerMain
```

**Client starten (in separatem Terminal):**

```bash
java -cp target/fpp-chatroom-1.0-SNAPSHOT.jar de.uni_jena.fpp.chatroom.ChatClientMain
```

Es können mehrere Clients parallel gestartet werden (je ein Terminal).

Der Client verbindet sich dabei mit `server.host` / `server.port` aus `chatroom.properties`.

---

### 5.3 Start über IntelliJ IDEA

* `ChatServerMain` als „Application“ Run-Configuration anlegen und starten.
* Mehrere `ChatClientMain`-Run-Configurations (oder mehrfach starten) für mehrere Clients.

---

## 6. Test-Szenario für den Meilenstein

**Empfohlener Ablauf für die Bewertung:**

1. **Server starten**

   * Es erscheint:
     `Starte ChatServer (Multi-Client) auf Port <port>`
     `Warte auf eingehende Clients ...`

2. **Client 1 starten**
   Beispiel-Eingaben:

   ```text
   /register alice 123
   /login alice 123
   /msg Hallo, ich bin Alice!
   ```

   Erwartung:

   * `REGISTER_OK`
   * `LOGIN_OK`
   * `USER_LIST ...`
   * eigene Chat-Nachricht wird angezeigt: `[alice] Hallo, ich bin Alice!`

3. **Client 2 starten**

   ```text
   /register bob 123
   /login bob 123
   /who
   /msg Hi Alice!
   ```

   Erwartung:

   * Alice sieht: `[bob] Hi Alice!`
   * Bob sieht: `[bob] Hi Alice!`
   * Beide sehen sinnvolle `USER_LIST`-Ausgaben.
   * Beim Login von Bob erscheint bei Alice: `[INFO] bob hat den Chat betreten.`

4. **Logout & Verbindungsabbruch**

   * In Client 2: `/logout` oder `/quit`
   * Alice sieht die Nachricht: `bob hat den Chat verlassen.`

---

## 7. Erfüllte Anforderungen Meilenstein 1

* **Konsolenbasierter Client & Server in Java**
* **Verwendung von Sockets** (`ServerSocket`, `Socket`) und Streams (`DataInputStream` / `DataOutputStream`)
* **Multi-Client-Unterstützung** mittels `ClientHandler`-Threads
* **Userverwaltung im Speicher**:

  * Registrierung (Username/Passwort)
  * Login mit Plausibilitätsprüfung
  * Verwaltung eingeloggter Benutzer
* **Nachrichtenaustausch**:

  * Broadcast von Chat-Nachrichten
  * Benutzerliste abrufbar (`WHO`)
  * Join/Leave-Benachrichtigungen
* **Fehlerbehandlung**:

  * Doppelte Usernamen bei Registrierung
  * Login mit falschen Credentials
  * Chat ohne vorherigen Login
  * Verbindungsabbruch (EOF) wird vom Server sauber behandelt

---

## 8. Einschränkungen & Ausblick

Einschränkungen von Meilenstein 1:

* Benutzer werden nur **im RAM** verwaltet (keine Datei-/Datenbankpersistenz).
* Kein GUI (ausschließlich Konsoleninteraktion).
* Keine Verschlüsselung (Kommunikation im Klartext).
* Keine Trennung von öffentlichen/privaten Chat-Räumen.

Mögliche Erweiterungen für weitere Meilensteine:

* Persistente Speicherung der Nutzeraccounts
* GUI-Client
* Unterstützung für private Nachrichten und Chat-Räume
* Erweiterte Protokollstruktur (z. B. Objektserialisierung)

---

