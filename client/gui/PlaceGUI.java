package place.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.network.PlaceExchange;
import place.network.PlaceRequest;
import place.server.PlaceServer;

import static java.lang.Integer.parseInt;

/**
 * The GUI of the Place Game, which includes the Login screen and the Game screen (Observer) with colors.
 * @author Carson Bloomingdale
 * @author Bryce Thompson
 */

public class PlaceGUI extends Application implements Observer{
    // Array of PlaceTiles used for accessing the color of that tile currently and the row and column.
    private PlaceBoard gameBoard;

    //Array of PlaceColors used for the transmission of the PlaceColor as a serialized object.
    private PlaceColor[] colors2 = new PlaceColor[]{PlaceColor.BLACK,PlaceColor.GRAY,PlaceColor.SILVER,PlaceColor.WHITE,PlaceColor.MAROON,PlaceColor.RED,PlaceColor.OLIVE,
            PlaceColor.YELLOW,PlaceColor.GREEN,PlaceColor.LIME,PlaceColor.TEAL,PlaceColor.AQUA,PlaceColor.NAVY,PlaceColor.BLUE,PlaceColor.PURPLE,PlaceColor.FUCHSIA};

    // Array of Strings of PlaceColors converted to hexidecimal form in order to color the buttons and tiles.
    private String[] colors = new String[]{String.format("#%02x%02x%02x", PlaceColor.BLACK.getRed(), PlaceColor.BLACK.getGreen(),PlaceColor.BLACK.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.GRAY.getRed(), PlaceColor.GRAY.getGreen(),PlaceColor.GRAY.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.SILVER.getRed(), PlaceColor.SILVER.getGreen(),PlaceColor.SILVER.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.WHITE.getRed(),PlaceColor.WHITE.getGreen(),PlaceColor.WHITE.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.MAROON.getRed(), PlaceColor.MAROON.getGreen(),PlaceColor.MAROON.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.RED.getRed(), PlaceColor.RED.getGreen(),PlaceColor.RED.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.OLIVE.getRed(), PlaceColor.OLIVE.getGreen(),PlaceColor.OLIVE.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.YELLOW.getRed(), PlaceColor.YELLOW.getGreen(),PlaceColor.YELLOW.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.GREEN.getRed(), PlaceColor.GREEN.getGreen(),PlaceColor.GREEN.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.LIME.getRed(), PlaceColor.LIME.getGreen(),PlaceColor.LIME.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.TEAL.getRed(), PlaceColor.TEAL.getGreen(),PlaceColor.TEAL.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.AQUA.getRed(), PlaceColor.AQUA.getGreen(),PlaceColor.AQUA.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.NAVY.getRed(), PlaceColor.NAVY.getGreen(),PlaceColor.NAVY.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.BLUE.getRed(), PlaceColor.BLUE.getGreen(),PlaceColor.BLUE.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.PURPLE.getRed(), PlaceColor.PURPLE.getGreen(),PlaceColor.PURPLE.getBlue()),
            String.format("#%02x%02x%02x", PlaceColor.FUCHSIA.getRed(), PlaceColor.FUCHSIA.getGreen(),PlaceColor.FUCHSIA.getBlue())};

    //the current placecolor that the user has
    private PlaceColor color = PlaceColor.BLACK;
    //the current hexidecimal color that the user has
    private String currentColor = colors[0];
    private long startTime2;
    private String startTime;
    private String userName;
    private long lastClick;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private PlaceExchange exchange;
    private Rectangle[][] rects;
    private static String[] arguments;
    /**
     * Updates the GUI by the Controller
     * @param o: The Observable Model
     * @param arg: An object
     */
    @Override
    public void update(Observable o, Object arg) {

    }

