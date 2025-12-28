# ⚽ Real-Time Multiplayer Soccer Game

这是一个基于 Java (Socket + JavaFX) 开发的多人实时足球游戏。支持 11vs11 模拟、管理员控制面板、玩家准入系统以及智能 AI 补位。

## 🛠️ 环境要求 (Prerequisites)
* **JDK**: 21 或更高版本
* **IDE**: IntelliJ IDEA (推荐)
* **Build Tool**: Maven

---

## 🚀 启动步骤 (How to Run)

请严格按照以下顺序启动程序，否则会导致连接失败。

### 第一步：启动服务器 (Server)
这是游戏的大脑，必须第一个启动。
1.  在项目目录中找到：`src/main/java/com/soccer/server/ServerMain.java`
2.  右键点击 -> **Run 'ServerMain'**
3.  **检查控制台**：看到 `>>> STARTING SOCCER SERVER on Port 9999 <<<` 表示启动成功。

### 第二步：启动管理员面板 (Admin Panel)
用于批准玩家进入和控制比赛开始。
1.  在项目目录中找到：`src/main/java/com/soccer/client/AdminLauncher.java`
    * ⚠️ **注意**：请运行 `AdminLauncher`，不要直接运行 `AdminClient`，以避免 JavaFX 错误。
2.  右键点击 -> **Run 'AdminLauncher'**
3.  一个黑色的 "ADMIN CONTROL PANEL" 窗口将会弹出。

### 第三步：启动玩家客户端 (Game Client)
1.  在项目目录中找到：`src/main/java/com/soccer/client/AppLauncher.java`
    * ⚠️ **注意**：请运行 `AppLauncher`，不要直接运行 `ClientMain`。
2.  右键点击 -> **Run 'AppLauncher'**
3.  输入你的名字 (Name)，点击 **JOIN MATCH**。
4.  此时你会看到黑屏提示 `WAITING FOR ADMIN APPROVAL...`，这是正常的。
5.  一直rerun可以开很多个acc
6.  force stop（admin的page） / end game过后 再rerun app launcher会重开新的一局。

---

## 🎮 游戏流程 (Gameplay Flow)

1.  **玩家加入**：
    * 玩家开启客户端并点击 JOIN 后，会进入等待状态。
    * 如果是**名字重复**或**服务器满员 (22人)**，系统会弹出错误提示。

2.  **管理员批准**：
    * 回到 **Admin Control Panel** 窗口。
    * 你会看到刚才加入的玩家出现在列表中，状态为 ❌。
    * 点击玩家旁边的 **[APPROVE]** 按钮。
    * 玩家的游戏窗口将瞬间解锁，进入比赛大厅/球场。

3.  **开始比赛**：
    * 当所有玩家都准备好后，管理员点击面板底部的 **[START MATCH NOW]**。
    * 游戏倒计时 3 秒，比赛正式开始！
    * **操作**：使用 `W` `A` `S` `D` 移动，`SPACE` 射门，`SHIFT` 加速。

4.  **结束比赛**：
    * 游戏时间结束后自动结算。
    * 或者管理员点击 **[FORCE END GAME]** 强制结束。

---

## 💡 常见问题 (Troubleshooting)

### Q: 如何在一台电脑上开多个玩家进行测试？
**IntelliJ IDEA 默认不仅允许运行一个实例。你需要开启 "Allow Multiple Instances"：**
1.  在 IDEA 右上角，点击 `AppLauncher` 旁边的下拉菜单 -> **Edit Configurations...**
2.  在右侧找到 **Modify options** (或者叫 Build and run options)。
3.  勾选 **Allow multiple instances** (或 Allow parallel run)。
4.  点击 OK。
5.  现在你可以多次点击 Run 按钮来开启 Player 1, Player 2, Player 3...

### Q: 为什么报错 "JavaFX runtime components are missing"?
**原因**：你可能直接运行了 `ClientMain` 或 `AdminClient`。
**解决**：请务必运行 **`AppLauncher`** 和 **`AdminLauncher`**。这两个类是专门为了绕过 JavaFX 模块检查而设计的。

### Q: 为什么我看不到队友/敌人移动？
**解决**：请检查是否先启动了 **ServerMain**。如果服务器没开，客户端无法同步数据。确保管理员在面板中点击了 **START MATCH**，游戏未开始时玩家无法移动。

---
