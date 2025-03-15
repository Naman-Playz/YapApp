import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class UserAlreadyExistsException extends Exception { //Exception that is raised when a account is being created with an existing email.
    UserAlreadyExistsException(String message) {
        super(message);
    }
}

class DataBase {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/YapApp.DB"; //Location of the Database
    private static final String USER = "yapApp_Server"; //Username that has access to the Database
    private static final String PASSWORD = "YapApp@0103256000"; //Password needed to connect to the Database

    private ProcessBuilder processBuilder; //To start the Postgress server if not already activated
    private Connection connection; //Object that has connection to the server

    DataBase() {
        try{
            processBuilder = new ProcessBuilder(
                    "/opt/homebrew/bin/pg_ctl", "start", "-D", "/opt/homebrew/var/postgresql@14" //Device specific change
            );
            processBuilder.start(); //Starts the Postgresql server
            System.out.println("Yap APP Database server started...");

            Thread.sleep(3000); //Sleep to allow the server to start (!Important)

            Class.forName("org.postgresql.Driver"); //Specify which driver to use

            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD); //Connect to the Database YapApp.DB
            this.connection = conn;
            System.out.println("Connected to Yap APP Database successfully!");
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, UserAlreadyExistsException{ //Main is for texting the functionality of the code
        DataBase db = new DataBase(); //Create a connection to the DB

        String userEmail1 = "k.shivaram252@gmail.com"; //Specify credentials of the first user
        String username1 = "True_Lord";
        String password1 = "CertifiedYapper";

        String userEmail2 = "Naman.and68@gmail.com"; //Specify credentials of the second user
        String username2 = "Naman_playz";
        String password2 = "Poki";

        db.createUser(userEmail1, username1, password1); //Create the accounts
        db.createUser(userEmail2, username2, password2);
        db.updatePassword(userEmail1, password2); //Change password and username
        db.updateUsername(userEmail2, "Naman_plays");

        String channelName = "SharkDuDu"; //Channel Details
        String channel_id = db.createChannel(channelName, userEmail1); //Create the channel
        db.addUserToChannel(userEmail2, channel_id); //Add user 2 to the channel

        db.sendMessage(channel_id, userEmail1, "Sup?"); //A conversation in the channel between both the users
        db.sendMessage(channel_id, userEmail2, "Nothing much, how are you?");
        db.sendMessage(channel_id, userEmail1, "Doing well. It's been awhile since we last talked.");
        db.sendMessage(channel_id, userEmail2, "Indeed.");
        db.sendMessage(channel_id, userEmail1, "Okay talk to you later.");
        db.sendMessage(channel_id, userEmail2, "Bye.");

        List<String> messages = db.getLast100Messages(channel_id); //Get a list of the past 100 messages sent in the channel
        for (String msg : messages) {
            System.out.println(msg);
        }

        db.removeUserFromChannel(userEmail2, channel_id); //Remove the user from the channel
        db.deleteChannel(channel_id); //Delete the channel
        db.deleteUser(userEmail2); //Delete the users
        db.deleteUser(userEmail1);
    }

    public boolean createUser(String email, String username, String password) { //Create a user account with a unique Email key
        String insertUserSQL = "INSERT INTO users (email, username, password) VALUES (?, ?, ?)"; //SQL Query to insert the account in the DB, Will throw an SQLException if a user with that email already exists

        try (PreparedStatement insertStmt = connection.prepareStatement(insertUserSQL)) { //Precompile the statment
            String hashedPassword = SecurityUtils.hashPassword(password); //Hashes the password before storing it

            insertStmt.setString(1, email); //input the details into the query
            insertStmt.setString(2, username);
            insertStmt.setString(3, hashedPassword);

            int rowsAffected = insertStmt.executeUpdate(); //Rows affected shows if any changes have been made due to the SQL query

            return rowsAffected > 0;
        } catch (SQLException e) { //Duplicate users in table will cause this exception
            e.getSQLState();
            return false;
        }
    }

    public String createChannel(String channel_name, String email){ //Create a new channel table
        int channel_id = -1; //Fetch which row the new channel was added to in the table channels
        String insertChannelSQL = "INSERT INTO channels (channel_name) VALUES (?) RETURNING channel_id"; //Query to insert channel name into the table and return the column_id
        String insertUserSQL = "INSERT INTO user_channels (email, channel_id) VALUES (?, ?)"; //Query to insert channel-user relationship in the user_channels table

        try (PreparedStatement insertStmt = connection.prepareStatement(insertChannelSQL); //Prepare SQL Query
             PreparedStatement insertUserStmt = connection.prepareStatement(insertUserSQL)) {

            insertStmt.setString(1, channel_name);
            try (ResultSet rs = insertStmt.executeQuery()) { //Execute the Query
                if (rs.next()) { //Channel_id you column id if the query gets executed
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
            ); //Create Table with channel_id name

            try (Statement createStmt = connection.createStatement()) { //Execute the query
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

    public boolean updatePassword(String email, String newPassword) { //Update users password
        String updatePasswordSQL = "UPDATE users SET password = ? WHERE email = ?"; //SQL Query

        String hashedPassword = SecurityUtils.hashPassword(newPassword); //Using PBKDF2 to hash the password

        try (PreparedStatement updateStmt = connection.prepareStatement(updatePasswordSQL)) { //Create Query
            updateStmt.setString(1, hashedPassword);
            updateStmt.setString(2, email);

            int rowsAffected = updateStmt.executeUpdate(); //Check if the query got executed

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUsername(String email, String newUsername) { //Update users username
        String sql = "UPDATE users SET username = ? WHERE email = ?"; //SQL Query

        try (PreparedStatement updateStmt = connection.prepareStatement(sql)) { //Create the statment
            updateStmt.setString(1, newUsername);
            updateStmt.setString(2, email);

            int rowsUpdated = updateStmt.executeUpdate(); //Execute the query
            return rowsUpdated > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addUserToChannel(String email, String channel_id) { //Add a user to the channel
        String addUserSQL = "INSERT INTO user_channels (email, channel_id) VALUES (?, ?)"; //Create Query

        try (PreparedStatement addUserStmt = connection.prepareStatement(addUserSQL)) { //Create statment
            addUserStmt.setString(1, email);
            addUserStmt.setInt(2, Integer.parseInt(channel_id.replace("channel_","")));
            return addUserStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeUserFromChannel(String email, String channel_id) { //Remove user from channel
        String removeUserSQL = "DELETE FROM user_channels WHERE email = ? AND channel_id = ?"; //SQL Query
        String useerCountSQL = "SELECT COUNT(*) FROM user_channels WHERE channel_id = ?";

        try (PreparedStatement removeUserStmt = connection.prepareStatement(removeUserSQL)) {//Create statment
            removeUserStmt.setString(1, email);
            removeUserStmt.setInt(2, Integer.parseInt(channel_id.replace("channel_",""))); //Replace 'channel_' from 'channel_{no}' and just get a int {no}

            boolean rowsRemoved = removeUserStmt.executeUpdate() > 0;
            if (rowsRemoved) {
                try(PreparedStatement userCountStmt = connection.prepareStatement(useerCountSQL)) {
                    userCountStmt.setString(1, channel_id);
                    try(ResultSet rs = userCountStmt.executeQuery()) {
                        int userCount = 0;
                        if (rs.next()) {
                            userCount = rs.getInt(1);
                        }

                        if (userCount == 0) this.deleteChannel(channel_id);
                    }
                }
            }

            return rowsRemoved;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteChannel(String channel_id) { //Delete channel from DB
        String deleteUsersSQL = "DELETE FROM user_channels WHERE channel_id = ?"; //SQL query to delete user-email relationship from user_channel where channel = channel_id
        String dropTableSQL = "DROP TABLE IF EXISTS " + channel_id; //SQL query to delete the channel_id table
        String deleteChannelSQL = "DELETE FROM channels WHERE channel_id = ?"; //SQL query to delete log of channel from channels table

        try (PreparedStatement deleteUsersStmt = connection.prepareStatement(deleteUsersSQL); //Create statments
             Statement dropTableStmt = connection.createStatement();
             PreparedStatement deleteChannelStmt = connection.prepareStatement(deleteChannelSQL)) {

            deleteUsersStmt.setInt(1, Integer.parseInt(channel_id.replace("channel_","")));
            deleteUsersStmt.executeUpdate();

            dropTableStmt.executeUpdate(dropTableSQL); //Execute statment

            deleteChannelStmt.setInt(1, Integer.parseInt(channel_id.replace("channel_","")));
            int rowsDeleted = deleteChannelStmt.executeUpdate();

            return rowsDeleted > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(String email) { //Delete user from DB
        String deleteFromUserChannelsSQL = "DELETE FROM user_channels WHERE email = ?"; //
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

    public List<String> getLast100Messages(String channel_id) {
        List<String> messages = new ArrayList<>();

        String fetchMessagesSQL = "SELECT sender_email, content, time FROM \"" + channel_id + "\" " +
                "ORDER BY time DESC LIMIT 100";

        try (PreparedStatement stmt = connection.prepareStatement(fetchMessagesSQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String sender = rs.getString("sender_email");
                String  content = rs.getString("content");
                String timestamp = rs.getTimestamp("time").toString();

                messages.add("[" + timestamp + "] " + sender + ": " + content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public ArrayList<String> getChannels() {
        ArrayList<String> channels = new ArrayList<>();
        String fetchChannelIdSQL = "SELECT channel_id FROM channels";

        try (PreparedStatement fetchChannelIdStmt = connection.prepareStatement(fetchChannelIdSQL) ) {
            try (ResultSet rs = fetchChannelIdStmt.executeQuery()) {
                while (rs.next()) {
                    channels.add("channel_" + rs.getString("channel_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return channels;
    }

    public ArrayList<String> getUsers(String channel_id) {
        ArrayList<String> users = new ArrayList<>();
        String fetchUsersSQL = "SELECT user_id FROM users WHERE channel_id = ?";

        try(PreparedStatement fetchUsersStmt = connection.prepareStatement(fetchUsersSQL)) {
            fetchUsersStmt.setString(1, channel_id);
            try (ResultSet rs = fetchUsersStmt.executeQuery()) {
                while (rs.next()) {
                    users.add("user_" + rs.getString("user_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return users;
    }
}