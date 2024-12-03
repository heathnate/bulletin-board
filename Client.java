import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final Scanner scanner = new Scanner(System.in);
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public static void main(String[] args) {
        try {
            while (true) {
                System.out.println("Bulletin Board Client");
                System.out.println("1. Connect to server");
                System.out.println("2. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        connectToServer();
                        break;
                    case 2:
                        return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void connectToServer() throws IOException {
        System.out.print("Enter server address (default localhost): ");
        String address = scanner.nextLine();
        if (address.isEmpty()) address = "localhost";

        System.out.print("Enter port number (default 5000): ");
        String portStr = scanner.nextLine();
        int port = portStr.isEmpty() ? 5000 : Integer.parseInt(portStr);

        socket = new Socket(address, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Start a thread to receive server messages
        new Thread(Client::receiveMessages).start();

        // Interactive command loop
        interactiveMode();
    }

    private static void interactiveMode() throws IOException {
        System.out.println("Connected to server.");
        while (true) {
            String command = scanner.nextLine();
            out.println(command);

            if (command.equalsIgnoreCase("%exit")) {
                break;
            }
        }
    }

    private static void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }
}
