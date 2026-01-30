
## ChatClient
- verwaltet Client-Verbindung (Socket + DataInputStream/DataOutputStream) und kapselt die komplette Client-Logik für GUI + Konsole
- hält das Client-Model (ClientModel) und feuert UI-Events über ChatClientListener (Observer-Pattern)
- connect() -> baut TCP Verbindung auf, erstellt Streams, startet eigenen Listener-Thread (ServerListener), feuert onInfo("Verbunden ...")
- disconnect() -> running=false, socket.close() (unterbricht readUTF), feuert onConnectionClosed()
- Actions für GUI:
    - register/login/createRoom/join/leave/sendMessage/logout -> sendet jeweils Protocol.build... an den Server
    - join() setzt model.currentRoom sofort (optimistisch), leave() setzt currentRoom="Lobby" + sendet LEAVE
- listenToServer() (Listener-Thread) -> while(running) readUTF(), bei EOFException/close -> beendet Loop, feuert onConnectionClosed()
- handleServerMessage():
    - Spezialfall CHAT: split mit max 4 Tokens (CHAT room from text...), schreibt in model.chatLines + onChatMessage()
    - ROOM_LIST: parsePipeList, model.rooms updaten, onRoomsUpdated()
    - ROOM_USERS: parsePipeList, model.usersInCurrentRoom nur wenn room==currentRoom, onUsersUpdated()
    - WARN/INFO/ERROR/BANNED: payload zusammensetzen, ins Model schreiben + passendes Listener-Event; bei BANNED zusätzlich disconnect()
    - Default: unknown message -> ins Model + onInfo()
- send() ist synchronized -> verhindert Race-Conditions wenn UI schnell mehrere Nachrichten sendet
- start()/readConsoleInput() -> optionaler Konsolenmodus (Debug): nimmt /register /login /create /join /leave /msg /logout /quit und nutzt dieselben Action-Methoden wie die GUI

## ChatClientListener
- Interface für das Event-System im Client (Observer-Pattern)
    - ChatClient ist der „Producer“ (feuert Events)
    - GUI/Frames sind „Consumer“ (registrieren sich als Listener)
- alle Methoden sind `default` -> Implementierer müssen nur das überschreiben, was sie wirklich brauchen
- Events für dynamische GUI-Updates:
    - onRoomsUpdated(rooms) -> Raumliste neu rendern
    - onUsersUpdated(room, users) -> Userliste für einen Raum neu rendern
    - onChatMessage(room, from, text) -> neue Chatzeile anzeigen
- Events für Auth-Flow:
    - onLoginOk() / onLoginFailed(reason)
    - onRegisterOk() / onRegisterFailed(reason)
- Status-/Fehlerkanal:
    - onInfo(text), onError(text), onWarn(text), onBanned(reason)
- Connection Lifecycle:
    - onConnectionClosed() -> UI kann Buttons deaktivieren / zurück zur Login-Ansicht / Hinweis anzeigen

## ChatClientMain
- Einstiegspunkt für den Konsolen-Client (Debug/Test-Variante ohne GUI)
- liest Host/Port aus `Config` und erstellt `ChatClient`
- registriert einen `ChatClientListener`, der alle Events einfach auf `System.out` ausgibt
    - INFO/ERROR/WARN/BANNED -> Statusausgaben
    - ROOMS/USERS -> zeigt dynamische Updates vom Server
    - CHAT -> zeigt eingehende Chatnachrichten
    - onConnectionClosed -> merkt, wenn die Verbindung weg ist
- ruft `client.start()` auf
    - startet die Socket-Verbindung + Listener-Thread
    - liest anschließend Konsolenbefehle (`/login`, `/create`, `/join`, `/msg`, …) und sendet sie an den Server

## ChatServer
- TCP Socketprogrammierung
- 1 Thread pro Client
- port des Servers
- running als Flag für die Server-Schleife
- connections -> alle Clienthandler
- rooms: HashMap -> Name, Room-Objekt
- userRepo + logger für Benutzerverwaltung und Logging
- start(): ServerSocket -> ClientHandler wird erstellt und speichert ihn in  Connections
- stop(): ServerSocket schließen -> Clienthandler schließen, state resetten (connections)
    - > Lobby wird persistent wieder angelegt
