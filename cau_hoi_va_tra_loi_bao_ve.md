# Cau Hoi Va Tra Loi Bao Ve Project Task Manager

## 1. Project cua em lam ve de tai gi?

Project cua em la ung dung quan ly cong viec ca nhan. Ung dung cho phep nguoi dung dang ky, dang nhap, tao cong viec, sua cong viec, xoa cong viec, gan folder, gan tag, tao subtask, dat muc do uu tien, theo doi tien do, dat nhac nho va xem thong ke.

## 2. Project dung ngon ngu va cong nghe gi?

Project dung Java 17, JavaFX de lam giao dien, Maven de quan ly thu vien, MariaDB/MySQL de luu du lieu, JDBC de ket noi database, TCP Socket de tach client-server, Apache POI de xuat Excel va PDFBox de xuat PDF.

File can mo:

- `pom.xml`
- `src/main/java/com/taskmanager/Main.java`
- `src/main/java/com/taskmanager/util/DBConnection.java`

## 3. Project co dung client-server khong?

Co. Project co tach client va server.

Client la ung dung JavaFX, khi chay se ket noi toi server qua:

```java
NetworkService.getInstance().connect("localhost", 9999);
```

Server chay tren cong 9999:

```java
TaskManagerServer server = new TaskManagerServer(9999);
```

Client gui `Request`, server xu ly va tra ve `Response`.

File can mo:

- `src/main/java/com/taskmanager/Main.java`
- `src/main/java/com/taskmanager/network/NetworkService.java`
- `src/main/java/com/taskmanager/server/ServerMain.java`
- `src/main/java/com/taskmanager/server/RequestRouter.java`

## 4. Project co ap dung MVC khong?

Co. Project ap dung MVC va tach them Service, DAO de ro rang hon.

- Model: luu du lieu, vi du `Task`, `User`, `Folder`, `Tag`, `Reminder`.
- View: giao dien JavaFX, vi du `AuthView`, `MainWindow`, `TaskListView`.
- Controller: nhan yeu cau tu View va gui request len server, vi du `TaskController`, `UserController`.
- Service: xu ly nghiep vu trung gian.
- DAO: thao tac truc tiep voi database.

File can mo:

- `src/main/java/com/taskmanager/model/Task.java`
- `src/main/java/com/taskmanager/view/TaskListView.java`
- `src/main/java/com/taskmanager/controller/TaskController.java`
- `src/main/java/com/taskmanager/service/TaskService.java`
- `src/main/java/com/taskmanager/dao/TaskDAO.java`

## 5. Project co dung database khong?

Co. Project dung MariaDB/MySQL, database ten la `taskmanager`.

Thong tin ket noi:

```text
Host: 127.0.0.1
Port: 3306
Database: taskmanager
User: root
Password: de trong
```

Ung dung tu tao database va cac bang neu chua co.

Bang chinh:

- `users`
- `tasks`
- `folders`
- `subtasks`
- `tags`
- `task_tags`
- `reminders`
- `settings`

File can mo:

- `src/main/java/com/taskmanager/util/DBConnection.java`
- `src/main/java/com/taskmanager/dao/TaskDAO.java`
- `src/main/java/com/taskmanager/dao/UserDAO.java`

## 6. Project co dung JDBC khong?

Co. Project dung JDBC de ket noi Java voi MariaDB/MySQL.

Trong `pom.xml` co thu vien:

```xml
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.5.1</version>
</dependency>
```

Trong `DBConnection.java` co JDBC URL:

```java
jdbc:mariadb://127.0.0.1:3306/taskmanager
```

Va ket noi bang:

```java
DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
```

Luu y: file `sqljdbc_13.4.0.0_enu` la JDBC driver cua Microsoft SQL Server, project nay khong can vi project dung MariaDB.

## 7. Project co bao mat khong?

Co cac muc bao mat co ban:

