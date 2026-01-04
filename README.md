# Real-Time Multiplayer Soccer Game

This project is a real-time multiplayer soccer game developed using Java, utilizing Socket programming for networking and JavaFX for the user interface. The system supports an 11 vs 11 simulation, an administrator control panel for match management, a player admission system, and intelligent AI bots that automatically fill vacant positions.

## System Requirements

* **JDK**: Version 21 or higher.
* **IDE**: IntelliJ IDEA is highly recommended for compatibility.
* **Build Tool**: Maven.

---

## Network Configuration (Important)

Before running the game on different computers, you must identify the IP address of the computer acting as the Server and ensure that Client computers can communicate with it.

### 1. How to Find the Server IP Address
Perform this step on the computer that will run the `ServerMain` class.

1.  Press the **Windows Key + R** on your keyboard to open the Run dialog.
2.  Type `cmd` and press **Enter** to open the Command Prompt.
3.  In the Command Prompt window, type the following command and press Enter:
    ```cmd
    ipconfig
    ```
4.  Look for the section labeled **Wireless LAN adapter Wi-Fi** (if using Wi-Fi) or **Ethernet adapter** (if using a cable).
5.  Locate the line that says **IPv4 Address**. It will look something like `192.168.0.105` or `10.x.x.x`.
6.  This number is your **Server IP Address**. Share this with all players joining the game.

### 2. How to Test Connectivity (Ping Command)
Perform this step on the Client computers to ensure they can reach the Server.

1.  Open the Command Prompt on the Client computer.
2.  Type the following command, replacing `[Server_IP]` with the address you found in the previous step:
    ```cmd
    ping [Server_IP]
    ```
    * *Example:* `ping 192.168.0.105`
3.  **Successful Connection:** If you see messages saying `Reply from 192.168.0.105: bytes=32 time=...`, the connection is good.
4.  **Failed Connection:** If you see `Request timed out`, the Server's firewall is likely blocking the connection. You may need to temporarily disable the firewall on the Server computer for the Private Network profile.

---

## Execution Instructions

Please execute the following modules in the exact order listed below. Failure to follow this order may result in connection errors.

### Step 1: Start the Server
The server acts as the central host for the game logic and must be running before any other component.

1.  Navigate to the project directory: `src/main/java/com/soccer/server/ServerMain.java`.
2.  Right-click on the file and select **Run 'ServerMain'**.
3.  Check the console output. The system is ready when you see the message: `>>> STARTING SOCCER SERVER on Port 9999 <<<`.

### Step 2: Start the Administrator Panel
This panel is required to approve players and control the start of the match.

1.  Navigate to the project directory: `src/main/java/com/soccer/client/AdminLauncher.java`.
    * **Important Note:** You must run `AdminLauncher`. Do not run `AdminClient` directly, as this will cause JavaFX initialization errors.
2.  Right-click on the file and select **Run 'AdminLauncher'**.
3.  A black window titled "ADMIN CONTROL PANEL" will appear.

### Step 3: Start the Game Clients (Players)
1.  Navigate to the project directory: `src/main/java/com/soccer/client/AppLauncher.java`.
    * **Important Note:** You must run `AppLauncher`. Do not run `ClientMain` directly.
2.  Right-click on the file and select **Run 'AppLauncher'**.
3.  Enter your Name in the text field.
    * If running locally, keep the IP as `localhost`.
    * If running on a different computer, enter the Server IP found in the "Network Configuration" section.
4.  Click the **JOIN MATCH** button.
5.  You will see a black screen displaying `WAITING FOR ADMIN APPROVAL...`. This is the expected behavior until the administrator grants access.
6.  **Running Multiple Players:** You can run the `AppLauncher` multiple times to create additional players on the same machine.
7.  **Restarting a Match:** After a game concludes, the administrator can force stop or end the game. Players must re-run the `AppLauncher` to join a new match session.

---

## Gameplay Flow

1.  **Player Registration:**
    * Once a player launches the client and clicks JOIN, they enter a waiting queue.
    * If the username is already taken or the server has reached its capacity (22 players), the system will display an error message.

2.  **Administrator Approval:**
    * The administrator must switch to the **Admin Control Panel** window.
    * The newly joined player will appear in the list with a status indicator (X).
    * The administrator must click the **[APPROVE]** button next to the player's name.
    * Upon approval, the player's game window will immediately unlock and display the match lobby or soccer field.

3.  **Starting the Match:**
    * Once all players have joined and are ready, the administrator clicks the **[START MATCH NOW]** button at the bottom of the panel.
    * The game will initiate a 3-second countdown, after which the match begins.
    * **Controls:**
        * **W, A, S, D**: Move the player.
        * **SPACE**: Shoot the ball.
        * **SHIFT**: Sprint (increase speed).

4.  **Ending the Match:**
    * The game will conclude automatically when the match timer expires.
    * Alternatively, the administrator can click **[FORCE END GAME]** to stop the session immediately.

---

## Troubleshooting Guide

### How to run multiple players on a single computer?
By default, IntelliJ IDEA may restrict you to a single instance of an application. To allow multiple players for testing:

1.  In the top-right corner of IntelliJ IDEA, click the dropdown menu next to the `AppLauncher` run configuration.
2.  Select **Edit Configurations...**.
3.  Locate the section labeled **Modify options** (or "Build and run options" in newer versions).
4.  Check the box for **Allow multiple instances** (sometimes labeled as "Allow parallel run").
5.  Click **OK** to save the changes.
6.  You can now click the Run button multiple times to launch Player 1, Player 2, etc.

### Error: "JavaFX runtime components are missing"
**Cause:** This error occurs if you attempt to run `ClientMain` or `AdminClient` classes directly.
**Solution:** Always execute the application using the launcher classes: **`AppLauncher`** and **`AdminLauncher`**. These classes are specifically designed to properly initialize the JavaFX environment.

### Players or enemies are not moving/visible
**Solution:** Ensure that `ServerMain` was started *before* any clients attempted to connect. If the server is not running, clients cannot synchronize data. Additionally, ensure the administrator has clicked **START MATCH**. Players cannot move freely until the match has officially started.
