1. Anforderungen für Meilenstein 2

1.1 Anforderungen an den Server
- Server als GUI-Anwendung (nicht Konsole).
- Dynamische Anzeige in der Server-GUI:
- Liste aller vorhandenen Räume
- Liste aller angemeldeten Benutzer + der Raum, in dem jeder Benutzer gerade ist
- Serverlog:
  - Live-Ansicht aller Aktivitäten (Verbindung, Kommunikation, Verwaltung) in der GUI
  zusätzlich in Datei protokollieren
  - Mehrraum-System statt Einraum-Broadcast.
  - Benutzerkontenverwaltung persistent & sicher auf Festplatte + Anzeige dieser Datensätze (z.B. Liste im Server).
  - Raumverwaltung (Server-Methoden): Räume erstellen und löschen.
  - Benutzerverwaltung (Server-Methoden):
  - Verwarnen (Client bekommt Nachricht)
  - Permanent bannen: Verbindung trennen und erneutes Anmelden verhindern

Hinweis: Der Bewertungsbogen listet zusätzlich „Datenbank“ und „sichere Kommunikation“ als Punkte. Für den MVP von MS2 ist im Aufgabenblatt aber sicher zentral: persistente, sichere Speicherung auf Festplatte + Anzeige sowie Admin-Funktionen (Warn/Ban).

1.2 Anforderungen an den Client
- Client als GUI-Anwendung.
- Dynamische Anzeige in der Client-GUI:
  - Liste aller Räume
  - Liste aller User im Raum, in dem der Client gerade ist
- Raumverwaltung im Client:
  - Raum erstellen
  - Raum betreten
  - Raum verlassen
  - Wenn die letzte Person einen Raum verlässt ⇒ Raum wird gelöscht
  - Kommunikationsoberfläche (Chat UI):
  - Nachrichtenfenster (Anzeige)
  - Eingabezeile
  - Senden-Button

2. Plan zur Umsetzung der Anforderungen
2.0 Ziel-Architektur
- Prinzip: UI ruft nur „saubere Methoden“ auf / bekommt Events, Netzwerk & Logik liegen separat.
- Server-Core: Räume, Nutzer, Logging, Admin-Aktionen, Protokoll
- Client-Core: Netzwerk + Model-State (aktueller Raum, RoomList, UserList, ChatMessages)
- UI (Ian): zeigt nur State an und feuert Aktionen (Button/Listen-Klicks)
- Für GUI-Umsetzung passen AWT/Swing-Komponenten (List/TextArea/TextField/Button) und Listener (ActionListener/ItemListener).

2.1 Protokoll erweitern
- Client → Server
- CREATE_ROOM <room>
- JOIN <room>
- LEAVE
- MSG <text>
- (Admin über Server-GUI) WARN <user> <text> / BAN <user> <reason>
- Server → Client
- ROOM_LIST <room1>|<room2>|... (bei Änderungen pushen)
- ROOM_USERS <room> <user1>|<user2>|... (für den aktuellen Raum pushen)
- CHAT <room> <from> <text>
- WARN <text>
- BANNED <reason> (danach close)

Technisch weiterhin über TCP Stream-Sockets, pro Client eigener Thread.

2.2 Mehrraum-System 
- Neue Datenstruktur (minimal):
  - Map<String, Room> rooms
- Room enthält z.B. Set<ClientHandler> oder Set<String> usernames
- Jeder ClientHandler hat currentRoom
- Regeln
  - Beim Login: in Default-Raum (z.B. "Lobby") oder „kein Raum“ bis JOIN.
  - MSG broadcastet nur in currentRoom
  - LEAVE: User aus Raum entfernen; wenn Raum leer ⇒ löschen (und ROOM_LIST pushen)

2.3 Persistente Benutzerverwaltung 
- Aus Aufgabenblatt: sicher & dauerhaft auf Festplatte + anzeigen.
- Vorschlag (MVP-sicher & simpel):
  - UserRepository Interface
  - loadAll(), saveAll(), createUser(...), verifyLogin(...), listUsers(), banUser(...)
  - FileUserRepository Implementation
  - Datei: z.B. users.db / users.json / users.ser
  - „sicher“: Passwörter nicht im Klartext (mind. Hash+Salt)
  - Anzeige: Server ruft listUsers() und UI zeigt Tabelle/Liste 
  - Datei-I/O sauber mit Streams/Buffered Streams (wie in den Unterlagen).

