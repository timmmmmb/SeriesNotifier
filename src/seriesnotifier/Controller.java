package seriesnotifier;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import java.sql.*;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ALL")
public class Controller {
    //Creates the object where we get inputs from javafx
    @FXML
    private ComboBox<String> serieslist;
    @FXML
    private ComboBox<String> userlist;
    @FXML
    private ComboBox<String> usersshow;
    @FXML
    private PasswordField userpassword;
    @FXML
    private TextField serieslink, seriesname, seriesepisode, seriesseason, usermail, username;
    @FXML
    private TableView<ResultDataType> table;
    @FXML
    private TableColumn<ResultDataType, String> truserSeason, truserEpisode, trseriesName;

    //this objects are used for accessing the DB
    private Connection con = null;
    private PreparedStatement pstmt = null;

    private String logdinusername;
    private int logdinuserid;

    /*
     * TODO: when a new association is created the dropdown should be limited to only show the series that are not connected also double check that in the sql code
     * TODO: Create a Client that has a Login
     * TODO: Client add new Series
     * TODO: Client add Table with series
     * TODO: Client remove entries
     */

    /**
     * This Function creates a new entry into the table users
     */
    public void addNewUser(){
        if("".equals(username.getText())||"".equals(usermail.getText())||"".equals(userpassword.getText())){
            System.out.println("one of the required text fields is empty");
            return;
        }
        try{
            if(con == null) {
                connectDatabase();
            }
            pstmt = con.prepareStatement(
                    "INSERT INTO users (name, email, password)" +
                            "VALUES (?, ?, ?);");

            pstmt.setString(1, username.getText());
            pstmt.setString(2, usermail.getText());
            pstmt.setString(3, userpassword.getText());
            pstmt.executeUpdate();
        }catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            closeStatement();
        }
    }


    public void registerUser(){
        addNewUser();
    }

    public void loginUser(){

    }
    /**
     * This Function creates a new entry into the table series
     */
    public void addNewSeries(){
        if("".equals(seriesname.getText())||"".equals(serieslink.getText())){
            System.out.println("one of the required text fields is empty");
            return;
        }
        try{
            if(con == null) {
                connectDatabase();
            }
            pstmt = con.prepareStatement(
                    "INSERT INTO series (name, link, currentseason, currentepisode)" +
                            "VALUES (?, ?, ?, ?);");

            int currentseason = getCurrentSeason(serieslink.getText());
            int currentepisode = getCurrentEpisode(serieslink.getText(), currentseason);
            pstmt.setString(1, seriesname.getText());
            pstmt.setString(2, serieslink.getText());
            pstmt.setInt(3, currentseason);
            pstmt.setInt(4, currentepisode);
            pstmt.executeUpdate();
        }catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            closeStatement();
        }

    }

    /**
     * This function updates the comboboxes with a select querry from the database
     */
    public void updateLists(){

        Statement stmt;
        ObservableList<String> users = FXCollections.observableArrayList ();
        ObservableList<String> series = FXCollections.observableArrayList ();
        try {
            if(con == null) {
                connectDatabase();
            }
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM users;");
            while (rs.next()) {
                users.add(rs.getString("name"));
            }
            rs = stmt.executeQuery("SELECT name FROM series;");
            while (rs.next()) {
                series.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        usersshow.setItems(users);
        userlist.setItems(users);
        serieslist.setItems(series);
    }

    /**
     * This Function creates a new entry into the table seriesusers
     */
    public void connectUsersToSeries(){
        try{
            if(con == null) {
                connectDatabase();
            }
            pstmt = con.prepareStatement(
                    "INSERT INTO seriesusers (seriesid, personid, currentseason, currentepisode)" +
                            "SELECT (SELECT id FROM series WHERE name = ? LIMIT 1), (SELECT id FROM users WHERE name = ? LIMIT 1), ?, ? " +
                            "WHERE NOT EXISTS (SELECT id FROM seriesusers as su WHERE su.seriesid = (SELECT series.id FROM series WHERE series.name = ? LIMIT 1) AND su.personid = (SELECT users.id FROM users WHERE users.name = ? LIMIT 1));");

            pstmt.setString(1, serieslist.getSelectionModel().getSelectedItem());
            pstmt.setString(2, userlist.getSelectionModel().getSelectedItem());
            pstmt.setInt(3, Integer.parseInt(seriesseason.getText()));
            pstmt.setInt(4, Integer.parseInt(seriesepisode.getText()));
            pstmt.setString(5, serieslist.getSelectionModel().getSelectedItem());
            pstmt.setString(6, userlist.getSelectionModel().getSelectedItem());
            pstmt.executeUpdate();
        }catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            closeStatement();
        }
    }

    /**
     * This function upadates the current episode and season of all series
     */
    public void updateSeries(){
        try{
            Statement stmt;
            if(con == null) {
                connectDatabase();
            }
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, link FROM series;");
            while (rs.next()) {
                int season = getCurrentSeason(rs.getString("link"));
                int episode = getCurrentEpisode(rs.getString("link"),season);
                pstmt = con.prepareStatement("UPDATE series SET currentseason = ?, currentepisode = ? WHERE id = ?");
                pstmt.setInt(1, season);
                pstmt.setInt(2, episode);
                pstmt.setInt(3, rs.getInt("id"));
                pstmt.executeUpdate();
            }
        }catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            closeStatement();
        }
        fillTable();
    }

    /**
     * This Function adds fills the Table with the required data from the database
     */
    public void fillTable(){
        initialize();

        Statement stmt;
        try {
            if(con == null) {
                connectDatabase();
            }
            if(usersshow.getSelectionModel().getSelectedItem()==null){
                return;
            }
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT  s.name as Series, su.currentseason as 'User Season', su.currentepisode as 'User Episode', s.currentseason as 'Current Season', s.currentepisode as 'Current Episode' FROM series as s\n" +
                    "LEFT JOIN seriesusers as su ON su.seriesid = s.id\n" +
                    "WHERE su.personid = (SELECT ID FROM users WHERE name = '"+ usersshow.getSelectionModel().getSelectedItem() +"' LIMIT 1);");

            table.getItems().clear();
            while (rs.next()) {
                table.getItems().add(new ResultDataType(rs.getString(1), String.valueOf(rs.getInt(2)), String.valueOf(rs.getInt(3)), rs.getInt(4), rs.getInt(5)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This Funcition removes the currentrly selected row
     */
    public void deleteRow(){
        ResultDataType selectedItem = table.getSelectionModel().getSelectedItem();
        if(usersshow == null || usersshow.getValue() == null || selectedItem == null || selectedItem.trseriesname == null){
            return;
        }
        try{
            //remove from DB
            Statement stmt;
            if(con == null) {
                connectDatabase();
            }
            stmt = con.createStatement();
            pstmt = con.prepareStatement("DELETE FROM seriesusers WHERE personid = (SELECT id FROM users WHERE name = ?) AND seriesid = (SELECT id FROM series WHERE name = ?)");
            pstmt.setString(1, usersshow.getValue());
            pstmt.setString(2, selectedItem.trseriesname);
            pstmt.executeUpdate();

        }catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            closeStatement();

            //remove from table
            table.getItems().remove(selectedItem);
        }
    }

    public class ResultDataType {
        private String trseriesname;
        private String usersseason;
        private String usersepisode;
        private int currentseason;
        private int currentepisode;
        ResultDataType(String trseriesname, String usersseason, String usersepisode, int currentseason, int currentepisode) {
            this.trseriesname = trseriesname ;
            this.usersseason = usersseason ;
            this.usersepisode = usersepisode ;
            this.currentseason = currentseason ;
            this.currentepisode = currentepisode ;
        }

        public int getCurrentepisode() {
            return currentepisode;
        }

        public int getCurrentseason() {
            return currentseason;
        }

        public String getUsersepisode() {
            return usersepisode;
        }

        public String getUsersseason() {
            return usersseason;
        }

        public String getTrseriesname() {
            return trseriesname;
        }

        public void setUsersseason(String usersseason){
            this.usersseason= usersseason;
        }

        public void setUsersepisode(String usersepisode) {
            this.usersepisode = usersepisode;
        }
    }

    /**
     * a helper function that closes a statement.
     */
    private void closeStatement(){
        try {
            if (pstmt != null) pstmt.close();
        }catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    /**
     * creates a connection to the Database
     * @throws SQLException
     */
    private void connectDatabase(){
        try{
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/notifier?useSSL=false&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC","root","root");
            System.out.println("Connected");
        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    /**
     * gets the current season of a series by its link
     * @param link
     * @return
     */
    private int getCurrentSeason(String link){
        int currentseason = 1;
        try {
            URL url = new URL(link);
            String body = getBody(url);

            String serieslink = link.replaceAll("https://bs.to/serie/", "");
            String patterntext = "<li><a href=\"serie/"+serieslink+"/";
            Pattern pattern = Pattern.compile(patterntext+"(\\d+)");
            Matcher matcher = pattern.matcher(body);
            while (matcher.find()) {
                int result = Integer.parseInt(body.substring(matcher.start()+patterntext.length(), matcher.end()));
                if(result>currentseason) {
                    currentseason = result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentseason;
    }

    /**
     * Gets the newest episode of the specified season
     * @param link
     * @param season
     * @return
     */
    private int getCurrentEpisode(String link, int season){
        int currentepisode = 1;
        try {
            URL url = new URL(link+"/"+season);
            String body = getBody(url);
            String serieslink = link.replaceAll("https://bs.to/serie/", "");
            String patterntext = "<a href=\"serie/"+serieslink+"/"+season+"/";
            Pattern pattern = Pattern.compile(patterntext+"(\\d+)");
            Matcher matcher = pattern.matcher(body);
            while (matcher.find()) {
                int result = Integer.parseInt(body.substring(matcher.start()+patterntext.length(), matcher.end()));
                if(result>currentepisode) {
                    currentepisode = result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentepisode;
    }

    /**
     * This Function gets the body of a link as a String
     * @param url
     * @return
     */
    private String getBody(URL url){
        try {
            URLConnection urlcon = url.openConnection();
            InputStream in = urlcon.getInputStream();
            String encoding = urlcon.getContentEncoding();  // ** WRONG: should use "con.getContentType()" instead but it returns something like "text/html; charset=UTF-8" so this value must be parsed to extract the actual encoding
            encoding = encoding == null ? "UTF-8" : encoding;
            String body = IOUtils.toString(in, encoding);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @FXML
    void keyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case DELETE:
            case BACK_SPACE:
                deleteRow();
                break;
            default:
                break;
        }
    }

    /**
     * This function is started when the application is initialized
     */
    public void initialize(){
        table.setRowFactory(new Callback<>() {
            @Override
            public TableRow<ResultDataType> call(TableView<ResultDataType> tableView) {

                return new TableRow<>() {
                    @Override
                    protected void updateItem(ResultDataType person, boolean empty) {
                        super.updateItem(person, empty);
                        if (person != null && (person.getCurrentepisode() > Integer.parseInt(person.getUsersepisode()) || person.getCurrentseason() > Integer.parseInt(person.getUsersseason()))) {
                            if (!getStyleClass().contains("highlightedRow")) {
                                getStyleClass().add("highlightedRow");
                            }
                        } else {
                            getStyleClass().removeAll(Collections.singleton("highlightedRow"));
                            getStyleClass().add("defaultTableStyle");
                        }
                    }
                };
            }
        });

        truserSeason.setCellValueFactory(new PropertyValueFactory<>("usersseason"));

        truserSeason.setOnEditCommit(
            (TableColumn.CellEditEvent<ResultDataType, String> t) -> {
                if (isInteger(t.getNewValue())) {
                    t.getTableView().getItems().get(t.getTablePosition().getRow()).setUsersseason(t.getNewValue());
                    updateCurrentSeason(t.getNewValue(), trseriesName.getCellData(t.getRowValue()));
                    t.getTableView().getColumns().get(0).setVisible(false);
                    t.getTableView().getColumns().get(0).setVisible(true);
                }
            }
        );
        truserSeason.setCellFactory(TextFieldTableCell.forTableColumn());

        truserEpisode.setCellValueFactory(new PropertyValueFactory<>("usersepisode"));

        truserEpisode.setOnEditCommit(
            (TableColumn.CellEditEvent<ResultDataType, String> t) -> {
                if(isInteger(t.getNewValue())){
                    t.getTableView().getItems().get(t.getTablePosition().getRow()).setUsersepisode(t.getNewValue());
                    updateCurrentEpisode(t.getNewValue(), trseriesName.getCellData(t.getRowValue()));
                    t.getTableView().getColumns().get(0).setVisible(false);
                    t.getTableView().getColumns().get(0).setVisible(true);
                }
            }
        );
        truserEpisode.setCellFactory(TextFieldTableCell.forTableColumn());
    }

    private void updateCurrentSeason(String newseason, String seriesname){
        try{
            Statement stmt;
            if(con == null) {
                connectDatabase();
            }
            stmt = con.createStatement();
            pstmt = con.prepareStatement("UPDATE seriesusers SET currentseason = ? WHERE personid = (SELECT id FROM users WHERE name = ?) AND seriesid = (SELECT id FROM series WHERE name = ?)");
            pstmt.setInt(1, Integer.parseInt(newseason));
            pstmt.setString(2, usersshow.getValue());
            pstmt.setString(3, seriesname);
            pstmt.executeUpdate();

        }catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            closeStatement();
        }
    }

    private void updateCurrentEpisode(String newepisode, String seriesname){
        try{
            Statement stmt;
            if(con == null) {
                connectDatabase();
            }
            stmt = con.createStatement();
            pstmt = con.prepareStatement("UPDATE seriesusers SET currentepisode = ? WHERE personid = (SELECT id FROM users WHERE name = ?) AND seriesid = (SELECT id FROM series WHERE name = ?)");
            pstmt.setInt(1, Integer.parseInt(newepisode));
            pstmt.setString(2, usersshow.getValue());
            pstmt.setString(3, seriesname);
            pstmt.executeUpdate();

        }catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            closeStatement();
        }
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        if (str.isEmpty()) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (str.length() == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}