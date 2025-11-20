1. Anforderungen für Meilenstein 1:

1.1 Anforderungen an den Server

Muss erfüllt sein:
- ServerSocket-Lebenszyklus implementiert
- Netzwerk-Listener (Verbindungsannehme)
- Nachrichten-Listener (Kommunikation mit Clients)
- Registrierung von Clients
- Anmeldung von Clients
- Login-Folgen (Willkommens-Infos & Benachrichtigungen)
- Broadcast-Nachrichten an alle Clients
- Fehlerfälle behandeln (z.B. Verbindungsabbrüche, doppelte Namen)

1.2 Anforderungen an den Client
Muss erfüllt sein:
- Verbindungsaufbau zum Server
- Verbindungstrennung vom Server
- Login- /Registrierungsdialog
- Senden von Nachrichten an den Server
- Empfang von Nachrichten vom Server
- Anzeige von Willkommens-Infos & Benachrichtigungen
- Reine Konsolenanwendung

2. Plan zur Umsetzung der Anforderungen
2.0 Projektstruktur & Grundsetup
Ziel: Saubere Struktur/ Basis
- Erstellen eines neuen (Maven/Gradle) Java-Projekts 
- Main-Klassen für Server und Client anlegen
- Konfigurationsdateien (z.B. port, host) erstellen

2.1 Kommunikationsprotokoll definieren
Ziel: Klare Regeln für Nachrichtenaustausch
(für Meilenstein 1: Einfaches Textprotokoll)
- Textbasiertes Protokoll per DataInput/OutputStream
(für spätere Meilensteine: serialisierte Objekte)

2.2 Server Grundgerüst (Single-Client-Prototyp)
Ziel: Funktionaler Server für einen Client
- ChatServer
- Streams vorbereiten
- Einfache Schleife

2.3 Multi-Client-Server mit Threads
Ziel: Mehrere Clients gleichzeitig bedienen
- Implementiere Klasse "ClientHandler extends Thread"

2.4 Benutzerverwaltung & Login/Registrierung
Ziel: explizite Anforderungen des Meilensteins erfüllen (Username, Passwort, Anmeldung, Registrierung)
- User-Class erstellen
- HashMap für Benutzer speichern
- ClientHandler erweitern (Login/Registrierung, Zustände)
- Thread-Sicherheit (Zugriffe auf registeredUsers/loggedInClients synchronisieren)

2.5 Broadcast & Notification-Funktionalität
Ziel: Erfüllt Anforderungen an Chat & Notifications nach Anmeldung
- Broadcast-Methode im ChatServer implementieren
- ClientHandler erweitern (Willkommensnachrichten, Benachrichtigungen)
- Fehlerbehandlung (Verbindungsabbrüche, doppelte Namen)

2.6 Client-Programm (Konsole)
Ziel: Funktionaler Client für Kommunikation mit Server
- ChatClient Klasse erstellen
- Zwei Threads (Eingabe & Ausgabe)

2.7 Logging & Meilenstein-Dokumentation
Ziel: Nachvollziehbarkeit & Dokumentation
- Logging (z.B. Server started/Stopped, User logged in/out, new connection, errors)
- Meilenstein-Dokumentation (Anforderungen, Umsetzung, Probleme, Tests)

3. Klassen und Methoden (Übersicht)

3.1 Server-Klassen

3.1.1 ChatServer
Atrribute:
- ServerSocket serverSocket
- Map<String, User> registeredUsers
- Map<String, ClientHandler> loggedInClients
- boolean isRunning

Methoden:
- startServer()
- stopServer()
- broadcastChat(String from, String message)
- broadcastSystem(String message)
- registerUser(String username, String password)
- loginUser(String username, String password, ClientHandler handler)

3.1.2 ClientHandler extends Thread
Atrribute:
- Socket clientSocket
- DataInputStream input
- DataOutputStream output
- ChatServer server
- User user (null wenn nicht angemeldet)
- boolen isRunning

Methoden: 
- run()
- send(String protocolMessage)
- handleRegister
- handleLogin
- handleMessage
- cleanupOnDisconnect()

3.1.3 User
Attribute:
- String username
- String password (nicht gehashed für Meilenstein 1)

3.2 Client-Klassen

3.2.1 ChatClient
Attribute:
- Socket socket
- DataInputStream input
- DataOutputStream output
- String username
- boolean isloggedIn
- boolean isRunning

Methoden:
- connect(String host, int port)
- disconnect()
- sendCommand(String command)
- startConsoleLoop()
- startServerListener()

3.2.2 ServerListener
Attribute:
- Referenz auf ChatClient
- DataInputStream input

Methoden:
- run()

3.3 Klassenübersicht UML-Diagramm
![UML-Diagramm Milestone 1](generatedUML-Milestone1.png)