# Huong Dan Bao Ve Project Task Manager

Tai lieu nay dung de tra loi khi giao vien hoi ve cac muc: client-server, MVC, database, bao mat va thread.

## 1. Client-server

### File can mo

- `src/main/java/com/taskmanager/Main.java`
- `src/main/java/com/taskmanager/server/ServerMain.java`
- `src/main/java/com/taskmanager/network/NetworkService.java`
- `src/main/java/com/taskmanager/server/RequestRouter.java`

### Doan can chi

Trong `Main.java`, chi dong:

```java
NetworkService.getInstance().connect("localhost", 9999);
```

Noi:

> Day la phia client JavaFX. Khi ung dung client chay, no ket noi den server qua dia chi localhost va cong 9999.

Trong `ServerMain.java`, chi dong:

```java
TaskManagerServer server = new TaskManagerServer(9999);
```

Noi:

> Day la phia server. Server mo cong 9999 de lang nghe ket noi tu client.

Trong `NetworkService.java`, chi cac thanh phan:

```java
Socket socket;
ObjectOutputStream out;
ObjectInputStream in;
```

Noi:

> Client va server giao tiep bang TCP Socket. Client gui Request len server va nhan Response tra ve.

Trong `RequestRouter.java`, chi ham:

```java
public Response route(Request request, ClientHandler handler)
```

Noi:

> Server nhan request tu client, dua vao action de dieu huong sang cac service xu ly tuong ung.

## 2. MVC

Project co ap dung MVC, dong thoi tach them service va DAO de code ro rang hon.

### File can mo theo chuc nang Task

- Model: `src/main/java/com/taskmanager/model/Task.java`
- View: `src/main/java/com/taskmanager/view/TaskListView.java`
- Controller: `src/main/java/com/taskmanager/controller/TaskController.java`
- Service: `src/main/java/com/taskmanager/service/TaskService.java`
- DAO: `src/main/java/com/taskmanager/dao/TaskDAO.java`

### Cach noi

Mo `Task.java`, noi:

> Day la Model, dung de luu thong tin cua cong viec nhu title, description, priority, status, ngay bat dau, ngay het han, subtask va tag.

Mo `TaskListView.java`, noi:

> Day la View, dung de hien thi danh sach cong viec va nhan thao tac tu nguoi dung nhu them, sua, xoa, loc, tim kiem.

Mo `TaskController.java`, chi vi du:

```java
Request req = new Request("TASK_GET_ALL")
```

Noi:

> Controller nhan yeu cau tu View, tao Request va gui len server thong qua NetworkService.

Mo `TaskService.java`, noi:

> Service la lop trung gian xu ly nghiep vu va goi xuong DAO.

Mo `TaskDAO.java`, noi:

> DAO la lop lam viec truc tiep voi database, gom cac cau lenh SELECT, INSERT, UPDATE, DELETE.

## 3. Database

### File can mo

- `src/main/java/com/taskmanager/util/DBConnection.java`
- `src/main/java/com/taskmanager/dao/TaskDAO.java`
- `src/main/java/com/taskmanager/dao/UserDAO.java`

### Doan can chi

Trong `DBConnection.java`, chi phan:

```java
DriverManager.getConnection(...)
```

Noi:

> Ung dung ket noi toi MariaDB/MySQL bang JDBC.

Chi phan:

```java
CREATE DATABASE IF NOT EXISTS
```

Noi:

> Neu database chua ton tai, chuong trinh se tu tao database.

Chi cac bang:

```sql
users
folders
tasks
subtasks
tags
task_tags
reminders
settings
```

Noi:

> Day la cac bang chinh cua he thong. Moi user co du lieu rieng thong qua cot user_id.

Trong `TaskDAO.java`, chi cac cau SQL:

```sql
SELECT
INSERT
UPDATE
DELETE
```

Noi:

> Day la cac thao tac CRUD voi bang tasks.

## 4. Bao mat