- registerUser -> valid input, createUser (repo), password wird als char[] verarbeitet
- authenticateUser -> verifyLogin + check if User banned
- warnUser, banUser -> Adminfunktion (ConcurrentHashmap)
- createRoom, joinRoom, leaveRoom -> Userfunktionen
    - > alles wird gebroadcastet an die anderen Clienthandler
- createRoomAsServer + delete... -> Adminfunktion
    - > room aus Map entfernen, broadcasten, alle joinen in Lobby
- Server pusht dauerhaft ROOM_LIST, ROOM_USERS, CHAT, ...

## ChatServerMain
- Einstiegspunkt für den Konsolen-Server (ohne GUI)
- liest den Port aus `Config`
- initialisiert die persistente Benutzerverwaltung:
    - erstellt ein `UserRepository` als `FileUserRepository` (Datei + PBKDF2-Iterations aus `Config`)
- initialisiert das Logging:
    - `ServerLogger` schreibt in Log-Datei + kann (optional) Listener für GUI haben
- erstellt den `ChatServer` mit `port`, `repo`, `logger`
- schreibt einmal die wichtigsten Config-Werte ins Log (`logger.info(...)`)
- ruft `server.start()` auf
    - startet den ServerSocket und nimmt Clients an (je Client ein `ClientHandler`-Thread)

## ClientHandler
- extends Thread
- liest vom Socket DataInputStream.readUTF() und writeUTF()
- hält für jeden Client den Session-State: user, displayName, Room, ...
- run() -> Streams werden angelegt, wartet auf Nachricht und gibt an handleCommand wieter
    - > wenn Client Verbindung schließt EOFException -> loop endet
- handleCommand() -> parst verschiedene commands
- handleRegister() prüft Syntax, sendet REGISTER_OK / FAILED + logInfo
- handleLogin() prüft Syntax, checkt und sendet LOGIN_OK
    - > doppeltes einloggen, banned user, falsche credentials
- handleCreateRoom() -> nach Login, Syntaxcheck
- handleLeave() -> joinRoom(Lobby)
- handleMsg() -> nach Login, ChatServer broadcastet alles
- handleLogout() -> sendet INFO Bye, running = false, cleanup()
- closeNow() bei Bann, oder Serverstop
- cleanUp() -> removeClient, socket schließen, server räumt auf

## ClientModel
- kapselt den kompletten GUI-State des Clients (reines Datenmodell, keine Netzwerklogik)
- hält:
    - rooms (Liste aller Räume vom Server)
    - currentRoom (aktueller Raum des Clients)
    - usersInCurrentRoom (Userliste nur für currentRoom)
    - chatLines (Chat-/Info-/Error-Historie fürs Nachrichtenfenster)
- Thread-Safety: alle Getter/Setter sind synchronized, weil UI-Thread und ServerListener-Thread gleichzeitig darauf zugreifen können
- getRooms/getUsers/getChatLines geben immer Kopien zurück -> UI kann nichts „kaputt-modifizieren“
- setRooms(): ersetzt komplette Roomliste, sortiert alphabetisch (case-insensitive)
- setCurrentRoom(): setzt Raum und leert usersInCurrentRoom, weil die Userliste ab jetzt neu vom Server kommen muss
- setUsersInCurrentRoom(): ersetzt Userliste, sortiert alphabetisch
- addChatLine(): hängt eine Zeile an und begrenzt die History über maxChatLines (Default 500), damit UI nicht unendlich wächst
- setMaxChatLines(): setzt Minimum (>=50) und trimmt ggf. sofort die History
- getRoomsUnmodifiable(): Debug/Test-Helper, gibt eine unmodifiable Copy zurück (zusätzliche Absicherung gegen externe Modifikation)

