import java.sql.*;
import java.time.Instant;

class UserAlreadyExistsException extends Exception {
    UserAlreadyExistsException(String message) {
        super(message);
    }
}

class ChannelAlreadyExistsException extends Exception {
    ChannelAlreadyExistsException(String message) {
        super(message);
    }
}

class DataBase {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/YapApp.DB";
    private static final String USER = "yapApp_Server";
    private static final String PASSWORD = "YapApp@0103256000";

    private ProcessBuilder processBuilder;
    private Connection connection;

    DataBase() {
        try{
            processBuilder = new ProcessBuilder(
                    "/opt/homebrew/bin/pg_ctl", "start", "-D", "/opt/homebrew/var/postgresql@14" //Device specific change
            );
            processBuilder.start();
            System.out.println("Yap APP Database server started...");

            Thread.sleep(3000);

            Class.forName("org.postgresql.Driver");

            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            this.connection = conn;
            System.out.println("Connected to Yap APP Database successfully!");
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, UserAlreadyExistsException, ChannelAlreadyExistsException {
        DataBase db = new DataBase();

        String userEmail1 = "k.shivaram252@gmail.com";
        String username1 = "True_Lord";
        String password1 = "CertifiedYapper";

        String userEmail2 = "Naman.and68@gmail.com";
        String username2 = "Naman_playz";
        String password2 = "Poki";

        db.createUser(userEmail1, username1, password1);
        db.createUser(userEmail2, username2, password2);
        db.updatePassword(userEmail1, password2);
        db.updateUsername(userEmail2, "Naman_plays");

        String channelName = "SharkDuDu";
        String channel_id = db.createChannel(channelName, userEmail1);
        db.addUserToChannel(userEmail2, channel_id);

        db.sendMessage(channel_id, userEmail1, "Sup?");
        db.sendMessage(channel_id, userEmail2, "Nothing much, how are you?");
        db.sendMessage(channel_id, userEmail1, "Doing well. It's been awhile since we last talked.");
        db.sendMessage(channel_id, userEmail2, "Indeed.");
        db.sendMessage(channel_id, userEmail1, "Okay talk to you later.");
        db.sendMessage(channel_id, userEmail2, "Bye.");

        db.removeUserFromChannel(userEmail2, channel_id);
        db.deleteChannel(channel_id);
        db.deleteUser(userEmail2);
        db.deleteUser(userEmail1);
    }

    public boolean createUser(String email, String username, String password) {
        String insertUserSQL = "INSERT INTO users (email, username, password) VALUES (?, ?, ?)";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertUserSQL)) {
            String hashedPassword = SecurityUtils.hashPassword(password);

            insertStmt.setString(1, email);
            insertStmt.setString(2, username);
            insertStmt.setString(3, hashedPassword);

            int rowsAffected = insertStmt.executeUpdate();

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.getSQLState();
            return false;
        }
    }

    public String createChannel(String channel_name, String email) throws ChannelAlreadyExistsException {
        int channel_id = -1;
        String insertChannelSQL = "INSERT INTO channels (channel_name) VALUES (?) RETURNING channel_id";
        String insertUserSQL = "INSERT INTO user_channels (email, channel_id) VALUES (?, ?)";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertChannelSQL);
             PreparedStatement insertUserStmt = connection.prepareStatement(insertUserSQL)) {

            insertStmt.setString(1, channel_name);
            try (ResultSet rs = insertStmt.executeQuery()) {
                if (rs.next()) {
                    channel_id = rs.getInt("channel_id");
                }
            }

            if (channel_id == -1) {
                System.out.println("Failed to create channel.");
                return null;
            }

            String tableName = "channel_" + channel_id;

            String createTableSQL = String.format(
                    "CREATE TABLE \"%s\" (" +
                            "id SERIAL PRIMARY KEY, " +
                            "sender_email TEXT REFERENCES users(email) ON DELETE CASCADE, " +
                            "content TEXT NOT NULL, " +
                            "time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ");", tableName
            );

            try (Statement createStmt = connection.createStatement()) {
                createStmt.executeUpdate(createTableSQL);
            }

            insertUserStmt.setString(1, email);
            insertUserStmt.setInt(2, channel_id);
            insertUserStmt.executeUpdate();

            System.out.println("Channel '" + channel_name + "' created successfully! (Table: " + tableName + ")");

            return tableName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updatePassword(String email, String newPassword) {
        String updatePasswordSQL = "UPDATE users SET password = ? WHERE email = ?";

        String hashedPassword = SecurityUtils.hashPassword(newPassword); // Using PBKDF2

        try (PreparedStatement updateStmt = connection.prepareStatement(updatePasswordSQL)) {
            updateStmt.setString(1, hashedPassword);
            updateStmt.setString(2, email);

            int rowsAffected = updateStmt.executeUpdate();

            return rowsAffected > 0;
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

    public boolean addUserToChannel(String email, String channel_id) {
        String addUserSQL = "INSERT INTO user_channels (email, channel_id) VALUES (?, ?)";

        try (PreparedStatement addUserStmt = connection.prepareStatement(addUserSQL)) {
            addUserStmt.setString(1, email);
            addUserStmt.setInt(2, Integer.parseInt(channel_id.replace("channel_","")));
            return addUserStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeUserFromChannel(String email, String channel_id) {
        String removeUserSQL = "DELETE FROM user_channels WHERE email = ? AND channel_id = ?";

        try (PreparedStatement removeUserStmt = connection.prepareStatement(removeUserSQL)) {
            removeUserStmt.setString(1, email);
            removeUserStmt.setInt(2, Integer.parseInt(channel_id.replace("channel_","")));
            return removeUserStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteChannel(String channel_id) {
        String deleteUsersSQL = "DELETE FROM user_channels WHERE channel_id = ?";
        String dropTableSQL = "DROP TABLE IF EXISTS " + channel_id;
        String deleteChannelSQL = "DELETE FROM channels WHERE channel_id = ?";

        try (PreparedStatement deleteUsersStmt = connection.prepareStatement(deleteUsersSQL);
             Statement dropTableStmt = connection.createStatement();
             PreparedStatement deleteChannelStmt = connection.prepareStatement(deleteChannelSQL)) {

            deleteUsersStmt.setInt(1, Integer.parseInt(channel_id.replace("channel_","")));
            deleteUsersStmt.executeUpdate();

            dropTableStmt.executeUpdate(dropTableSQL);

            deleteChannelStmt.setInt(1, Integer.parseInt(channel_id.replace("channel_","")));
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

    public boolean sendMessage(String channel_id, String senderEmail, String content) {
        String tableName = channel_id;
        if (!isValidTableName(tableName)) {
            System.out.println("Invalid table name: " + tableName);
            return false;
        }

        String insertMessageSQL = "INSERT INTO \"" + tableName + "\" (sender_email, content, time) VALUES (?, ?, ?)";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertMessageSQL)) {
            insertStmt.setString(1, senderEmail);
            insertStmt.setString(2, content);
            insertStmt.setTimestamp(3, Timestamp.from(Instant.now()));

            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isValidTableName(String tableName) {
        return tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }
}