import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        BankDataStore dataStore = new BankDataStore("data/Users.txt", "data/Accounts.txt", "data/Transaction.txt");

        try {
            dataStore.initializeFiles();
        } catch (IOException e) {
            System.out.println("Failed to initialize data files: " + e.getMessage());
            return;
        }

        boolean running = true;

        while (running) {
            System.out.println("\n=== Bank Management System ===");
            System.out.println("1. Create Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            int choice = readInt();

            switch (choice) {
                case 1:
                    createAccount(dataStore);
                    break;
                case 2:
                    loginUser(dataStore);
                    break;
                case 3:
                    running = false;
                    System.out.println("Thank you for using the Bank Management System.");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void createAccount(BankDataStore dataStore) {
        System.out.print("Enter your name: ");
        String username = SCANNER.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }

        if (dataStore.userExists(username)) {
            System.out.println("That name is already in use.");
            return;
        }

        System.out.print("Enter a password: ");
        String password = SCANNER.nextLine().trim();

        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        Account account = new Account(username, password, 0.0);

        try {
            dataStore.createAccount(account);
            System.out.println("Account created successfully.");
        } catch (IOException e) {
            System.out.println("Unable to create account: " + e.getMessage());
        }
    }

    private static void loginUser(BankDataStore dataStore) {
        System.out.print("Name: ");
        String username = SCANNER.nextLine().trim();

        System.out.print("Password: ");
        String password = SCANNER.nextLine().trim();

        try {
            Account account = dataStore.authenticate(username, password);

            if (account == null) {
                System.out.println("Invalid name or password.");
                return;
            }

            System.out.println("Login successful. Welcome, " + account.getUsername() + "!");
            loggedInMenu(dataStore, account);
        } catch (IOException e) {
            System.out.println("Unable to login: " + e.getMessage());
        }
    }

    private static void loggedInMenu(BankDataStore dataStore, Account account) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n=== Account Menu ===");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Transfer");
            System.out.println("4. View Balance");
            System.out.println("5. View Transaction History");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");

            int choice = readInt();

            switch (choice) {
                case 1:
                    deposit(dataStore, account);
                    break;
                case 2:
                    withdraw(dataStore, account);
                    break;
                case 3:
                    transfer(dataStore, account);
                    break;
                case 4:
                    System.out.printf("Current balance: %.2f%n", account.getBalance());
                    break;
                case 5:
                    viewTransactionHistory(dataStore, account);
                    break;
                case 6:
                    loggedIn = false;
                    System.out.println("Logged out successfully.");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void deposit(BankDataStore dataStore, Account account) {
        System.out.print("Enter amount to deposit: ");
        double amount = readDouble();

        try {
            Account updatedAccount = dataStore.deposit(account.getUsername(), amount);
            syncAccount(account, updatedAccount);
            System.out.printf("Deposit successful. New balance: %.2f%n", account.getBalance());
        } catch (IOException e) {
            System.out.println("Unable to save deposit: " + e.getMessage());
        }
    }

    private static void withdraw(BankDataStore dataStore, Account account) {
        System.out.print("Enter amount to withdraw: ");
        double amount = readDouble();

        try {
            Account updatedAccount = dataStore.withdraw(account.getUsername(), amount);
            syncAccount(account, updatedAccount);
            System.out.printf("Withdrawal successful. New balance: %.2f%n", account.getBalance());
        } catch (IOException e) {
            System.out.println("Unable to save withdrawal: " + e.getMessage());
        }
    }

    private static void transfer(BankDataStore dataStore, Account sender) {
        System.out.print("Enter recipient name: ");
        String recipientName = SCANNER.nextLine().trim();

        System.out.print("Enter amount to transfer: ");
        double amount = readDouble();

        try {
            Account updatedSender = dataStore.transfer(sender.getUsername(), recipientName, amount);
            syncAccount(sender, updatedSender);
            System.out.printf("Transfer successful. New balance: %.2f%n", sender.getBalance());
        } catch (IOException e) {
            System.out.println("Unable to save transfer: " + e.getMessage());
        }
    }

    private static void viewTransactionHistory(BankDataStore dataStore, Account account) {
        try {
            List<String> history = dataStore.getTransactionHistory(account.getUsername());

            if (history.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }

            System.out.println("\n=== Transaction History ===");
            for (String entry : history) {
                System.out.println(entry);
            }
        } catch (IOException e) {
            System.out.println("Unable to read transaction history: " + e.getMessage());
        }
    }

    private static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(SCANNER.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a whole number: ");
            }
        }
    }

    private static double readDouble() {
        while (true) {
            try {
                return Double.parseDouble(SCANNER.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number: ");
            }
        }
    }

    private static void syncAccount(Account target, Account source) {
        double balanceDifference = source.getBalance() - target.getBalance();
        if (balanceDifference > 0) {
            target.deposit(balanceDifference);
        } else if (balanceDifference < 0) {
            target.withdraw(-balanceDifference);
        }
    }
}