    /**
     * Creates the GUI and presents it to the user
     * @param primaryStage: The stage which all of the other GUI objects are placed on
     * @throws IOException
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        BorderPane pane2 = new BorderPane();
        pane2.setStyle("-fx-background-color: #99ccff");
        Scene login = new Scene(pane2, 280,320);
        Label l = new Label("Username: " + "\n *needs to be unique");
        TextField input = new TextField();
        Button submit = new Button("Submit");
        VBox box = new VBox(l,input,submit);
        pane2.setCenter(box);
        ImageView logo = new ImageView("Place.png");
        logo.setFitHeight(200);
        logo.setFitWidth(280);
        pane2.setTop(logo);
        primaryStage.setScene(login);
        primaryStage.setTitle("Login");
        primaryStage.show();
        Platform.setImplicitExit(false);
        submit.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                userName = input.getText();
                primaryStage.hide();

                new Thread(()->{
                    try{
                        // Create a socket to connect to the server
                        Socket socket = new Socket(arguments[0],parseInt(arguments[1]));

                        // Create an input stream to receive data from the server
                        inputStream = new ObjectInputStream(socket.getInputStream());
                        // Create an output stream to send data to the server
                        outputStream = new ObjectOutputStream(socket.getOutputStream());
                        exchange = new PlaceExchange();
                        userName = input.getText();
                        PlaceRequest login = new PlaceRequest(PlaceRequest.RequestType.LOGIN, userName);
                        exchange.send(login, outputStream);
                        while(true){
                            PlaceRequest object = exchange.receive((inputStream));
                            if(object.getType() == PlaceRequest.RequestType.LOGIN_SUCCESS){
                                break;
                            }else if (object.getType() == PlaceRequest.RequestType.ERROR){
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        primaryStage.show();
                                        input.setText(null);
                                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                        errorAlert.setHeaderText("Input not valid");
                                        errorAlert.setContentText("Please enter a unique username");
                                        errorAlert.showAndWait();
                                        userName = input.getText();
                                        PlaceRequest login = new PlaceRequest(PlaceRequest.RequestType.LOGIN, userName);
                                        try {
                                            exchange.send(login, outputStream);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                continue;
                            }
                        }
                        Platform.runLater(new Runnable() {
                            @Override public void run() {
                                PlaceRequest object2 = exchange.receive((inputStream));
                                gameBoard = (PlaceBoard) object2.getData();
                                new Date();
                                startTime2 = System.currentTimeMillis();
                                lastClick = System.currentTimeMillis();
                                startTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                                BorderPane pane = new BorderPane();
                                TilePane side = new TilePane();
                                // loops to create the buttons which determines the current color. Default is Black.
                                for (int k = 0; k < gameBoard.DIM ; k++) {
                                    Button b1 = new Button(colors2[k].toString());
                                    b1.setPrefSize(40, 40);
                                    ButtonData data = new ButtonData(colors2[k],colors[k]);
                                    b1.setStyle("-fx-background-color: " + colors[k]);
                                    //changes the users current color when the button is pressed
                                    b1.setOnAction(new EventHandler<ActionEvent>() {
                                        @Override
                                        public void handle(ActionEvent event) {
                                            color = (data.getColor());
                                            currentColor = data.getColor2();

                                        }
                                    });
                                    side.getChildren().add(b1);
                                }
                                pane.setTop(side);
                                GridPane board = new GridPane();
                                //Creates the Grid that the rectangles lay on
                                rects = new Rectangle[gameBoard.DIM][gameBoard.DIM];
                                for (int i = 0; i < gameBoard.DIM; i++){
                                    for (int j = 0; j < gameBoard.DIM; j++) {
                                        //creates a placetile for that rectangle that also holds values like current color and the username of who last changed it.
                                        Rectangle r = new Rectangle(40,40);

                                        rects[i][j] = r;
                                        javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.rgb(gameBoard.getTile(i,j).getColor().getRed(), gameBoard.getTile(i,j).getColor().getGreen(), gameBoard.getTile(i,j).getColor().getBlue());
                                        if (!gameBoard.getTile(i,j).getOwner().equals("")){
                                            Tooltip t2 = new Tooltip("( " + i +
                                                    "," + j +
                                                    " )\n" + userName +
                                                    "\n" + color +
                                                    "\n" + new SimpleDateFormat("yyyy/MM/dd\nHH:mm:ss").format(new Date()));
                                            Tooltip.install(rects[i][j], t2);
                                        }
                                        rects[i][j].setFill(fxColor);
                                        int finalJ = j;
                                        int finalI = i;
                                        //changes the current rectangle color when a Mouse Event Occurs
                                        rects[i][j].setOnMouseClicked(new EventHandler<MouseEvent>() {
                                            @Override
                                            public void handle(MouseEvent event) {
                                                if (((System.currentTimeMillis() - lastClick) / 1000) > .5) {
                                                    gameBoard.getTile(finalI, finalJ).setColor(color);
                                                    PlaceTile tile = gameBoard.getTile(finalI,finalJ);
                                                    rects[tile.getRow()][tile.getCol()].setStyle("-fx-background-color: " + colors[tile.getColor().getNumber()]);
                                                    lastClick = System.currentTimeMillis();
                                                    gameBoard.getTile(finalI, finalJ).setOwner(userName);
                                                    gameBoard.getTile(finalI, finalJ).setTime((System.currentTimeMillis() - startTime2) / 1000);
                                                    PlaceRequest tileChange = new PlaceRequest(PlaceRequest.RequestType.CHANGE_TILE, gameBoard.getTile(finalI, finalJ));
                                                    sendMove(tileChange, exchange);
                                                    Tooltip t = new Tooltip("( " + finalI +
                                                            "," + finalJ +
                                                            " )\n" + userName +
                                                            "\n" + color +
                                                            "\n" + new SimpleDateFormat("yyyy/MM/dd\nHH:mm:ss").format(new Date()));
                                                    Tooltip.install(rects[finalI][finalJ], t);
                                                }
                                            }
                                        });
                                        board.setRowIndex(rects[i][j], i);
                                        board.setColumnIndex(rects[i][j], j);
                                        board.getChildren().addAll(rects[i][j]);
                                        //hello
                                    }
                                }
                                pane.setCenter( board);
                                Scene scene = new Scene(pane, 640, 680);

                                primaryStage.setScene(scene);
                                primaryStage.setTitle("Place: " + userName);
                                primaryStage.show();

                                try{
                                    Thread.sleep(100);
                                }catch(InterruptedException ex){
                                    ex.printStackTrace();
                                }
                                new Thread(() -> {
                                    while (true) {
                                        try {
                                            PlaceRequest object = exchange.receive(inputStream);
                                            PlaceRequest.RequestType type2 = object.getType();
                                            switch (type2) {

                                                case TILE_CHANGED:
                                                    PlaceTile tile = (PlaceTile) object.getData();
                                                    Platform.runLater(new Runnable(){
                                                        @Override
                                                        public void run() {
                                                            javafx.scene.paint.Color fxColor = javafx.scene.paint.Color.rgb(tile.getColor().getRed(), tile.getColor().getGreen(), tile.getColor().getBlue());
                                                            rects[tile.getRow()][tile.getCol()].setFill(fxColor);
                                                            Tooltip t2 = new Tooltip("( " + tile.getRow() +
                                                                    "," + tile.getCol() +
                                                                    " )\n" + userName +
                                                                    "\n" + color +
                                                                    "\n" + new SimpleDateFormat("yyyy/MM/dd\nHH:mm:ss").format(new Date()));
                                                            Tooltip.install(rects[tile.getRow()][tile.getCol()], t2);
                                                        }
                                                    });
                                                    gameBoard.setTile(tile);
                                                    break;

                                            }
                                            outputStream.flush();
                                        } catch (IOException ioe) {
                                            ioe.printStackTrace();
                                        }
                                    }
                                }).start();
                            }
                        });

                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }).start();
        }
    });
}


    /**
     * Sends the PlaceRequest move to the server
     * @param request: The request being made by the GUI
     * @param exchange: The PlaceExchange that the request will be sent with
     * this message is here so i could push again
     */
    public void sendMove(PlaceRequest request, PlaceExchange exchange){
        try {
            exchange.send(request,outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Separate class for ButtonData that holds the PlaceColor and String Color of every Button.
    public class ButtonData{
        private PlaceColor color;
        private String color2;

        ButtonData(PlaceColor color, String color2){
            this.color = color;
            this.color2 = color2;
        }

        /**
         * Accessor for Color
         * @return color: The color as a PlaceColor
         */
        public PlaceColor getColor(){
            return this.color;
        }

        /**
         * Accessor for color2
         * @return color2: The color as a String
         */
        public String getColor2(){
            return this.color2;
        }


    }
    public static void main(String[] args) {
        arguments = args;
        launch(args);
    }
}

