import java.sql.*;
import java.time.Instant;

class DataBase {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/YapApp.DB";
    private static final String USER = "yapApp_Server";
    private static final String PASSWORD = "YapApp@0103256000";

    private ProcessBuilder processBuilder;
    private Connection connection;

    DataBase() {
        try{
            processBuilder = new ProcessBuilder(
                    "/opt/homebrew/bin/pg_ctl", "start", "-D", "/opt/homebrew/var/postgresql@14"
            );
            processBuilder.start();
            System.out.println("Yap APP Database server started...");

            Thread.sleep(3000);

            this.connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            System.out.println("Connected to Yap APP Database successfully!");
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {}

    public boolean createUser(String email, String username, String password, Connection connection) {
        String insertUserSQL = "INSERT INTO users (email, username, password) VALUES (?, ?, ?)";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertUserSQL)) {
            String hashedPassword = SecurityUtils.hashPassword(password);

            insertStmt.setString(1, email);
            insertStmt.setString(2, username);
            insertStmt.setString(3, hashedPassword);

            int rowsAffected = insertStmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("User '" + username + "' created successfully!");
                return true;
            } else {
                System.out.println("Failed to create user.");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createChannel(String channelName, String email) {
        String channelId = null;
        String insertChannelSQL = "INSERT INTO channels (channelName) VALUES (?) RETURNING channelId";
        String createTableSQL = String.format(
                "CREATE TABLE %s (" +
                        "id SERIAL PRIMARY KEY, " +
                        "sender_email TEXT REFERENCES users(email) ON DELETE CASCADE, " +
                        "content TEXT NOT NULL, " +
                        "time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");", channelName
        );
        String insertUserSQL = "INSERT INTO user_channels (email, channelId) VALUES (?, ?)";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertChannelSQL);
             Statement createStmt = connection.createStatement();
             PreparedStatement insertUserStmt = connection.prepareStatement(insertUserSQL)) {

            insertStmt.setString(1, channelName);
            ResultSet rs = insertStmt.executeQuery();
            if (rs.next()) {
                channelId = rs.getString("channelId");
            }
            rs.close();

            if (channelId == null) {
                System.out.println("Failed to create channel.");
                return false;
            }

            createStmt.executeUpdate(createTableSQL);

            insertUserStmt.setString(1, email);
            insertUserStmt.setString(2, channelId);
            insertUserStmt.executeUpdate();

            System.out.println("Channel '" + channelName + "' created successfully!");

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePassword(String email, String newPassword) {
        String updatePasswordSQL = "UPDATE users SET password = ? WHERE email = ?";

        String hashedPassword = SecurityUtils.hashPassword(newPassword); // Using PBKDF2

        try (PreparedStatement updateStmt = connection.prepareStatement(updatePasswordSQL)) {
            updateStmt.setString(1, hashedPassword);
            updateStmt.setString(2, email);

            int rowsAffected = updateStmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Password updated successfully for user: " + email);
                return true;
            } else {
                System.out.println("No user found with email: " + email);
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUsername(String email, String newUsername) {
        String sql = "UPDATE users SET username = ? WHERE email = ?";

        try (PreparedStatement updateStmt = connection.prepareStatement(sql)) {
            updateStmt.setString(1, newUsername);
            updateStmt.setString(2, email);

            int rowsUpdated = updateStmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addUserToChannel(String email, String channelId) {
        String addUserSQL = "INSERT INTO user_channels (email, channelId) VALUES (?, ?)";

        try (PreparedStatement addUserStmt = connection.prepareStatement(addUserSQL)) {
            addUserStmt.setString(1, email);
            addUserStmt.setString(2, channelId);
            return addUserStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeUserFromChannel(String email, String channelId) {
        String removeUserSQL = "DELETE FROM user_channels WHERE email = ? AND channelId = ?";

        try (PreparedStatement removeUserStmt = connection.prepareStatement(removeUserSQL)) {
            removeUserStmt.setString(1, email);
            removeUserStmt.setString(2, channelId);
            return removeUserStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteChannel(String channelId) {
        String deleteUsersSQL = "DELETE FROM user_channels WHERE channelId = ?";
        String dropTableSQL = "DROP TABLE IF EXISTS " + channelId;
        String deleteChannelSQL = "DELETE FROM channels WHERE channelId = ?";

        try (PreparedStatement deleteUsersStmt = connection.prepareStatement(deleteUsersSQL);
             Statement dropTableStmt = connection.createStatement();
             PreparedStatement deleteChannelStmt = connection.prepareStatement(deleteChannelSQL)) {

            deleteUsersStmt.setString(1, channelId);
            deleteUsersStmt.executeUpdate();

            dropTableStmt.executeUpdate(dropTableSQL);

            deleteChannelStmt.setString(1, channelId);
            int rowsDeleted = deleteChannelStmt.executeUpdate();

            return rowsDeleted > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(String email) {
        String deleteFromUserChannelsSQL = "DELETE FROM user_channels WHERE email = ?";
        String deleteUserSQL = "DELETE FROM users WHERE email = ?";

        try (PreparedStatement deleteUserChannelsStmt = connection.prepareStatement(deleteFromUserChannelsSQL);
             PreparedStatement deleteUserStmt = connection.prepareStatement(deleteUserSQL)) {

            deleteUserChannelsStmt.setString(1, email);
            deleteUserChannelsStmt.executeUpdate();

            deleteUserStmt.setString(1, email);
            int rowsDeleted = deleteUserStmt.executeUpdate();

            return rowsDeleted > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendMessage(String channelId, String senderEmail, String content) {
        String insertMessageSQL = "INSERT INTO " + channelId + " (sender_email, content, time) VALUES (?, ?, ?)";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertMessageSQL)) {
            insertStmt.setString(1, senderEmail);
            insertStmt.setString(2, content);

            Timestamp currentTime = Timestamp.from(Instant.now());
            insertStmt.setTimestamp(3, currentTime);

            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}