## Config
- zentrale Konfigurationsklasse (statische Werte, überall per Config.get…() abrufbar)
- lädt beim Programmstart automatisch über static { load(); } (Java Static Initializer)
- liest `chatroom.properties` aus dem Classpath (getResourceAsStream), sonst Defaults + Console-Warnung
- verwaltet folgende Konfig-Werte:
    - serverHost (Default: `localhost`)
    - serverPort (Default: `5001`)
    - usersFile (Default: `data/users.db`) -> Persistenz der Benutzer
    - pbkdf2Iterations (Default: `PasswordUtil.DEFAULT_ITERATIONS`) -> Security-Parameter fürs Hashing
    - serverLogFile (Default: `data/server.log`) -> Logfile des Servers
- parseInt(): robustes Parsing für Zahlenwerte (trim + try/catch), fällt bei Fehlern auf Default zurück
- Getter sind read-only -> Config ist „immutable“ von außen (Werte werden nur beim Laden gesetzt)

## FileUserRepository
- Implementiert `UserRepository` und speichert User dauerhaft in einer Datei (ohne Klartext-Passwörter)
- Hält zusätzlich eine In-Memory-Map `users` (`ConcurrentHashMap`) für schnelle Zugriffe
    - Key = Username, Value = `User`-Objekt (enthält Iterationen, Salt, Hash, banned-Flag)
- Konstruktor
    - `filePath` wird zu `Path`
    - `defaultIterations` wird auf mindestens `10_000` gesetzt
    - lädt beim Start alles aus der Datei via `loadAll()`
- createUser(username, password)
    - synchronized, damit keine Race Conditions beim gleichzeitigen Schreiben entstehen
    - validiert Username + prüft ob bereits vorhanden
    - erzeugt `salt` (`PasswordUtil.newSalt()`)
    - berechnet PBKDF2-Hash (`PasswordUtil.pbkdf2(...)`)
    - legt neuen `User` an (banned=false), speichert ihn in `users`
    - schreibt anschließend die komplette DB-Datei neu (`saveAll()`)
- verifyLogin(username, password)
    - liest User aus `users`
    - prüft Passwort über `PasswordUtil.matches(password, u)`
    - gibt bei Erfolg das `User`-Objekt zurück (inkl. banned-Flag), sonst `null`
- listUsers()
    - gibt sortierte Kopie aller User zurück (case-insensitive nach Username)
- setBanned(username, banned)
    - synchronized, weil Datei geändert wird
    - ersetzt User in der Map durch `old.withBanned(banned)` (immutables Update)
    - persistiert Änderung über `saveAll()`
- loadAll()
    - sorgt dafür, dass der Ordner existiert (`Files.createDirectories`)
    - liest Datei zeilenweise, ignoriert Kommentare/Leerzeilen
    - `parseLine(...)` baut daraus `User`-Objekte und füllt `users`
- saveAll()
    - schreibt atomar:
        - schreibt erst in `-.tmp`
        - dann `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`
    - Format je Zeile: `username;iterations;saltBase64;hashBase64;banned`
    - Base64 wird genutzt, um Salt/Hash als Text speichern zu können
- Validierung
    - `isValidName`: verhindert kaputte Zeilen/Injection ins Dateiformat (`" "`, `"|"`, `";"` sind verboten)
    - `parseLine` ist defensiv: bei Fehlern -> `null` (Zeile wird übersprungen)

## GuiClientMain
- Einstiegspunkt für den GUI-Client
- Liest Host und Port aus `Config` und erstellt einen `ChatClient`
- Ablauf in `main(...)`
    - `ChatClient client = new ChatClient(Config.getServerHost(), Config.getServerPort());`
        - initialisiert die Netzwerk-Komponente (noch ohne GUI)
    - `client.connect();`
        - baut die TCP-Verbindung zum Server auf
        - startet intern den Listener-Thread (`ServerListener`), der Server-Nachrichten empfängt und ins Model/Listener feuert
- Swing-Thread (EDT)
    - `SwingUtilities.invokeLater(...)`
        - sorgt dafür, dass alle GUI-Operationen im Swing Event Dispatch Thread laufen (Swing ist nicht thread-sicher)
    - `LoginFrame login = new LoginFrame(client);`
        - Übergibt den bereits verbundenen `ChatClient` an die Login-GUI, damit Buttons später direkt `client.login(...)` / `client.register(...)` aufrufen können
    - `login.initialize(login);`
        - baut die GUI-Komponenten zusammen und zeigt das Fenster an

