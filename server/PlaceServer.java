package place.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceExchange;
import place.network.PlaceRequest;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Observable;

/**
 * PlaceServer that permits connections from other clients to play the game "Place"
 * @author Bryce Thompson
 * @author Carson Bloomingdale
 */

public class PlaceServer  extends Application
{
    private int port;
    private ServerSocket serverSocket;
    private ArrayList<ObjectOutputStream> clientout = new ArrayList<ObjectOutputStream>();
    private ArrayList<ObjectInputStream> clientin = new ArrayList<ObjectInputStream>();
    private ArrayList<String> userNames = new ArrayList<>();
    private Socket player;
    TextArea taLog;
    PlaceBoard board;

    /**
     * Start method that sets up the GUI for the server (something to make the server look more visually appealing)
     * @param primaryStage: the stage of the Server GUI
     */
    public void start(Stage primaryStage) {
        taLog = new TextArea();
        board = new PlaceBoard(16);
        // Create a scene and place it in the stage
        Scene place = new Scene(new ScrollPane(taLog), 450, 200);
        primaryStage.setTitle("Place Server"); // Set the stage title
        primaryStage.setScene(place); // Place the scene in the stage
        primaryStage.show(); // Display the stage

        new Thread(() -> {
            connecttoServer();
        }).start();
    }

    /**
     * Accessor for userNames
     * @return userNames: The usernames that are in use
     */
    public ArrayList<String> getUsernames()
    { return this.userNames; }

    /**
     * Mutator for the Server GUI to print out a message.
     * @param txt: The message to add to the label
     */
    public void setLabelText(String txt){
        taLog.appendText(txt);
    }

    /**
     * Creates a ServerSocket and then prints out the socket information
     */
    private void connecttoServer(){
        try{
            ServerSocket serverSocket = new ServerSocket(8000);
            setLabelText(new Date() + ": Server started at socket 8000\n");
            waitForClients(serverSocket);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * Waits for clients to connect to the ServerSocket, and creates a new thread with a ClientHandler
     * @param mySocket: The client socket that connects to the server
     */
    private void waitForClients(ServerSocket mySocket ){
        while(true) {
            try {
                setLabelText("Ready to receive \n");
                Socket client = mySocket.accept();
                setLabelText(client.toString() + " connected to Server\n");
                Thread t = new Thread(new ClientHandler(client));
                t.start();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }

    /**
     * A method that shares a PlaceRequest with all of the clients that are connected to the server
     * @param request: The PlaceRequest to share
     * @param exchange: The PlaceExchange which will permit the distribution of the request
     */
    public void shareToAll(PlaceRequest request, PlaceExchange exchange){
        for (ObjectOutputStream stream: clientout){
            try{
                exchange.send(request,stream);
                stream.flush();
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Method that receives requests from all of the clients
     * @param exchange: The PlaceExchange that receives the PlaceRequests
     * @return object: The PlaceRequest that was received.
     */
    public PlaceRequest getFromAll(PlaceExchange exchange){
        PlaceRequest object = null;
        for (ObjectInputStream stream: clientin){
            object = exchange.receive(stream);
        }
        return object;
    }

    /**
     * Class that handles the ObjectOutput and ObjectInput Streams
     * @author Carson Bloomingdale
     * @author Bryce Thompson
     */
    private class ClientHandler implements Runnable{
        Socket clientSocket;
        ObjectOutputStream out;
        ObjectInputStream in;


        /**
         * Constructor for ClientHandler, sets the clientsocket and streams
         * @param clientSocket: The Socket that is connected to the server
         */
        public ClientHandler(Socket clientSocket){
            this.clientSocket = clientSocket;
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Run method that listens for the requests from the client and responds with other requests
         */
        @Override
        public void run(){
            while(true){


                clientout.add(out);
                System.out.println(clientout.toString());
                PlaceExchange exchange = new PlaceExchange();

//                PlaceRequest login = new PlaceRequest(PlaceRequest.RequestType.LOGIN, null);
//                try {
//                    exchange.send(login,out);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                PlaceRequest object;
                object = exchange.receive(in);

                if(object.getType().equals(PlaceRequest.RequestType.LOGIN)){
                    while(true) {
                        String tempUser = object.getData().toString();
                        if (userNames.contains(tempUser)) {
                            String strError = "Username already exists. Please enter another: ";
                            //Bad Login
                            PlaceRequest error = new PlaceRequest(PlaceRequest.RequestType.ERROR, strError);
                            try {
                                exchange.send(error, out);

                                object = exchange.receive(in);
                                System.out.println(object.getData().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            userNames.add(tempUser);
                            PlaceRequest success = new PlaceRequest(PlaceRequest.RequestType.LOGIN_SUCCESS, "Login Successful.");
                            try {
                                exchange.send(success, out);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            setLabelText("Login Success \n");
                            break;
                        }

                    }

                }
                PlaceRequest sendBoard = new PlaceRequest(PlaceRequest.RequestType.BOARD, board);
                try {
                    exchange.send(sendBoard, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while(true){
                    object = exchange.receive(in);
                    if(object.getType().equals(PlaceRequest.RequestType.CHANGE_TILE)) {
                        setLabelText("Tile Changed \n");
                        board.setTile((PlaceTile) object.getData());
                        PlaceRequest send = new PlaceRequest(PlaceRequest.RequestType.TILE_CHANGED, object.getData());
                        shareToAll(send, exchange);
                    }
                    else if(object.getType().equals((PlaceRequest.RequestType.ERROR))){
                        setLabelText("Client Error \n");
                    }
                }
            }
        }
    }

}
