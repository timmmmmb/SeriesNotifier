package seriesnotifier;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class Main extends Application {
private static String mode;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root;
        if("admin".equals(mode)){
            root = FXMLLoader.load(getClass().getResource("adminclient.fxml"));
        }else if("notify".equals(mode)){
            //updates all the series
            Controller con = new Controller();
            con.updateSeries();
            //gets all new episodes
            ArrayList<String> updatedSeries = con.getAllSeriesToNotifie(1);
            String message = "The following Series you are watching have been updated: ";
            //writes a new notification message
            for(String updateserie:updatedSeries){
                message = message + "\n"+updateserie;
            }
            if(updatedSeries.size() != 0){
                displayTray("New Episodes",message);
            }
            //displays a new notification
            System.exit(0);
            return;
        }else{
            root = FXMLLoader.load(getClass().getResource("client.fxml"));
        }
        primaryStage.setTitle("Series Notifier");
        primaryStage.setScene(new Scene(root, 600, 400));
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

    public static void displayTray(String caption, String message) throws AWTException, MalformedURLException {
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

        trayIcon.displayMessage(caption, message, TrayIcon.MessageType.INFO);
        tray.remove(trayIcon);
    }
}
