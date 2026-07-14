import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BankDataStore {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path usersFile;
    private final Path accountsFile;
    private final Path transactionsFile;

    public BankDataStore(String usersFile, String accountsFile, String transactionsFile) {
        this.usersFile = Paths.get(usersFile);
        this.accountsFile = Paths.get(accountsFile);
        this.transactionsFile = Paths.get(transactionsFile);
    }

    public synchronized void initializeFiles() throws IOException {
        createFileWithHeader(usersFile, "username,password");
        createFileWithHeader(accountsFile, "username,balance");
        createFileWithHeader(transactionsFile, "dateTime,username,type,target,amount,balanceAfter");
    }

    public synchronized boolean userExists(String username) {
        try {
            return resolveUsername(username) != null;
        } catch (IOException e) {
            return false;
        }
    }

    public synchronized void createAccount(Account account) throws IOException {
        if (resolveUsername(account.getUsername()) != null) {
            throw new IOException("That name is already taken.");
        }

        appendLine(usersFile, account.getUsername() + "," + account.getPassword());
        appendLine(accountsFile, account.getUsername() + "," + account.getBalance());
    }

    public synchronized Account authenticate(String username, String password) throws IOException {
        String resolvedUsername = resolveUsername(username);

        if (resolvedUsername == null) {
            return null;
        }

        String storedPassword = findPassword(resolvedUsername);
        if (storedPassword == null || !storedPassword.equals(password)) {
            return null;
        }

        return getAccount(resolvedUsername, storedPassword);
    }

    public synchronized Account getAccount(String username) throws IOException {
        String resolvedUsername = resolveUsername(username);
        if (resolvedUsername == null) {
            return null;
        }

        return getAccount(resolvedUsername, findPassword(resolvedUsername));
    }

    public synchronized String resolveUsername(String username) throws IOException {
        String normalizedInput = normalize(username);
        if (normalizedInput.isEmpty()) {
            return null;
        }

        List<String> lines = readAllLines(usersFile);
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",", -1);
            if (parts.length >= 1 && normalize(parts[0]).equalsIgnoreCase(normalizedInput)) {
                return parts[0];
            }
        }

        return null;
    }

    public synchronized void updateBalance(String username, double newBalance) throws IOException {
        String resolvedUsername = resolveUsername(username);
        if (resolvedUsername == null) {
            throw new IOException("Account not found.");
        }

        List<String> lines = readAllLines(accountsFile);
        List<String> updated = new ArrayList<>();
        updated.add(lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",", -1);
            if (parts.length >= 2 && parts[0].equals(resolvedUsername)) {
                updated.add(resolvedUsername + "," + newBalance);
            } else {
                updated.add(lines.get(i));
            }
        }

        Files.write(accountsFile, updated);
    }

    public synchronized void recordTransaction(String username, String type, String target, double amount, double balanceAfter)
        throws IOException {
        String entry = String.format(
            "%s,%s,%s,%s,%.2f,%.2f",
            LocalDateTime.now().format(TIME_FORMAT),
            username,
            type,
            target,
            amount,
            balanceAfter
        );
        appendLine(transactionsFile, entry);
    }

    public synchronized List<String> getTransactionHistory(String username) throws IOException {
        String resolvedUsername = resolveUsername(username);
        List<String> history = new ArrayList<>();

        if (resolvedUsername == null) {
            return history;
        }

        List<String> lines = readAllLines(transactionsFile);
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",", -1);
            if (parts.length >= 6 && parts[1].equals(resolvedUsername)) {
                String target = parts[3].equals("-") ? "" : " | Target: " + parts[3];
                history.add(parts[0] + " | " + parts[2] + target + " | Amount: " + parts[4] + " | Balance: " + parts[5]);
            }
        }

        return history;
    }

    public synchronized Account deposit(String username, double amount) throws IOException {
        if (amount <= 0) {
            throw new IOException("Amount must be greater than zero.");
        }

        Account account = getRequiredAccount(username);
        account.deposit(amount);
        updateBalance(account.getUsername(), account.getBalance());
        recordTransaction(account.getUsername(), "DEPOSIT", "-", amount, account.getBalance());
        return account;
    }

    public synchronized Account withdraw(String username, double amount) throws IOException {
        if (amount <= 0) {
            throw new IOException("Amount must be greater than zero.");
        }

        Account account = getRequiredAccount(username);
        if (!account.withdraw(amount)) {
            throw new IOException("Insufficient balance.");
        }

        updateBalance(account.getUsername(), account.getBalance());
        recordTransaction(account.getUsername(), "WITHDRAW", "-", amount, account.getBalance());
        return account;
    }

    public synchronized Account transfer(String senderName, String recipientName, double amount) throws IOException {
        if (amount <= 0) {
            throw new IOException("Amount must be greater than zero.");
        }

        Account sender = getRequiredAccount(senderName);
        String targetName = normalize(recipientName);

        if (targetName.isEmpty()) {
            throw new IOException("Enter a recipient name.");
        }

        if (sender.getUsername().equalsIgnoreCase(targetName)) {
            throw new IOException("You cannot transfer money to the same account.");
        }

        if (!sender.withdraw(amount)) {
            throw new IOException("Insufficient balance.");
        }

        updateBalance(sender.getUsername(), sender.getBalance());
        recordTransaction(sender.getUsername(), "TRANSFER_OUT", targetName, amount, sender.getBalance());
        return sender;
    }

    private Account getRequiredAccount(String username) throws IOException {
        Account account = getAccount(username);
        if (account == null) {
            throw new IOException("Account not found.");
        }

        return account;
    }

    private Account getAccount(String username, String password) throws IOException {
        if (password == null) {
            return null;
        }

        List<String> lines = readAllLines(accountsFile);
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",", -1);
            if (parts.length >= 2 && parts[0].equals(username)) {
                double balance = Double.parseDouble(parts[1]);
                return new Account(username, password, balance);
            }
        }

        return null;
    }

    private String findPassword(String username) throws IOException {
        List<String> lines = readAllLines(usersFile);
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",", -1);
            if (parts.length >= 2 && parts[0].equals(username)) {
                return parts[1];
            }
        }

        return null;
    }

    private void createFileWithHeader(Path file, String header) throws IOException {
        if (!Files.exists(file)) {
            Files.write(file, List.of(header));
        } else if (Files.size(file) == 0) {
            Files.write(file, List.of(header));
        }
    }

    private List<String> readAllLines(Path file) throws IOException {
        return Files.readAllLines(file);
    }

    private void appendLine(Path file, String line) throws IOException {
        String prefix = Files.size(file) > 0 ? System.lineSeparator() : "";
        Files.writeString(file, prefix + line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