2.4 Admin-Funktionen: Warnen & Permanent Bann
- Aus Aufgabenblatt: Verwarnung + permanentes Ausschließen, inkl. Re-Login verhindern.
- warnUser(username, text) ⇒ Server findet ClientHandler ⇒ sendet WARN ...
- banUser(username, reason):
- Repository markiert Benutzer als gebannt (persistiert)
- Wenn online: BANNED ... senden, Socket schließen
- Beim Login: wenn gebannt ⇒ direkt ablehnen + close

2.5 Logging (GUI + Datei) 
- Aus Aufgabenblatt: Log in GUI + Datei.
- Design:
  - ServerLogger (oder einfach ChatServer.log(String))
  - schreibt in Datei (append)
  - feuert Event an UI (z.B. Listener/Callback)
  - Log-Ereignisse: Serverstart/stop, connect/disconnect, login/logout, room create/delete, join/leave, msg, warn/ban
  - Für robustes Schließen von Ressourcen: try/catch/finally / throws Regeln beachten.

2.6 Client-State-Model für GUI 
- List<String> rooms
- String currentRoom
- List<String> usersInCurrentRoom
- List<String> chatLines (oder Event-Stream)

- Methoden:
  - createRoom(name), join(room), leave(), sendMessage(text)
  - Client hat weiter 2 Threads: UI-Thread + Netzwerk-Listener (habt ihr vom Konsolenclient). Der Listener aktualisiert nur Model + ruft UI-Callback.

2.7 GUI-Implementierung 
- ClientWindow: Room-Liste + User-Liste + Chat (TextArea + TextField + Button)
- ServerWindow: Room-Liste + User+Room-Liste + Log-TextArea + Admin-Aktionen (Warn/Ban Buttons oder Menü)
- Events: Button = ActionListener, Room-Liste = ItemListener/ActionEvent.

2.8 Integration & Tests 
- Minimal-Testmatrix für Abgabe:
  - 2 Clients connecten, registrieren/login
  - Raum erstellen/joinen, Msg nur im Raum sichtbar
  - Raum verlassen ⇒ wenn letzter ⇒ Raum gelöscht & bei allen aktualisiert
  - Server-UI zeigt Rooms + Users(+currentRoom) dynamisch
  - Warn: User bekommt Warn-Nachricht
  - Ban: User fliegt raus & kann nicht mehr rein
  - Neustart Server ⇒ Userdaten noch da + „anzeigen“ funktioniert
  - Log-Datei entsteht und enthält Events

3) Klassenübersicht als „Adjazenzliste“ (ER-ähnlich, minimal & UI-freundlich)
ChatServer
→ Map<String, Room> rooms
→ Map<String, ClientHandler> onlineUsers
→ UserRepository userRepo
→ ServerLogger logger
→ Methoden: createRoom(), deleteRoom(), joinRoom(), leaveRoom(), broadcastToRoom(), warnUser(), banUser()

Room
→ String name
→ Set<ClientHandler> members
→ Methoden: addMember(), removeMember(), isEmpty()

ClientHandler (Thread)
→ Socket socket
→ DataInputStream in, DataOutputStream out
→ ChatServer server
→ User user (oder String username)
→ Room currentRoom
→ Methoden: send(cmd), handleCommand(...), close()

User
→ String username
→ byte[] salt
→ byte[] passwordHash
→ boolean banned

UserRepository (Interface)
→ FileUserRepository (MS2 MVP)
(optional später) → SqlUserRepository (falls ihr „Datenbank“ mitnehmen wollt)

ServerLogger
→ File logFile
→ List<LogListener> listeners (UI hängt sich dran)
→ Methoden: log(event)

ChatClient
→ Socket socket, DataInputStream/OutputStream
→ ClientModel model
→ NetworkListenerThread

ClientModel
→ rooms, usersInRoom, currentRoom, chatLines
→ ModelListener (UI)