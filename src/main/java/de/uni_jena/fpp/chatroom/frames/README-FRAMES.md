## ChatFrame (GUI)
* Swing-Clientfenster: links Chat (Ausgabe + Eingabe), rechts Raum-/Userliste + Buttons
* Hält `ChatClient client`, damit Button-Events direkt Server-Commands auslösen können (`sendMessage`, `join`, `leave`, …)
* UI-State über `DefaultListModel`:
    * `roomsModel` für Raumliste (wird dynamisch befüllt)
    * `usersModel` für User im aktuellen Raum
* Events:
    * `Send` / Enter im Textfeld → `client.sendMessage(text)` und Feld leeren
    * `Raum beitreten` → `client.join(room)`, Chat-Border wird auf Raum gesetzt, Schutz gegen „schon im Raum“
    * `Raum verlassen` → öffnet nur `CloseRoomFrame` (Dialog entscheidet, ob wirklich `leave` gesendet wird)
    * `Raum erstellen` → öffnet `CreateRoomFrame(client)` (Room-Creation GUI)
* Listener-Anbindung:
    * `client.addListener(new ChatClientListener(){...})` verbindet Netzwerk-Events mit GUI-Updates
    * Updates laufen per `SwingUtilities.invokeLater(...)` (wichtig, weil ServerListener-Thread nicht der UI-Thread ist)
    * `onRoomsUpdated` → aktualisiert `roomsModel`
    * `onUsersUpdated` → aktualisiert `usersModel`
    * `onChatMessage` / `onInfo` / `onWarn` / `onError` → schreibt in `chatBox`
    * `onBanned` → zeigt Dialog + schließt Fenster

## CloseRoomFrame (GUI)
* Kleiner Swing-Dialog, der das „Raum verlassen?“ bestätigt (Confirm/Cancel)
* Hält optional `ChatClient client`, um bei Bestätigung wirklich den Server-Command auszulösen
* Buttons:
    * `sicher` → `client.leave()` (falls Client gesetzt) und danach `dispose()`
    * `abbrechen` → nur `dispose()` (kein Leave, kein Seiteneffekt)
* Zweck: verhindert den Bug „man verlässt sofort den Raum, obwohl man abbrechen drückt“, weil `leave()` erst im Confirm-Handler passiert
* `DISPOSE_ON_CLOSE`, damit nur dieses Fenster geschlossen wird (nicht die ganze App)

## CreateRoomFrame (GUI)
* Kleines Swing-Fenster zum Erstellen eines neuen Raums (Textfeld + Erstellen/Abbrechen)
* Hält optional `ChatClient client`, damit der Button wirklich `CREATE_ROOM` an den Server schicken kann
* UI-Aufbau:
    * `tfRoomName` im Panel mit `TitledBorder("Raum erstellen")`
    * Button-Leiste unten: `erstellen` / `abbrechen`
* `btnCreate` Ablauf:
    * liest `roomName = tfRoomName.getText().trim()`
    * macht minimale Client-seitige Validierung (leer, Default-“Name”, enthält Leerzeichen oder `|`)
        * bei invalid: `MessageFrame(2,1)` (“Invalider Raum Name.”)
    * wenn `client == null`: `MessageFrame(0,0)` (nur UI-Vorschau / nicht verbunden)
    * sonst: `client.createRoom(roomName)` → sendet Protokoll-Kommando an den Server
        * danach MVP-mäßig Erfolgsmeldung `MessageFrame(2,0)` + Fenster schließen (`dispose()`)
        * bei `IOException`: Fehlerpfad `MessageFrame(2,2)` (hier als generisches “fehlgeschlagen” genutzt)
* `btnCancel`: schließt nur das Fenster (`dispose()`), ohne Server-Call
* `DISPOSE_ON_CLOSE`, damit nur dieses Fenster geschlossen wird (nicht die ganze Anwendung)

## LoginFrame (GUI)
* Swing-Login/Registrierungsfenster mit `tfName` + `tfPassword` und zwei Buttons
* Hält `ChatClient client` (wird aus `GuiClientMain` reingereicht), damit Login/Register direkt ans Server-Protokoll gehen
* `initialize()` baut UI:
    * `formPanel` (GridLayout 2x2) für Labels + Textfelder
    * `btnLogin` ruft `doLogin(frame)` auf
    * `btnRegister` ruft `doRegister()` auf
    * Enter im Passwortfeld triggert ebenfalls `doLogin(frame)`
