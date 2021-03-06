package seriesnotifier;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.jimmc.jshortcut.JShellLink;

import java.awt.*;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class Main extends Application {
private static String mode;

    /**
     * TODO: enable the reset of your password per email
     */

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root;
        /*if("admin".equals(mode)){
            root = FXMLLoader.load(getClass().getResource("adminclient.fxml"));
        }else*/ if("notify".equals(mode)){
            //updates all the series
            Controller con = new Controller();
            //gets the last loggdin person from the file user.txt
            String usermd5 = con.readFile("user.txt");
            usermd5 = usermd5.replace("\n","");
            int userid = con.getUserIdFromMD5(usermd5);
            con.updateSeriesPerUser(userid);
            //gets all new episodes
            ArrayList<String> updatedSeries = con.getAllSeriesToNotifie(userid);
            StringBuilder message = new StringBuilder("The following Series you are watching have been updated: ");
            //writes a new notification message
            for(String updateserie:updatedSeries){
                message.append("\n").append(updateserie);
            }
            //displays a new notification
            if(updatedSeries.size() != 0){
                displayTray(message.toString());
            }
            con.afterNotifie(userid);
            System.exit(0);
            return;
        }else if("update".equals(mode)){
            Controller con = new Controller();
            con.updateAllSeries();
            System.exit(0);
            return;
        }else if("addseries".equals(mode)){
            Controller con = new Controller();
            String[] genres = {"Sitcom"};
            for(String genre:genres){
                con.getSeries(genre);
            }
            System.exit(0);
            return;
        }else if("sendmail".equals(mode)){
            Controller con = new Controller();
            con.sendMail("");
            System.exit(0);
            return;
        }else{
            try {
                JShellLink link = new JShellLink();
                String filePath = JShellLink.getDirectory("") + new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
                link.setFolder(System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup");
                link.setName("SeriesNotifier");
                link.setPath(filePath);
                link.setArguments("notify");
                link.save();
            } catch (URISyntaxException e) {
                System.out.println(e.getMessage());
            }
            root = FXMLLoader.load(getClass().getResource("client.fxml"));
        }
        primaryStage.setTitle("Series Notifier");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        if(args.length != 0){
            mode = args[0];
        }else{
            mode = "";
        }
        launch(args);
    }

    private void displayTray(String message) throws AWTException {
        //Obtain only one instance of the SystemTray object
        SystemTray tray = SystemTray.getSystemTray();

        //If the icon is a file
        Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
        //Alternative (if the icon is on the classpath):
        //Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("icon.png"));

        TrayIcon trayIcon = new TrayIcon(image, "SeriesNotifier");
        //Let the system resize the image if needed
        trayIcon.setImageAutoSize(true);
        //Set tooltip text for the tray icon
        trayIcon.setToolTip("SeriesNotifier");
        tray.add(trayIcon);

        trayIcon.displayMessage("New Episodes", message, TrayIcon.MessageType.INFO);
        tray.remove(trayIcon);
    }

}
