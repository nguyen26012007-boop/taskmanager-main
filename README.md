# Personal Task Manager

Ung dung quan ly cong viec ca nhan viet bang JavaFX, TCP Socket va MariaDB/MySQL.

## Cong nghe

- Java 17
- JavaFX 21
- Maven
- MariaDB/MySQL
- TCP Socket client-server
- Apache POI
- Apache PDFBox

## Tinh nang chinh

- Dang ky, dang nhap va dang xuat tai khoan
- Quan ly cong viec: them, sua, xoa, hoan thanh
- Xem cong viec theo danh sach, card va kanban
- Lich bieu theo tuan
- Nhac nho cong viec
- Thong ke tien do
- Quan ly nguoi dung cho tai khoan admin
- Xuat bao cao CSV, Excel va PDF

## Cach chay

Yeu cau:

- JDK 17 tro len
- Maven 3.8 tro len
- MariaDB/MySQL server dang chay

Ung dung chay theo mo hinh client-server:

- Server Java chay tren cong `9999`
- Client JavaFX ket noi toi server qua `localhost:9999`
- Server ket noi toi MariaDB/MySQL de xu ly du lieu

Mac dinh server ket noi database toi:

```text
host: 127.0.0.1
port: 3306
database: taskmanager
user: root
password:
```

Co the doi cau hinh database bang system properties khi chay server:

```bash
mvn exec:java -Dexec.mainClass="com.taskmanager.server.ServerMain" -Dtaskmanager.db.host=127.0.0.1 -Dtaskmanager.db.port=3306 -Dtaskmanager.db.name=taskmanager -Dtaskmanager.db.user=root -Dtaskmanager.db.password=your_password
```

Hoac bang bien moi truong:

```text
TASKMANAGER_DB_HOST
TASKMANAGER_DB_PORT
TASKMANAGER_DB_NAME
TASKMANAGER_DB_USER
TASKMANAGER_DB_PASSWORD
```

Chay ung dung tren Windows:

1. Mo terminal thu nhat va chay server:

```bash
run-server.bat
```

2. Mo terminal thu hai va chay client:

```bash
run-client.bat
```

Hoac chay bang Maven:

1. Chay server:

```bash
mvn exec:java -Dexec.mainClass="com.taskmanager.server.ServerMain"
```

2. Chay client:

```bash
mvn javafx:run
```

Luu y: can chay server truoc client. Neu server chua chay, client se bao loi khong ket noi duoc.

Build file jar:

```bash
mvn clean package -DskipTests
```

Sau khi build xong, file jar nam trong thu muc `target/`.

## Du lieu

Ung dung tu tao database `taskmanager` va cac bang can thiet neu user MySQL/MariaDB co quyen `CREATE DATABASE`.

## Ghi chu

- Sao luu/khoi phuc du lieu MySQL/MariaDB nen thuc hien bang HeidiSQL, `mysqldump`, hoac cong cu quan tri database tuong duong.
