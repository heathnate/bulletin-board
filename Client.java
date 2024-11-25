import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private boolean joined = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the bulletin board client!\n"
            + "Available commands:\n"
            + "%connect <server_address> <port_number> - Connect to a server\n"
            + "%join - Join a single message board\n"
            + "%post / <message_subject> / <message_body> - Post a message to a message board\n"
            + "%users - View a list of all users in a group\n"
            + "%leave - Leave the current group\n"
            + "%message <message_ID> - View the content of a message\n"
            + "%exit - Disconnect from the server\n"
            + "%help - View list of all commands");

            while(true) {
                System.out.print("Enter a command: ");
                String command = scanner.nextLine().trim();

                if (command.startsWith("%connect")){
                    handleConnect(command);
                } else if (command.equals("%join")){
                    handleJoin();
                } else if (command.startsWith("%post")){
                    handlePost(command);
                } else if (command.equals("%users")){
                    handleUsers();
                } else if (command.equals("%leave")){
                    handleLeave();
                } else if (command.startsWith("%message")){
                    handleMessage(command);
                } else if (command.equals("%exit")){
                    handleExit();
                    break;
                } else if (command.equals("%help")){
                    handleHelp();
                } else {
                    System.out.println("Invalid command: '" + command + "'. Please enter one of the following commands:");
                    handleHelp();
                }
            }
    }

    private void handleConnect(String cmd) {
        if (connected) {
            System.out.println("Already connected to a server.");
            return;
        }

        String[] cmdParts = cmd.split(" ");
        if (cmdParts.length != 3) {
            System.out.println("Invalid %connect command. Format: '%connect <server_address> <port_number>'");
            return;
        }

        String serverAddress = cmdParts[1];
        int port;

        try {
            port = Integer.parseInt(cmdParts[2]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number provided.");
            return;
        }

        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            System.out.println("Connected to server at " + serverAddress + ":" + port);
            startServerListener();
        } catch (IOException e) {
            System.out.println("Couldn't connect to server: " + e.getMessage());
        }
    }

    private void handleJoin() {
        if (!connected) {
            System.out.println("You must be connected to a server before you can join a bulletin board. Use %connect to do so.");
            return;
        }
        // Send join command to the server
        out.println("%join");
        joined = true;
    }

    private void handlePost(String cmd) {
        if (!connected) {
            System.out.println("You must be connected to a server before you can post a message. Use %connect to do so.");
            return;
        }
        if (!joined) {
            System.out.println("You must join a bulletin board before you can post a message. Use %join to do so.");
            return;
        }

        String[] cmdParts = cmd.split(" / ");
        if (cmdParts.length != 3) {
            System.out.println("Invalid %post command. Format: '%post / <message_subject> / <message_body>'");
            return;
        }

        String subject = cmdParts[1];
        String body = cmdParts[2];
        out.println("%post / " + subject + " / " + body);
    }

    private void handleUsers() {
        if (!connected) {
            System.out.println("You must be connected to a server before you can view users. Use %connect to do so.");
            return;
        }
        if (!joined) {
            System.out.println("You must join a bulletin board before you can view its users. Use %join to do so.");
            return;
        }
        out.println("%users");
    }

    private void handleLeave() {
        if (!connected) {
            System.out.println("You must be connected to a server before you can leave a bulletin board. Use %connect to do so.");
            return;
        }
        if (!joined) {
            System.out.println("You must join a bulletin board before you can leave it. Use %join to do so.");
            return;
        }
        out.println("%leave");
        joined = false;
    }

    private void handleMessage(String cmd) {
        if (!connected) {
            System.out.println("You must be connected to a server before you can view a message. Use %connect to do so.");
            return;
        }
        if (!joined) {
            System.out.println("You must join a bulletin board before you can view its messages. Use %join to do so.");
            return;
        }

        String[] cmdParts = cmd.split(" ");
        if (cmdParts.length != 2) {
            System.out.println("Invalid %message command. Format: '%message <message_ID>'");
            return;
        }
        
        int messageId;
        try {
            messageId = Integer.parseInt(cmdParts[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid message ID provided.");
            return;
        }
        out.println("%message " + messageId);
    }

    private void handleExit() {
        if (connected) {
            try {
                out.println("%exit");
                socket.close();
                connected = false;
                joined = false;
                System.out.println("Disconnected from the server.");
            } catch (IOException e) {
                System.out.println("Error while disconnecting: " + e.getMessage());
                return;
            }
        }
        System.out.println("Exiting the client. Goodbye!");
    }

    private void handleHelp() {
        System.out.println("Available commands:\n"
            + "%connect <server_address> <port_number> - Connect to a server\n"
            + "%join - Join a single message board\n"
            + "%post / <message_subject> / <message_body> - Post a message to a message board\n"
            + "%users - View a list of all users in a group\n"
            + "%leave - Leave the current group\n"
            + "%message <message_ID> - View the content of a message\n"
            + "%exit - Disconnect from the server\n"
            + "%help - View list of all commands");
    }

    private void startServerListener() {
        new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server. " + e.getMessage());
            }
        }).start();
    }
}
    
    // // Provide a list of all possible commands
    // private static void printHelpMessage() {
    //     String helpMessage = "Commands:\n" + "%connect - Connect to a server\n" + "%exit - Disconnect from the server\n"
    //         + "%join - Join a single message board\n" + "%post - Post a message to a message board\n"
    //         + "%users - View a list of all users in a group\n" + "%leave - Leave the current group\n"
    //         + "%message - View the content of a certain message\n" + "%groups - View a list of all groups\n"
    //         + "%groupjoin - Join a specific group\n" + "%grouppost - Post a message to a specific group\n"
    //         + "%groupusers - View a list of all users within a specific group\n" + "%groupleave - Leave a specific group\n"
    //         + "%groupmessage - View the content of a message within a specific group\n" + "%help - Repeat this message";
    //     System.out.println(helpMessage);
    // }

    // // Connect to a server
    // private static Socket connect(String address, int port) {
    //     try {
    //         Socket socket = new Socket(address, port);
    //         System.out.println("Connected to server: " + address + ":" + port);

    //         return socket;
    //     } catch (IOException e) {
    //         System.err.println("Error connecting to server: " + e.getMessage());
    //         return null;
    //     }
    // }

    // // Disconnect from a server
    // private static void disconnect(Scanner scanner, Socket socket) {
    //     scanner.close();
    //     try {
    //         socket.close();
    //     } catch (IOException e) {
    //         System.err.println("Error closing socket: " + e.getMessage());
    //     }
    // }

    // // private static void join()

    // public static void main(String[] args) {
    //         // PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    //         // BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    //     Socket socket = null;
    //     try (Scanner scanner = new Scanner(System.in)) {
    //         String command;

    //         while (true) {
    //             System.out.println("Enter %help for a list of all commands.");

    //             command = scanner.nextLine().trim();

    //             switch(command.toLowerCase()) {
    //                 case "%help":
    //                     printHelpMessage();
    //                     break;
    //                 case "%exit":
    //                     System.out.println("Exiting... Goodbye!");
    //                     disconnect(scanner, socket);
    //                     return;
    //                 case "%connect":
    //                     // Get address (hostname) and port #
    //                     System.out.println("Enter the address of the server to connect to: ");
    //                     String address = scanner.nextLine().trim();
    //                     System.out.println("Enter the port number of the server to connect to: ");
    //                     int port = Integer.parseInt(scanner.nextLine().trim());
                        
    //                     socket = connect(address, port);

    //                     break;
                    
    //                 // Part 1
    //                 case "%join":
    //                     // Join the single message board
    //                     break;

    //                 case "%post":
    //                     // Post a message to the board
    //                     // Followed by the message subject and the message content or main body
    //                     break;
    //                 case "%users":
    //                     // Retrieve a list of users in the same group
    //                     break;
    //                 case "%leave":
    //                     // Leave the group
    //                     break;
    //                 case "%message":
    //                     // Retrieve the content of the message
    //                     // Followed by message ID
    //                     break;
                    
                    // Part 2
                    // case "%groups":
                    //     // Retrieve a list of all groups that can be joined
                    //     break;
                    // case "%groupjoin":
                    //     // Join a specific group
                    //     // Followed by the group id/name
                    //     break;
                    // case "%grouppost":
                    //     // Post to a specific group
                    //     // Followed by the group id/name, message subject, and message content or main body to post a message to a messaage board owned by a specific group
                    //     break;
                    // case "%groupusers":
                    //     // Retrieve a list of users in teh given group
                    //     // Followed by the group id/name
                    //     break;
                    // case "%groupleave":
                    //     // Leave a specific group
                    //     // Followed by group id/name
                    //     break;
                    // case "%groupmessage":
                    //     // Retrieve the content of the message posted earlier on a message board owned by a specific group
                    //     // Followed by the gruop id/name and message ID
                    //     break;
        //             default:
        //                 System.out.println("Unknown command. Available commands: %exit, %connect, %join, %post, %users, %leave, %message, %groups, %groupjoin, %grouppost, %groupusers, %groupleave, %groupmessage.");
        //         }
        //     }
        // } catch(Exception e) {
        //     System.err.println("Error: " + e.getMessage());
        // } finally {
        //     // Ensure socket is closed when program exits
        //     if (socket != null && !socket.isClosed()) {
        //         try {
        //             socket.close();
        //         } catch (IOException e) {
        //             System.err.println("Error closing socket: " + e.getMessage());
        //         }
        //     }
        // }
        
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
//     }
// }