## GuiServerMain
- Einstiegspunkt für den GUI-Server
- Baut alle Server-Komponenten zusammen (Config, User-Persistenz, Logging) und startet danach die Server-GUI
- Aufbau der Server-Abhängigkeiten
    - `int port = Config.getServerPort();`
        - Port kommt aus `chatroom.properties` oder Default aus `Config`
    - `UserRepository repo = new FileUserRepository(Config.getUsersFile(), Config.getPbkdf2Iterations());`
        - Persistenz-Schicht für Benutzerkonten (Datei-basiert)
        - Passwort-Hashes via PBKDF2 (Iterationen aus Config)
    - `ServerLogger logger = new ServerLogger(Config.getServerLogFile());`
        - Logger schreibt in Datei + verteilt Log-Zeilen an GUI-Listener
    - `ChatServer server = new ChatServer(port, repo, logger);`
        - Kern-Server: akzeptiert Clients, verwaltet Rooms, Nutzer, Admin-Aktionen
- ShutdownHook (sauberes Beenden)
    - `Runtime.getRuntime().addShutdownHook(...)`
        - Wird beim Beenden der JVM ausgeführt (z.B. Fenster schließen / Ctrl+C)
        - `server.stop()` beendet accept-loop + trennt Clients
        - `logger.close()` schließt Datei-Handle
- Swing-Thread (EDT)
    - `SwingUtilities.invokeLater(...)`
        - GUI-Erzeugung im EDT (Swing-Konvention)
    - `ServerFrame frame = new ServerFrame(server);`
        - Übergibt den Server an die GUI, damit Buttons/Timer direkt `server.start()/stop()` sowie Listen-Refresh nutzen können
    - `frame.initialize(frame); frame.setVisible(true);`
        - baut GUI zusammen und zeigt sie an

## PasswordUtil
- Hilfsklasse für sicheres Passwort-Hashing mit PBKDF2 (keine Klartext-Passwörter)
- Technik: Salt + Iterationen pro User, gespeicherter Hash wird später gegen neu berechneten Hash geprüft
- Konstanten
    - `DEFAULT_ITERATIONS = 120_000` Standard-Work-Factor (macht Brute-Force teurer)
    - `SALT_BYTES = 16` zufälliger Salt pro User
    - `KEY_BITS = 256` Ziellänge des Hashes
- `newSalt()`
    - erzeugt `SALT_BYTES` zufällige Bytes via `SecureRandom`
    - wird bei Registrierung genutzt, damit gleiche Passwörter nicht gleiche Hashes ergeben
- `pbkdf2(password, salt, iterations)`
    - berechnet Hash mit `SecretKeyFactory` und `PBEKeySpec`
    - primär: `PBKDF2WithHmacSHA256`, fallback: `PBKDF2WithHmacSHA1`
    - `spec.clearPassword()` löscht Passwort aus dem Spec (Best Practice)
- `matches(password, user)`
    - berechnet Hash erneut mit den gespeicherten `salt` + `iterations` des Users
    - vergleicht konstantzeitnah mit `MessageDigest.isEqual(...)`
    - überschreibt `computed` danach mit 0-Bytes (reduziert Datenreste im Speicher)

## Protocol
- Client -> Server Commands:
    - Register, Login, Msg, Logout, createRoom, join, leave
- Server -> Client Commands:
    - Register_OK / FAILED, Login_OK / FAILED, ROOM_LIST, ROOM_USERS, CHAT, WARN, BANNED, INFO
- splitTokens() -> splittet nach whitespace, maximal 3 Teile um die Commands zu parsen
- Builder-Methoden um Syntax zu vereinfachen (buildLogin, buildChat)
- payloads für ROOM_LIST und ROOM_USERS