- Co dang ky, dang nhap.
- Mat khau khong luu truc tiep ma luu dang hash.
- Recovery PIN cung duoc hash.
- Co phan quyen admin bang truong `is_admin`.
- Co gioi han so lan dang nhap sai tren giao dien.
- Truy van database dung `PreparedStatement` de giam nguy co SQL Injection.

File can mo:

- `src/main/java/com/taskmanager/util/PasswordUtil.java`
- `src/main/java/com/taskmanager/dao/UserDAO.java`
- `src/main/java/com/taskmanager/view/AuthView.java`

Doan can chi:

```java
MessageDigest.getInstance("SHA-256");
```

```java
PreparedStatement
```

## 8. Project co dung thread khong?

Co. Project dung thread o nhieu cho:

- Server tao thread rieng de lang nghe client.
- Moi client ket noi vao co mot `ClientHandler` rieng.
- Reminder dung `ScheduledExecutorService` de kiem tra nhac nho dinh ky.
- Giao dien dung JavaFX `Task` de tai du lieu nen khong bi treo UI.

File can mo:

- `src/main/java/com/taskmanager/server/TaskManagerServer.java`
- `src/main/java/com/taskmanager/server/ClientHandler.java`
- `src/main/java/com/taskmanager/service/ReminderService.java`
- `src/main/java/com/taskmanager/view/TaskListView.java`

Doan can chi:

```java
new Thread(handler, "ClientHandler-" + socket.getRemoteSocketAddress()).start();
```

```java
ScheduledExecutorService
```

## 9. Cach chay project nhu the nao?

Chay theo thu tu:

```powershell
.\start-db.bat
.\run-server.bat
```

Mo terminal khac:

```powershell
.\run-client.bat
```

Luu y: phai chay database truoc, sau do chay server, cuoi cung moi chay client.

## 10. Neu giao vien yeu cau mo MariaDB thi mo o dau?

Co the mo bang HeidiSQL hoac terminal.

Neu dung HeidiSQL:

```text
Network type: MariaDB or MySQL (TCP/IP)
Hostname / IP: 127.0.0.1
User: root
Password: de trong
Port: 3306
Database: taskmanager
```

Neu dung terminal:

```powershell
& 'C:\Program Files\MariaDB 12.3\bin\mariadb.exe' -uroot taskmanager
```

Sau do co the go:

```sql
SHOW TABLES;
SELECT id, username, name, is_admin FROM users;
SELECT id, title, status, priority FROM tasks;
```

## 11. Tai sao can chay server truoc client?

Vi client JavaFX khong ket noi truc tiep database. Client chi gui request toi server qua socket port 9999. Neu server chua chay, client se bao loi khong ket noi duoc server.

## 12. Tai sao khong can mo Laragon nua?

Luc dau project dung MySQL cua Laragon, nhung de tranh viec phu thuoc Laragon, project hien da dung MariaDB Server rieng. Script `start-db.bat` se khoi dong service `TaskManagerMariaDB`, nen khong can mo Laragon.

## 13. Tai khoan cu trong database la gi?

Du lieu cu da duoc chuyen tu MySQL cua Laragon sang MariaDB moi. Trong database co tai khoan:

```text
tnguyeen
tri123
```

Luu y ten dang nhap la `tnguyeen`, co hai chu `e`.

## 14. Cau tra loi tong hop ngan gon

Da, project cua em la ung dung quan ly cong viec ca nhan viet bang JavaFX. Project co dung mo hinh client-server: client JavaFX ket noi server qua TCP Socket port 9999, server xu ly request va lam viec voi MariaDB/MySQL. Project co ap dung MVC, gom Model, View, Controller va tach them Service, DAO. Database ten la `taskmanager`, ket noi bang JDBC thong qua thu vien `mariadb-java-client`. Project co cac tinh nang bao mat co ban nhu hash mat khau/PIN bang SHA-256, phan quyen admin, gioi han dang nhap sai va dung PreparedStatement. Project cung co dung thread: server xu ly moi client bang thread rieng, reminder chay dinh ky va UI tai du lieu nen bang JavaFX Task.

