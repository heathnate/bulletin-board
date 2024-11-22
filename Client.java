import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        final String host = "localhost";
        final int port = 5000;

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server");
            System.out.println("Enter exit to quit");

            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed by server.");
                }
            });

            listenerThread.start();

            String userInput;
            while (true) {
                userInput = scanner.nextLine();
                out.println(userInput);

                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }

            socket.close();
            System.out.println("Connection closed");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