## Room
- Datenklasse für einen Chatraum: `name`, `persistent` und `members`
- `members` ist ein thread-sicheres Set via `ConcurrentHashMap.newKeySet()` (wichtig, weil mehrere `ClientHandler`-Threads parallel join/leave machen)
- Konstruktoren
    - `Room(name)` erstellt normalen (nicht persistenten) Raum
    - `Room(name, persistent)` für Admin-/dauerhafte Räume (z.B. Lobby)
- Member-Handling
    - `addMember/removeMember` verwalten die `ClientHandler` im Raum
    - `isEmpty()` prüft, ob Raum leer ist (wird im Server fürs automatische Löschen genutzt)
- `getMemberNames()`
    - mapped `ClientHandler -> displayName`
    - filtert leere Namen, sortiert alphabetisch und liefert eine Liste für `ROOM_USERS` an den Client

## ServerLogger
- Zentrale Logging-Komponente für den Server (Konsole/Datei + GUI-Updates)
- Persistenz (Datei)
    - schreibt jede Log-Zeile in `logFile` via `BufferedWriter` (APPEND)
    - erstellt Ordner automatisch (`Files.createDirectories(...)`)
- In-Memory History
    - speichert die letzten `maxHistory` Zeilen in `history` (`ArrayDeque`)
    - `getHistorySnapshot()` liefert eine Kopie für UI/Debug
- Thread-Sicherheit
    - Schreiben + History-Update sind `synchronized`, damit parallele Threads keine Zeilen vermischen
    - `listeners` ist `CopyOnWriteArrayList`, damit Listener (GUI) parallel hinzugefügt/entfernt werden können
- UI-Callbacks
    - nach dem Schreiben ruft er `ServerLogListener.onLogLine(line)` für alle Listener auf (z.B. ServerFrame aktualisiert TextArea)
- `close()`
    - schließt den Writer sauber (für ShutdownHook / Programmende)

## ServerLogListener
- `@FunctionalInterface` → genau eine abstrakte Methode, daher ideal für Lambdas (`line -> ...`)
- Dient als Callback-Schnittstelle für den `ServerLogger`
    - `onLogLine(String line)` wird aufgerufen, sobald eine neue Log-Zeile entsteht
- Wird z.B. im `ServerFrame` registriert, um die GUI-Logbox live zu aktualisieren

## User
- Immutable Datenklasse für einen persistierten Benutzer (keine Klartext-Passwörter)
- Felder enthalten alles für Auth + Bannstatus:
    - `username`
    - `iterations`, `salt`, `passwordHash` → Parameter/Ergebnis von PBKDF2
    - `banned` → ob der User dauerhaft ausgeschlossen ist
- Getter liefern nur Lesezugriff (keine Setter → Objekt bleibt unverändert)
- `withBanned(boolean)` erzeugt eine neue `User`-Instanz mit geändertem Bannstatus (funktionaler/immutabler Stil)
- `toString()` nur für Debug/Logs (zeigt Username + Bannstatus, keine sensiblen Daten)

## UserRepository
- Interface als Abstraktion für Benutzer-Persistenz (damit `ChatServer` nicht an eine konkrete Speicherart gekoppelt ist)
- Definiert die Kern-Operationen:
    - `createUser(username, password)` → neuen User anlegen (Passwort kommt als `char[]`, damit man es danach überschreiben kann)
    - `verifyLogin(username, password)` → Login prüfen und bei Erfolg den `User`-Datensatz zurückgeben (inkl. `banned`-Flag)
    - `listUsers()` → alle gespeicherten User für Admin/Debug anzeigen
    - `setBanned(username, banned)` → Bannstatus persistent setzen/entfernen
- Konkrete Implementierung ist bei euch `FileUserRepository` (Datei-basiert), aber durch das Interface könnte man später leicht auf DB wechseln


## In Studium/FortProg/FPP-Chatroom
### Commands (kompilieren):
- `mkdir -p out`                                     
- `find src/main/java -name "*.java" > sources.txt`
- `javac -d out @sources.txt`
### Server:
- `java -cp out de.uni_jena.fpp.chatroom.GuiServerMain`

### Client:
- `java -cp out de.uni_jena.fpp.chatroom.GuiClientMain`