import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    // Scanner for reading user input from the console
    private static final Scanner scanner = new Scanner(System.in);
    // Socket to establish connection with the server
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        try {
            // Continuous loop to provide menu options
            while (true) {
                System.out.println("Bulletin Board Client");
                System.out.println("1. Connect to server");
                System.out.println("2. Exit");
                System.out.print("Choose an option: ");

                // Get user's choice
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                // Process user's choice
                switch (choice) {
                    case 1: 
                        // Attempt to connect to the server
                        connectToServer();
                        break;
                    case 2:
                        // Exit the applicatoin
                        System.out.println("Goodbye!");
                        return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to establish connection with server with user-provided host and port
    private static void connectToServer() throws IOException {
        // Prompt user for server address
        System.out.print("Enter server address (default localhost): ");
        String address = scanner.nextLine();
        if (address.isEmpty()) address = "localhost"; // Default localhost

        // Prompt user for port number
        System.out.print("Enter port number (default 5000): ");
        String portStr = scanner.nextLine();
        int port = portStr.isEmpty() ? 5000 : Integer.parseInt(portStr); // Default port 5000

        // Create socket connection to the server
        socket = new Socket(address, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Start a thread to receive server messages
        new Thread(Client::receiveMessages).start();

        // Enter interactive mode for sending commands
        interactiveMode();
    }

    // Method to sendd commands to the server
    private static void interactiveMode() throws IOException {
        System.out.println("Connected to server.");

        // Continuous loop to send user commands to the server
        while (true) {
            // Read user input
            String command = scanner.nextLine();
            // Send command to server
            out.println(command);

            // Check for exit condition
            if (command.equalsIgnoreCase("%exit")) {
                break;
            }
        }
    }

    // Method to continuously receive and display messages from the server
    private static void receiveMessages() {
        try {
            // Read messages from the server until connection is closed
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }
}