### File can mo

- `src/main/java/com/taskmanager/util/PasswordUtil.java`
- `src/main/java/com/taskmanager/dao/UserDAO.java`
- `src/main/java/com/taskmanager/view/AuthView.java`
- `src/main/java/com/taskmanager/server/RequestRouter.java`

### Doan can chi

Trong `PasswordUtil.java`, chi:

```java
MessageDigest.getInstance("SHA-256");
```

Noi:

> Mat khau va recovery PIN khong luu truc tiep, ma duoc bam SHA-256 truoc khi luu vao database.

Trong `UserDAO.java`, chi:

```java
password_hash
recovery_pin_hash
is_admin
```

Noi:

> Bang users luu password_hash, recovery_pin_hash va co truong is_admin de phan quyen admin.

Trong `UserDAO.java`, chi:

```java
PreparedStatement
```

Noi:

> Cac cau lenh SQL dung PreparedStatement de truyen tham so, giup giam nguy co SQL Injection.

Trong `AuthView.java`, chi:

```java
MAX_LOGIN_ATTEMPTS
LOCK_DURATION
```

Noi:

> Giao dien co gioi han so lan dang nhap sai. Neu sai qua so lan quy dinh thi khoa tam thoi.

Trong `RequestRouter.java`, chi:

```java
if (handler.getLoggedInUser() != null) {
    SessionContext.setCurrentUser(handler.getLoggedInUser());
} else {
    return Response.error("Chua dang nhap");
}
```

Noi:

> Cac chuc nang quan ly du lieu yeu cau nguoi dung da dang nhap. Neu chua dang nhap thi server khong cho thuc hien.

## 5. Thread

### File can mo

- `src/main/java/com/taskmanager/server/TaskManagerServer.java`
- `src/main/java/com/taskmanager/server/ClientHandler.java`
- `src/main/java/com/taskmanager/service/ReminderService.java`
- `src/main/java/com/taskmanager/view/TaskListView.java`

### Doan can chi

Trong `TaskManagerServer.java`, chi:

```java
new Thread(() -> {
```

Noi:

> Server dung mot thread rieng de lang nghe ket noi tu client.

Trong `TaskManagerServer.java`, chi:

```java
new Thread(handler, "ClientHandler-" + socket.getRemoteSocketAddress()).start();
```

Noi:

> Moi client ket noi vao se co mot ClientHandler chay tren thread rieng.

Trong `ClientHandler.java`, chi:

```java
public class ClientHandler implements Runnable
```

Noi:

> ClientHandler implement Runnable de co the chay nhu mot thread xu ly rieng cho tung client.

Trong `ReminderService.java`, chi:

```java
ScheduledExecutorService
```

Noi:

> Chuc nang nhac viec dung ScheduledExecutorService de kiem tra reminder dinh ky.

Trong `TaskListView.java`, chi:

```java
new javafx.concurrent.Task<>()
```

Noi:

> Phan giao dien dung JavaFX Task de tai du lieu o nen, tranh lam treo giao dien khi truy van du lieu.

## Cau tra loi tong hop ngan gon

Neu giao vien hoi tong quan, co the noi:

> Da, project cua em co su dung client-server, MVC, database, bao mat va thread. Client la JavaFX, ket noi server qua TCP Socket port 9999. Server nhan Request, xu ly qua RequestRouter, Service va DAO roi tra Response ve client. Project ap dung MVC voi Model, View, Controller, tach them Service va DAO. Database dung MariaDB/MySQL, tu tao cac bang users, tasks, folders, tags, reminders. Bao mat gom hash mat khau/PIN bang SHA-256, PreparedStatement chong SQL Injection co ban, phan quyen admin va gioi han dang nhap sai. Thread duoc dung o server de lang nghe ket noi, moi client co ClientHandler rieng, reminder chay dinh ky bang ScheduledExecutorService va giao dien tai du lieu bang JavaFX Task.

