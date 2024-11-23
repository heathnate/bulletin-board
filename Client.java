import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
            // PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        Socket socket = null;
        try (Scanner scanner = new Scanner(System.in)) {
            String command;

            while (true) {
                System.out.println("Enter %help for a list of all commands.");

                command = scanner.nextLine().trim();

                switch(command.toLowerCase()) {
                    case "%help":
                        // Provide a list of all possible commands
                        String helpMessage = "Commands:\n" + "%connect - Connect to a server\n" + "%exit - Disconnect from the server\n"
                            + "%join - Join a single message board\n" + "%post - Post a message to a message board\n"
                            + "%users - View a list of all users in a group\n" + "%leave - Leave the current group\n"
                            + "%message - View the content of a certain message\n" + "%groups - View a list of all groups\n"
                            + "%groupjoin - Join a specific group\n" + "%grouppost - Post a message to a specific group\n"
                            + "%groupusers - View a list of all users within a specific group\n" + "%groupleave - Leave a specific group\n"
                            + "%groupmessage - View the content of a message within a specific group\n" + "%help - Repeat this message";
                        System.out.println(helpMessage);
                    case "%exit":
                        // Disconnect from server
                        System.out.println("Exiting... Goodbye!");
                        scanner.close();
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.err.println("Error closing socket: " + e.getMessage());
                        }
                        return;

                    case "%connect":
                        // Connect to a server
                        System.out.println("Enter the address of the server to connect to: ");
                        String address = scanner.nextLine().trim();

                        System.out.println("Enter the port number of the server to connect to: ");
                        int port = Integer.parseInt(scanner.nextLine().trim());
                        
                        try {
                            socket = new Socket(address, port);
                            System.out.println("Connected to server: " + address + ":" + port);
                        } catch (IOException e) {
                            System.err.println("Error connecting to server: " + e.getMessage());
                        }
                        break;
                    
                        // Part 1
                    case "%join":
                        // Join the single message board
                        break;

                    case "%post":
                        // Post a message to the board
                        // Followed by the message subject and the message content or main body
                        break;
                    case "%users":
                        // Retrieve a list of users in the same group
                        break;
                    case "%leave":
                        // Leave the group
                        break;
                    case "%message":
                        // Retrieve the content of the message
                        // Followed by message ID
                        break;
                    // Part 2
                    case "%groups":
                        // Retrieve a list of all groups that can be joined
                        break;
                    case "%groupjoin":
                        // Join a specific group
                        // Followed by the group id/name
                        break;
                    case "%grouppost":
                        // Post to a specific group
                        // Followed by the group id/name, message subject, and message content or main body to post a message to a messaage board owned by a specific group
                        break;
                    case "%groupusers":
                        // Retrieve a list of users in teh given group
                        // Followed by the group id/name
                        break;
                    case "%groupleave":
                        // Leave a specific group
                        // Followed by group id/name
                        break;
                    case "%groupmessage":
                        // Retrieve the content of the message posted earlier on a message board owned by a specific group
                        // Followed by the gruop id/name and message ID
                        break;
                    default:
                        System.out.println("Unknown command. Available commands: %exit, %connect, %join, %post, %users, %leave, %message, %groups, %groupjoin, %grouppost, %groupusers, %groupleave, %groupmessage.");
                }
            }
        } catch(Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            // Ensure socket is closed when program exits
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }
        
        // System.out.println("Enter %help for all command options.");

        // System.out.println("Connected to server");
        // System.out.println("Enter exit to quit");

        // Thread listenerThread = new Thread(() -> {
        //     try {
        //         String serverMessage;
        //         while ((serverMessage = in.readLine()) != null) {
        //             System.out.println(serverMessage);
        //         }
        //     } catch (IOException e) {
        //         System.out.println("Connection closed by server.");
        //     }
        // });

        // listenerThread.start();

        // String userInput;
        // while (true) {
        //     userInput = scanner.nextLine();
        //     out.println(userInput);

        //     if ("exit".equalsIgnoreCase(userInput)) {
        //         break;
        //     }
        // }

        // socket.close();
        // System.out.println("Connection closed");
    }
}
