import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;

/* ===================== DATABASE CONFIG ===================== */
class DatabaseConfig {
    private static final String URL = "jdbc:mysql://localhost:3306/student_management";
    private static final String USER = "root";
    private static final String PASSWORD = "password";
    private static final int MAX_CONNECTIONS = 10;

    private static BasicDataSource dataSource;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dataSource = new BasicDataSource();
            dataSource.setUrl(URL);
            dataSource.setUsername(USER);
            dataSource.setPassword(PASSWORD);
            dataSource.setMinIdle(5);
            dataSource.setMaxIdle(MAX_CONNECTIONS);
            dataSource.setMaxOpenPreparedStatements(100);
            System.out.println("Database connection pool initialized!");
        } catch (Exception e) {
            System.out.println("Database initialization failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

/* ===================== DB UTILITY CLASS ===================== */
class DBUtil {
    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                System.out.println("Resource close failed: " + e.getMessage());
            }
        }
    }

    public static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                System.out.println("Rollback failed: " + e.getMessage());
            }
        }
    }
}

/* ===================== STUDENT MODEL ===================== */
class Student {
    private int id;
    private String studentId;
    private String firstName;
    private String lastName;
    private Date dateOfBirth;
    private String email;
    private String phone;
    private String department;
    private Date enrollmentDate;
    private List<Integer> selectedCourses;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Date getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(Date enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public List<Integer> getSelectedCourses() { return selectedCourses; }
    public void setSelectedCourses(List<Integer> selectedCourses) { this.selectedCourses = selectedCourses; }
}

/* ===================== STUDENT DAO ===================== */
class StudentDAO {

    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (student_id, first_name, last_name, date_of_birth, email, phone, department, enrollment_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, student.getStudentId());
            pstmt.setString(2, student.getFirstName());
            pstmt.setString(3, student.getLastName());
            pstmt.setDate(4, new java.sql.Date(student.getDateOfBirth().getTime()));
            pstmt.setString(5, student.getEmail());
            pstmt.setString(6, student.getPhone());
            pstmt.setString(7, student.getDepartment());
            pstmt.setDate(8, new java.sql.Date(student.getEnrollmentDate().getTime()));

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                ResultSet keys = pstmt.getGeneratedKeys();
                if (keys.next()) {
                    student.setId(keys.getInt(1));
                }

                if (student.getSelectedCourses() != null && !student.getSelectedCourses().isEmpty()) {
                    addStudentCourses(conn, student.getId(), student.getSelectedCourses());
                }

