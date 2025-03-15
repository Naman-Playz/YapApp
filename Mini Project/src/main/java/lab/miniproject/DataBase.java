package lab.miniproject;

import lab.SECRET.ROT42069;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class UserAlreadyExistsException extends Exception {
    UserAlreadyExistsException(String message) {
        super(message);
    }
}

class DataBase {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/YapAppDB";
    private static final String USER = "postgres";
    private static final String PASSWORD = "root";

    private ProcessBuilder processBuilder;
    private Connection connection;

    DataBase() {
        try{
            processBuilder = new ProcessBuilder(
                    "C:\\Program Files\\PostgreSQL\\17\\bin\\pg_ctl.exe", "start", "-D", "C:\\Program Files\\PostgreSQL\\17\\data" //Device specific change
            );
            processBuilder.start();
            System.out.println("Yap APP Database server started...");

            Thread.sleep(3000);
            try {
                Class.forName("org.postgresql.Driver");
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            this.connection = conn;
            System.out.println("Connected to Yap APP Database successfully!");
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, UserAlreadyExistsException{
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

        List<String> messages = db.getLast100Messages(channel_id, 0);
        for (String msg : messages) {
            System.out.println(msg);
        }

        db.removeUserFromChannel(userEmail2, channel_id);
        db.deleteChannel(channel_id);
        db.deleteUser(userEmail2);
        db.deleteUser(userEmail1);
    }

    public boolean createUser(String email, String username, String password) {
        String insertUserSQL = "INSERT INTO users (email, username, password) VALUES (?, ?, ?)";
        ROT42069 secret = new ROT42069();
        try (PreparedStatement insertStmt = connection.prepareStatement(insertUserSQL)) {
            String hashedPassword = secret.rot42069(password);

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

    public String createChannel(String channel_name, String email){
        int id = -1;
        String insertChannelSQL = "INSERT INTO channels (name) VALUES (?) RETURNING id";
        String insertUserSQL = "INSERT INTO user_channels (email, id) VALUES (?, ?)";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertChannelSQL);
             PreparedStatement insertUserStmt = connection.prepareStatement(insertUserSQL)) {

            insertStmt.setString(1, channel_name);
            try (ResultSet rs = insertStmt.executeQuery()) {
                if (rs.next()) {
                    id = rs.getInt("id");
                }
            }

            if (id == -1) {
                System.out.println("Failed to create channel.");
                return null;
            }

            String tableName = "channel_" + id;

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
            insertUserStmt.setInt(2, id);
            insertUserStmt.executeUpdate();

            System.out.println("Channel '" + channel_name + "' created successfully! (Table: " + tableName + ")");

            return tableName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean createDM(String email, String secondEmail) throws SQLException {
        String channel_name = email + "_" + secondEmail;
        String tableName = createChannel(channel_name, email);
        addUserToChannel(secondEmail, tableName);

        String changeChannelTypeSQL = "UPDATE channels SET type = ? WHERE id = ?";

        try(PreparedStatement changeChannelTypeStmt = connection.prepareStatement(changeChannelTypeSQL)) {
            changeChannelTypeStmt.setBoolean(1, true);
            changeChannelTypeStmt.setInt(2, Integer.parseInt(tableName.replace("channel_","")));
            int executed = changeChannelTypeStmt.executeUpdate();
            
            return executed > 0;
        }
    }

    public boolean getChannelType(String id) {
        String getChannelTypeSQL = "SELECT type FROM channels WHERE id = ?";

        try (PreparedStatement getChannelTypeStmt = connection.prepareStatement(getChannelTypeSQL)) {
            getChannelTypeStmt.setInt(1, Integer.parseInt(id.replace("channel_", "")));

            try (ResultSet rs = getChannelTypeStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("type");
                }

                return false;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean updatePassword(String email, String newPassword) {
        String updatePasswordSQL = "UPDATE users SET password = ? WHERE email = ?";
        ROT42069 secret = new ROT42069();
        String hashedPassword = secret.rot42069(newPassword); // Using PBKDF2

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

    public boolean addUserToChannel(String email, String id) {
        String addUserSQL = "INSERT INTO user_channels (email, id) VALUES (?, ?)";

        try (PreparedStatement addUserStmt = connection.prepareStatement(addUserSQL)) {
            addUserStmt.setString(1, email);
            addUserStmt.setInt(2, Integer.parseInt(id.replace("channel_","")));
            return addUserStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeUserFromChannel(String email, String id) {
        String removeUserSQL = "DELETE FROM user_channels WHERE email = ? AND id = ?";

        try (PreparedStatement removeUserStmt = connection.prepareStatement(removeUserSQL)) {
            removeUserStmt.setString(1, email);
            removeUserStmt.setInt(2, Integer.parseInt(id.replace("channel_","")));
            return removeUserStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteChannel(String id) {
        String deleteUsersSQL = "DELETE FROM user_channels WHERE id = ?";
        String dropTableSQL = "DROP TABLE IF EXISTS " + id;
        String deleteChannelSQL = "DELETE FROM channels WHERE id = ?";

        try (PreparedStatement deleteUsersStmt = connection.prepareStatement(deleteUsersSQL);
             Statement dropTableStmt = connection.createStatement();
             PreparedStatement deleteChannelStmt = connection.prepareStatement(deleteChannelSQL)) {

            deleteUsersStmt.setInt(1, Integer.parseInt(id.replace("channel_","")));
            deleteUsersStmt.executeUpdate();

            dropTableStmt.executeUpdate(dropTableSQL);

            deleteChannelStmt.setInt(1, Integer.parseInt(id.replace("channel_","")));
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

    public boolean sendMessage(String id, String senderEmail, String content) {
        String tableName = id;

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

    public List<String> getLast100Messages(String id, int range) {
        List<String> messages = new ArrayList<>();

        String fetchMessagesSQL = "SELECT sender_email, content, time FROM \"" + id + "\" " +
                "ORDER BY time DESC LIMIT 100 OFFSET ?";

        try (PreparedStatement stmt = connection.prepareStatement(fetchMessagesSQL)){
             stmt.setInt(1, range*100);
             try(ResultSet rs = stmt.executeQuery()) {

                 while (rs.next()) {
                     String sender = rs.getString("sender_email");
                     String content = rs.getString("content");
                     String timestamp = rs.getTimestamp("time").toString();

                     messages.add("[" + timestamp + "] " + sender + ": " + content);
                 }
             }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public ArrayList<String> getChannels() {
        ArrayList<String> channels = new ArrayList<>();
        String fetchChannelIdSQL = "SELECT id FROM channels";

        try (PreparedStatement fetchChannelIdStmt = connection.prepareStatement(fetchChannelIdSQL) ) {
            try (ResultSet rs = fetchChannelIdStmt.executeQuery()) {
                while (rs.next()) {
                    channels.add("channel_" + rs.getString("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return channels;
    }

    public ArrayList<String> getUsers(){
        ArrayList<String> users = new ArrayList<>();
        String fetchUsersSQL = "SELECT email FROM users";
        
        try (PreparedStatement fetchUsersStmt = connection.prepareStatement(fetchUsersSQL)) {
            try (ResultSet rs = fetchUsersStmt.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getString("email"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return users;
    }

    public ArrayList<String> getUserChannels(String email) throws SQLException {
        ArrayList<String> channels = new ArrayList<>();
        String fetchChannelsSQL = "SELECT id FROM user_channels WHERE email = ?";

        try (PreparedStatement fetchChannelsStmt = connection.prepareStatement(fetchChannelsSQL)){
            fetchChannelsStmt.setString(1, email);

            try (ResultSet rs = fetchChannelsStmt.executeQuery()) {
                while (rs.next()) {
                    channels.add("channel_" + rs.getString("id"));
                }
            }
        }

        return channels;
    }

    public ArrayList<String> getChannelUsers(String id) throws SQLException {
        ArrayList<String> users = new ArrayList<>();
        String fetchUsersSQL = "SELECT email FROM user_channels WHERE id = ?";

        try (PreparedStatement fetchUsersStmt = connection.prepareStatement(fetchUsersSQL)) {
            fetchUsersStmt.setString(1, id);

            try (ResultSet rs = fetchUsersStmt.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getString("email"));
                }
            }
        }

        return users;
    }

    public boolean DMExists(String email, String secondEmail) {
        String checkDmSQL = "SELECT id FROM channels WHERE id IN (SELECT id FROM user_channels WHERE email = ? INTERSECT SELECT id FROM user_channels WHERE email = ?) AND type = TRUE";
        try (PreparedStatement checkDmStmt = connection.prepareStatement(checkDmSQL)) {
            checkDmStmt.setString(1, email);
            checkDmStmt.setString(2, secondEmail);

            try (ResultSet rs = checkDmStmt.executeQuery()) {
                if (rs.next()) {
                    return true;
                }

                return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }
}