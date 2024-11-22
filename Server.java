import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5000;
    private static final List<String> connectedUsers = Collections.synchronizedList(new ArrayList<>());
    private static final List<ClientHandler> connectedClients = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> messageHistory = Collections.synchronizedList(new ArrayList<>());
    private static final List<Group> groups = Arrays.asList(
            new Group(1, "Group A"),
            new Group(2, "Group B"),
            new Group(3, "Group C"),
            new Group(4, "Group D"),
            new Group(5, "Group E")
    );
    private static int messageId = 0;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("A client just connected.");
                ClientHandler clientHandler = new ClientHandler(socket);
                connectedClients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private final List<Group> userGroups = new ArrayList<>();

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("Enter username: ");
                String clientInput;

                while ((clientInput = in.readLine()) != null) {
                    if (username == null) {
                        username = clientInput.trim();
                        connectedUsers.add(username);
                        System.out.println("Welcome " + username);
                        broadcast("Welcome " + username);
                        broadcast("Connected Users: " + connectedUsers);

                        groups.forEach(group -> out.println("ID: " + group.id + ", Name: " + group.name));
                        out.println("Enter group IDs or names to join:");

                        if (!messageHistory.isEmpty()) {
                            out.println("Last 2 messages:");
                            messageHistory.stream()
                                    .skip(Math.max(0, messageHistory.size() - 2))
                                    .forEach(out::println);
                        }
                    } else if ("exit".equalsIgnoreCase(clientInput.trim())) {
                        disconnect();
                        break;
                    } else if (userGroups.isEmpty()) {
                        joinGroups(clientInput);
                    } else {
                        sendMessageToGroups(clientInput);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void joinGroups(String clientInput) {
            String[] inputs = clientInput.split(" ");
            for (String input : inputs) {
                groups.stream()
                        .filter(group -> String.valueOf(group.id).equals(input) || group.name.equalsIgnoreCase(input))
                        .forEach(group -> {
                            if (!userGroups.contains(group)) {
                                userGroups.add(group);
                                group.addUser(this);
                            }
                        });
            }
            out.println("You have joined: " + userGroups.stream().map(g -> g.name).toList());
        }

        private void sendMessageToGroups(String message) {
            String timestamp = new Date().toString();
            String formattedMessage = messageId++ + " " + username + " " + timestamp + ": " + message;
            System.out.println(formattedMessage);
            messageHistory.add(formattedMessage);

            userGroups.forEach(group -> group.broadcast(formattedMessage, this));
        }

        private void disconnect() {
            connectedUsers.remove(username);
            connectedClients.remove(this);
            System.out.println(username + " has disconnected.");
            broadcast(username + " has disconnected.");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void broadcast(String message) {
        synchronized (connectedClients) {
            connectedClients.forEach(client -> client.out.println(message));
        }
    }

    private static class Group {
        private final int id;
        private final String name;
        private final List<ClientHandler> users = new ArrayList<>();

        public Group(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public void addUser(ClientHandler user) {
            users.add(user);
        }

        public void broadcast(String message, ClientHandler sender) {
            users.stream().filter(user -> user != sender).forEach(user -> user.out.println(message));
        }
    }
}