                conn.commit();
                return true;
            }
            conn.rollback();
            return false;

        } catch (SQLException e) {
            DBUtil.rollbackQuietly(conn);
            System.out.println("Error adding student: " + e.getMessage());
            return false;
        } finally {
            DBUtil.closeQuietly(pstmt);
            DBUtil.closeQuietly(conn);
        }
    }

    public Student getStudentById(int id) {
        String sql = "SELECT * FROM students WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractStudent(rs);
            }
            return null;

        } catch (SQLException e) {
            System.out.println("Error fetching student: " + e.getMessage());
            return null;
        }
    }

    public List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY last_name, first_name";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(extractStudent(rs));
            }
            return list;

        } catch (SQLException e) {
            System.out.println("Error fetching students: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Student> searchStudents(String keyword) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE first_name LIKE ? OR last_name LIKE ? OR student_id LIKE ? OR email LIKE ? OR department LIKE ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String search = "%" + keyword + "%";
            for (int i = 1; i <= 5; i++) {
                pstmt.setString(i, search);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(extractStudent(rs));
            }
            return list;

        } catch (SQLException e) {
            System.out.println("Search error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET first_name = ?, last_name = ?, date_of_birth = ?, email = ?, phone = ?, department = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, student.getFirstName());
            pstmt.setString(2, student.getLastName());
            pstmt.setDate(3, new java.sql.Date(student.getDateOfBirth().getTime()));
            pstmt.setString(4, student.getEmail());
            pstmt.setString(5, student.getPhone());
            pstmt.setString(6, student.getDepartment());
            pstmt.setInt(7, student.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteStudent(int id) {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Delete error: " + e.getMessage());
            return false;
        }
    }

    public void addStudentsInBatch(List<Student> students) throws SQLException {
        String sql = "INSERT INTO students (student_id, first_name, last_name, email, department) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Student s : students) {
                pstmt.setString(1, s.getStudentId());
                pstmt.setString(2, s.getFirstName());
                pstmt.setString(3, s.getLastName());
                pstmt.setString(4, s.getEmail());
                pstmt.setString(5, s.getDepartment());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            System.out.println("Batch insert successful!");

        } catch (SQLException e) {
            throw e;
        }
    }

    public List<Student> getStudentsByDepartment(String department) {
        List<Student> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             CallableStatement cstmt = conn.prepareCall("{call GetStudentsByDepartment(?)}")) {

            cstmt.setString(1, department);
            ResultSet rs = cstmt.executeQuery();
            while (rs.next()) {
                list.add(extractStudent(rs));
            }
            return list;

        } catch (SQLException e) {
            System.out.println("Stored procedure error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private Student extractStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setStudentId(rs.getString("student_id"));
        s.setFirstName(rs.getString("first_name"));
        s.setLastName(rs.getString("last_name"));
        s.setDateOfBirth(rs.getDate("date_of_birth"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setDepartment(rs.getString("department"));
        s.setEnrollmentDate(rs.getDate("enrollment_date"));
        return s;
    }

    private void addStudentCourses(Connection conn, int studentId, List<Integer> courseIds) throws SQLException {
        String sql = "INSERT INTO student_courses (student_id, course_id, enrollment_date) VALUES (?, ?, CURDATE())";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int courseId : courseIds) {
                pstmt.setInt(1, studentId);
                pstmt.setInt(2, courseId);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}



class DatabaseBackupUtil {
    private static final String DB_NAME = "student_management";
    private static final String USER = "root";
    private static final String PASSWORD = "password";
    private static final String BACKUP_PATH = "backup.sql";

    public static void backupDatabase() {
        try {
            String command = "mysqldump -u" + USER + " -p" + PASSWORD + " " + DB_NAME + " > " + BACKUP_PATH;
            Process p = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", command});
            int result = p.waitFor();
            if (result == 0) System.out.println("Database backup successful!");
            else System.out.println("Database backup failed!");
        } catch (Exception e) {
            System.out.println("Backup error: " + e.getMessage());
        }
    }

    public static void restoreDatabase() {
        try {
            String command = "mysql -u" + USER + " -p" + PASSWORD + " " + DB_NAME + " < " + BACKUP_PATH;
            Process p = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", command});
            int result = p.waitFor();
            if (result == 0) System.out.println("Database restored successfully!");
            else System.out.println("Database restore failed!");
        } catch (Exception e) {
            System.out.println("Restore error: " + e.getMessage());
        }
    }
}


class DatabaseInitializer {
    public static void initializeDatabase() {
        String[] sqls = {
            "CREATE DATABASE IF NOT EXISTS student_management",
            "USE student_management",
            """
            CREATE TABLE IF NOT EXISTS students (
                id INT PRIMARY KEY AUTO_INCREMENT,
                student_id VARCHAR(20) UNIQUE NOT NULL,
                first_name VARCHAR(50) NOT NULL,
                last_name VARCHAR(50) NOT NULL,
                date_of_birth DATE NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                phone VARCHAR(15),
                department VARCHAR(50) NOT NULL,
                enrollment_date DATE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS courses (
                id INT PRIMARY KEY AUTO_INCREMENT,
                course_code VARCHAR(20) UNIQUE NOT NULL,
                course_name VARCHAR(100) NOT NULL,
                credits INT NOT NULL,
                department VARCHAR(50) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS student_courses (
                student_id INT,
                course_id INT,
                enrollment_date DATE NOT NULL,
                grade VARCHAR(2),
                PRIMARY KEY (student_id, course_id),
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE PROCEDURE IF NOT EXISTS GetStudentsByDepartment(IN dept_name VARCHAR(50))
            BEGIN
                SELECT * FROM students WHERE department = dept_name ORDER BY last_name, first_name;
            END
            """
        };

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : sqls) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    System.out.println("SQL Error: " + e.getMessage());
                }
            }
            System.out.println("Database initialized successfully!");

        } catch (SQLException e) {
            System.out.println("Database initialization failed: " + e.getMessage());
        }
    }
}


    public static void main(String[] args) {

        
        DatabaseInitializer.initializeDatabase();

        StudentDAO dao = new StudentDAO();

        
        Student s = new Student();
        s.setStudentId("STU101");
        s.setFirstName("Rohan");
        s.setLastName("Rana");
        s.setDateOfBirth(java.sql.Date.valueOf("2005-02-02"));
        s.setEmail("rohan@example.com");
        s.setPhone("9876543210");
        s.setDepartment("Computer Science");
        s.setEnrollmentDate(new java.sql.Date(System.currentTimeMillis()));
        s.setSelectedCourses(Arrays.asList(1, 2));

        dao.addStudent(s);

        
        List<Student> students = dao.getAllStudents();
        for (Student stu : students) {
            System.out.println(stu.getFirstName() + " " + stu.getLastName());
        }

        
        DatabaseBackupUtil.backupDatabase();
    }
}