* `doLogin()`:
    * Validiert: Client vorhanden, Username/Passwort nicht leer
    * ruft `client.login(u, p)` (sendet `LOGIN <u> <p>` über Socket)
* `doRegister()`:
    * Validiert analog, ruft `client.register(u, p)` (sendet `REGISTER ...`)
* `bindClientListener()`:
    * bindet **genau einmal** (`listenerBound`) einen `ChatClientListener`, um Server-Antworten in der GUI zu verarbeiten
    * nutzt `SwingUtilities.invokeLater(...)`, weil die Events aus dem Netzwerk-Thread kommen
    * bei Login-Erfolg: öffnet `ChatFrame(new ChatFrame(client))` und schließt Login-Fenster
    * bei Register-Erfolg/Fehler: zeigt passenden `MessageFrame`
    * bei `onError` / `onConnectionClosed`: zeigt Dialoge

## MainFrame (GUI-Basis)
* `extends JFrame` und dient als **gemeinsame Basis** für alle GUI-Fenster (Client/Server)
* definiert zentrale UI-Konstanten:
    * `mainFont`: Standard-Font für alle Komponenten
    * `mainColor`: Standard-Hintergrundfarbe für Panels
* dadurch: **einheitliches Look & Feel**, weniger Duplication in `LoginFrame`, `ChatFrame`, `ServerFrame`, usw.
* `main()` ist nur ein **Test-/Demo-Entry**, startet direkt den `LoginFrame` (im Projekt wird normalerweise über `GuiClientMain` gestartet)

## MessageFrame (kleines Info-/Fehler-Popup)
* `extends MainFrame` und ist ein **generisches Dialogfenster** für kurze Meldungen (Info/Fehler)
* speichert feste Texte in `String[][] message`
    * Index `x` = Kategorie (z.B. Registrierung, Raum)
    * Index `y` = konkreter Status (ok/fail/…)
* `initialize(frame, x, y)`:
    * wählt Text `message[x][y]`
    * zeigt ihn als `JLabel` an
    * hat einen **OK-Button**, der das Fenster via `frame.dispose()` schließt
* nutzt `DISPOSE_ON_CLOSE`, damit nur das Popup geschlossen wird und nicht die ganze Anwendung
* `main()` ist nur ein **Test-Entry**, um das Popup standalone zu starten

## ServerFrame (Server-GUI)
* `extends MainFrame` und ist die **Admin-Oberfläche** für den ChatServer
* linke Seite: **Log-Anzeige** (`chatBox`) + **Start/Stop Buttons**
    * `btnStart` startet den Server in einem **separaten Thread** (`serverThread = new Thread(server::start, ...)`), damit die GUI nicht blockiert
    * `btnStop` ruft `server.stop()` auf und toggelt die Button-States
    * `server.addLogListener(...)` leitet ServerLogger-Zeilen in die GUI weiter (**SwingUtilities.invokeLater** wegen UI-Thread)
* rechte Seite oben: **Räume-Liste** + Admin-Raumverwaltung
    * `roomsModel`/`roomList` zeigen dynamisch alle Räume an
    * `btnRoomCreate` -> InputDialog -> `server.createRoomAsServer(name)` (erstellt **persistenten** Raum)
    * `btnRoomDelete` -> bestätigt -> `server.deleteRoomAsServer(selected)` (löscht Raum, Lobby ist gesperrt)
* rechte Seite unten: **User-Liste** + Admin-Userverwaltung
    * `usersModel`/`lstUsers` zeigen User an (Label-Format: `"username | room"`)
    * `btnKick` öffnet Options-Dialog: **Warnen / Bannen**
        * Warnen: `server.warnUser(username, text)` -> sendet `WARN` an Online-User
        * Bannen: `server.banUser(username, reason)` -> persistenter Ban + Verbindung wird getrennt
* dynamische Updates per `Timer(1000, ...)`
    * pollt `server.getRoomNames()` und `server.getRoomsUnsafe()` / `server.getOnlineUserRooms()`
    * aktualisiert Models und versucht **Selections wiederherzustellen** (Room + User)
