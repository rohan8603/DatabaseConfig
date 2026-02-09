# 🎓 Student Management System (Java + MySQL)

This is a console-based Student Management System built using Java, JDBC, MySQL, and Apache DBCP. It supports CRUD operations, transaction management, stored procedures, connection pooling, and database backup & recovery.

---

## 🚀 Features

- MySQL database schema design
- JDBC connection pooling (Apache DBCP)
- DAO (Data Access Object) pattern
- CRUD operations using PreparedStatement
- Transaction management (commit & rollback)
- Stored procedures for complex queries
- Batch inserts for performance
- Database backup & recovery
- Utility classes for reusable DB operations

---

## 🛠 Technologies Used

- Java (JDK 8+)
- MySQL (8.0+)
- JDBC
- Apache Commons DBCP

---

## 📂 Project Structure

student-management/
DatabaseConfig.java  
DBUtil.java  
Student.java  
StudentDAO.java  
DatabaseInitializer.java  
DatabaseBackupUtil.java  
StudentManagementApp.java  
README.md  

---

## ⚙️ Setup Instructions

### 1. Install Requirements
- Install Java JDK
- Install MySQL Server
- Download Apache Commons DBCP and MySQL Connector/J

---

### 2. Configure Database
Edit DatabaseConfig.java:

private static final String URL = "jdbc:mysql://localhost:3306/student_management";
private static final String USER = "root";
private static final String PASSWORD = "password";

---

### 3. Compile the Project

Open terminal / PowerShell in project folder:

javac *.java

---

### 4. Run the Application

java StudentManagementApp

---

## 🔐 Database Backup & Restore

Backup:
DatabaseBackupUtil.backupDatabase();

Restore:
DatabaseBackupUtil.restoreDatabase();

---

## 🧠 Learning Outcomes

This project helps you understand:
- JDBC architecture
- DAO design pattern
- Transaction handling
- Connection pooling
- SQL stored procedures
- Enterprise-style Java applications

---

## 📌 Author

Rohan kumar  
B.Tech Student | Java Developer  

---

## ⭐ License

This project is free to use for learning and educational purposes.
