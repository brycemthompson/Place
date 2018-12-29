package place.client.ptui;

import javafx.scene.control.Alert;
import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.network.PlaceExchange;
import place.network.PlaceRequest;

import java.io.*;
import java.net.Socket;
import java.util.*;

import static java.lang.Integer.parseInt;

/**
 * PlacePTUI Class, a Plain Text Graphical User Interface to connect to the PlaceServer.
 * @author Carson Bloomingdale
 * @author Bryce Thompson
 */
public class PlacePTUI  {
    private static PlaceColor[] colors2 = new PlaceColor[]{PlaceColor.BLACK,PlaceColor.GRAY,PlaceColor.SILVER,PlaceColor.MAROON,PlaceColor.RED,PlaceColor.OLIVE,
            PlaceColor.YELLOW,PlaceColor.GREEN,PlaceColor.LIME,PlaceColor.TEAL,PlaceColor.AQUA,PlaceColor.NAVY,PlaceColor.BLUE,PlaceColor.PURPLE,PlaceColor.FUCHSIA};
    public static PlaceBoard board;

    /**
     * Main method that runs the program threaded
     * @param args
     */
    public static void main(String args[]){
        final String[] finalUser = new String[1];
        new Thread (()-> {
            try {
                // Create a socket to connect to the server
                long lastClick = System.currentTimeMillis();
                Socket socket = new Socket(args[0], parseInt(args[1]));

                // Create an input stream to recieve data from the server
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                // Create an output stream to send data to the server
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                PlaceExchange exchange = new PlaceExchange();
                PlaceRequest object;
                String userName;
                int dimensions;
                int row = 0;
                int column = 0;
                String color;
                boolean flag = false;
                Scanner scan = new Scanner(System.in);
                PlaceRequest login;

                System.out.println("Please Print Username");
                userName = scan.next();
                login = new PlaceRequest(PlaceRequest.RequestType.LOGIN, userName);
                exchange.send(login, outputStream);
                object = exchange.receive(inputStream);

                if (object.getType() == PlaceRequest.RequestType.ERROR) {
                    while(object.getType() == PlaceRequest.RequestType.ERROR)
                    {
                        System.out.println(object.getData().toString());
                        userName = scan.next();
                        login = new PlaceRequest(PlaceRequest.RequestType.LOGIN, userName);
                        exchange.send(login, outputStream);
                        object = exchange.receive(inputStream);
                    }
                }

                if (object.getType() == PlaceRequest.RequestType.LOGIN_SUCCESS)
                {
                    System.out.println(object.getData().toString());
                    object = exchange.receive(inputStream);
                    if(object.getType() == PlaceRequest.RequestType.BOARD)
                    {
                        board = (PlaceBoard) object.getData();

                        new Thread(() -> {
                            while (true) {
                                try {
                                    PlaceRequest object2 = exchange.receive(inputStream);
                                    PlaceRequest.RequestType type2 = object2.getType();
                                    switch (type2) {

                                        case TILE_CHANGED:
                                            PlaceTile tile = (PlaceTile) object2.getData();
                                            board.setTile(tile);
                                            System.out.println(board.toString());
                                            break;

                                    }
                                    outputStream.flush();
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }




                while (((System.currentTimeMillis() - lastClick) / 1000) > .5) {
                    System.out.println(board.toString());
                    do {
                        System.out.println("Enter your row: ");
                        row = scan.nextInt();
                        if (row > board.DIM) {
                            System.out.println("Invalid Input");
                            flag = true;
                        }
                        if (row < 0) {

                            System.out.println("Invalid Input");
                            flag = true;

                        }


                    } while (flag);

                    flag = false;
                    do {
                        System.out.println("Enter your column: ");
                        column = scan.nextInt();
                        if (column > board.DIM) {
                            System.out.println("Invalid Input");
                            flag = true;
                        }
                        if (column < 0) {

                            System.out.println("Invalid Input");
                            flag = true;
                        } else {
                            break;
                        }
                    } while (flag);

                    flag = false;
                    do {
                        System.out.println("Enter your color: 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15 ");
                        color = scan.next();
                        if (color.equals("1") || color.equals("2") || color.equals("3") || color.equals("4")
                                || color.equals("5") || color.equals("6") || color.equals("7") || color.equals("8") || color.equals("9") || color.equals("10")
                                || color.equals("11") || color.equals("12") || color.equals("13") || color.equals("14") || color.equals("15")) {
                            int index = parseInt(color);
                            finalUser[0] = "userName";
                            PlaceTile tile = new PlaceTile(row, column, finalUser[0], colors2[index - 1]);
                            PlaceRequest change = new PlaceRequest(PlaceRequest.RequestType.CHANGE_TILE, tile);
                            exchange.send(change, outputStream);
                            outputStream.flush();

                            break;
                        } else {
                            System.out.println("Please enter a valid color");
                            flag = true;
                        }
                    } while (flag);
                }


            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();



    }


}
