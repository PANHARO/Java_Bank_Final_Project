import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer {
    private static final int DEFAULT_PORT = 8080;
    private static final Path WEB_ROOT = Path.of("web");

    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        BankDataStore dataStore = new BankDataStore("data/Users.txt", "data/Accounts.txt", "data/Transaction.txt");
        dataStore.initializeFiles();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/register", new RegisterHandler(dataStore));
        server.createContext("/api/login", new LoginHandler(dataStore));
        server.createContext("/api/account", new AccountHandler(dataStore));
        server.createContext("/api/deposit", new DepositHandler(dataStore));
        server.createContext("/api/withdraw", new WithdrawHandler(dataStore));
        server.createContext("/api/transfer", new TransferHandler(dataStore));
        server.createContext("/api/history", new HistoryHandler(dataStore));
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("J Bank web app running at http://localhost:" + port);
        System.out.println("Started at " + LocalDateTime.now());
    }

    private static int resolvePort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }

        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    private abstract static class ApiHandler implements HttpHandler {
        protected final BankDataStore dataStore;

        protected ApiHandler(BankDataStore dataStore) {
            this.dataStore = dataStore;
        }

        protected Map<String, String> readForm(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            return parseEncoded(body);
        }

        protected Map<String, String> readQuery(HttpExchange exchange) throws IOException {
            String rawQuery = exchange.getRequestURI().getRawQuery();
            return parseEncoded(rawQuery == null ? "" : rawQuery);
        }

        protected double parseAmount(String value) throws IOException {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new IOException("Enter a valid amount.");
            }
        }

        protected void sendJson(HttpExchange exchange, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/json; charset=utf-8");
            headers.set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }

        protected void sendError(HttpExchange exchange, int status, String message) throws IOException {
            sendJson(exchange, status, "{\"ok\":false,\"message\":\"" + escapeJson(message) + "\"}");
        }

        protected void requirePost(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                throw new IOException("Use POST for this action.");
            }
        }
    }

    private static class RegisterHandler extends ApiHandler {
        RegisterHandler(BankDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requirePost(exchange);
                Map<String, String> form = readForm(exchange);
                String username = required(form, "username", "Enter your name.");
                String password = required(form, "password", "Enter a password.");

                dataStore.createAccount(new Account(username, password, 0.0));
                Account account = dataStore.authenticate(username, password);
                sendJson(exchange, 200, accountJson("Welcome, " + account.getUsername() + ".", account, dataStore.getTransactionHistory(account.getUsername())));
            } catch (IOException e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private static class LoginHandler extends ApiHandler {
        LoginHandler(BankDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requirePost(exchange);
                Map<String, String> form = readForm(exchange);
                String username = required(form, "username", "Enter your name.");
                String password = required(form, "password", "Enter a password.");
                Account account = dataStore.authenticate(username, password);

                if (account == null) {
                    throw new IOException("Invalid name or password.");
                }

                sendJson(exchange, 200, accountJson("Welcome back, " + account.getUsername() + ".", account, dataStore.getTransactionHistory(account.getUsername())));
            } catch (IOException e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private static class AccountHandler extends ApiHandler {
        AccountHandler(BankDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> query = readQuery(exchange);
                String username = required(query, "username", "Choose an account first.");
                Account account = dataStore.getAccount(username);

                if (account == null) {
                    throw new IOException("Account not found.");
                }

                sendJson(exchange, 200, accountJson("Account refreshed.", account, dataStore.getTransactionHistory(account.getUsername())));
            } catch (IOException e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private static class DepositHandler extends ApiHandler {
        DepositHandler(BankDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requirePost(exchange);
                Map<String, String> form = readForm(exchange);
                String username = required(form, "username", "Choose an account first.");
                double amount = parseAmount(required(form, "amount", "Enter an amount."));
                Account account = dataStore.deposit(username, amount);
                sendJson(exchange, 200, accountJson("Deposit successful.", account, dataStore.getTransactionHistory(account.getUsername())));
            } catch (IOException e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private static class WithdrawHandler extends ApiHandler {
        WithdrawHandler(BankDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requirePost(exchange);
                Map<String, String> form = readForm(exchange);
                String username = required(form, "username", "Choose an account first.");
                double amount = parseAmount(required(form, "amount", "Enter an amount."));
                Account account = dataStore.withdraw(username, amount);
                sendJson(exchange, 200, accountJson("Withdrawal successful.", account, dataStore.getTransactionHistory(account.getUsername())));
            } catch (IOException e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private static class TransferHandler extends ApiHandler {
        TransferHandler(BankDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                requirePost(exchange);
                Map<String, String> form = readForm(exchange);
                String username = required(form, "username", "Choose an account first.");
                String recipient = required(form, "recipient", "Enter a recipient name.");
                double amount = parseAmount(required(form, "amount", "Enter an amount."));
                Account account = dataStore.transfer(username, recipient, amount);
                sendJson(exchange, 200, accountJson("Transfer successful.", account, dataStore.getTransactionHistory(account.getUsername())));
            } catch (IOException e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private static class HistoryHandler extends ApiHandler {
        HistoryHandler(BankDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> query = readQuery(exchange);
                String username = required(query, "username", "Choose an account first.");
                Account account = dataStore.getAccount(username);

                if (account == null) {
                    throw new IOException("Account not found.");
                }

                sendJson(exchange, 200, accountJson("History loaded.", account, dataStore.getTransactionHistory(account.getUsername())));
            } catch (IOException e) {
                sendError(exchange, 400, e.getMessage());
            }
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            Path filePath = resolveStaticPath(requestPath);

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                filePath = WEB_ROOT.resolve("index.html");
            }

            byte[] bytes = Files.readAllBytes(filePath);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType(filePath));
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }

        private Path resolveStaticPath(String requestPath) {
            if (requestPath == null || "/".equals(requestPath)) {
                return WEB_ROOT.resolve("index.html");
            }

            String trimmed = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            return WEB_ROOT.resolve(trimmed).normalize();
        }

        private String contentType(Path filePath) {
            String name = filePath.getFileName().toString().toLowerCase();
            if (name.endsWith(".css")) {
                return "text/css; charset=utf-8";
            }
            if (name.endsWith(".js")) {
                return "application/javascript; charset=utf-8";
            }
            if (name.endsWith(".png")) {
                return "image/png";
            }
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                return "image/jpeg";
            }
            return "text/html; charset=utf-8";
        }
    }

    private static String required(Map<String, String> values, String key, String message) throws IOException {
        String value = values.getOrDefault(key, "").trim();
        if (value.isEmpty()) {
            throw new IOException(message);
        }
        return value;
    }

    private static String accountJson(String message, Account account, List<String> history) {
        return "{"
            + "\"ok\":true,"
            + "\"message\":\"" + escapeJson(message) + "\","
            + "\"username\":\"" + escapeJson(account.getUsername()) + "\","
            + "\"balance\":" + String.format("%.2f", account.getBalance()) + ","
            + "\"history\":" + historyJson(history)
            + "}";
    }

    private static String historyJson(List<String> history) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("\"").append(escapeJson(history.get(i))).append("\"");
        }
        builder.append("]");
        return builder.toString();
    }

    private static Map<String, String> parseEncoded(String raw) throws IOException {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return values;
        }

        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }

            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }

        return values;
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}
