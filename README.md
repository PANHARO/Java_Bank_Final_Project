# J Bank

A simple Java banking project with:

- A console app in `src/Main.java`
- A web server in `src/WebServer.java`
- Static frontend files in `web/`
- Plain text storage managed by `src/BankDataStore.java`

## Project Structure

- `src/`: Java source files
- `src/Main.java`: console version of the app
- `src/WebServer.java`: HTTP server and API routes
- `src/BankDataStore.java`: file-based storage logic
- `src/Account.java`: account model
- `web/`: frontend HTML, CSS, and JavaScript
- `data/`: runtime text files created and used by the app
- `bin/`: compiled Java output

## Run The Web App

Compile:

```powershell
javac -d bin src\*.java
```

Start the server:

```powershell
java -cp bin WebServer
```

Then open `http://localhost:8080`.

## Run The Console App

Compile:

```powershell
javac -d bin src\*.java
```

Start:

```powershell
java -cp bin Main
```

## Notes

- This project uses text files instead of a database.
- Do not publish real user data or passwords from the files inside `data/`.
- Compiled `.class` files in `bin/` are generated output and should not be committed.
- The duplicate root frontend files were removed so `web/` is the single frontend source.

## Detailed Overview

### How the bank system works

The project has one banking engine with two interfaces:

- `src/Main.java` provides the console interface.
- `src/WebServer.java` provides the local web server and API.
- `src/BankDataStore.java` contains the shared banking and file-storage logic.
- `src/Account.java` represents an account in memory.

Both interfaces use the same files in `data/`:

1. A user registers, and the system saves the username and password in `Users.txt`.
2. The starting balance is saved in `Accounts.txt`.
3. On login, the system checks the credentials and loads the account balance.
4. Deposits, withdrawals, and transfers update the balance and add a record to `Transaction.txt`.
5. The console or browser displays the updated account and transaction history.

### Main Java files

`Account.java` is the in-memory account model. It stores the username, password, and balance, and provides deposit and withdrawal operations. It does not read or write files.

`BankDataStore.java` is the core of the application. It creates the data files, creates accounts, authenticates users, loads and updates balances, records transactions, and returns transaction history. Most of its methods are synchronized so both web and console requests can use the storage safely.

`Main.java` handles console input, menus, validation, login, and account actions. It calls `BankDataStore` instead of managing files directly.

`WebServer.java` starts an HTTP server on port `8080`, serves files from `web/`, and connects browser requests to the banking logic through these routes:

```text
/api/register   /api/login     /api/account
/api/deposit    /api/withdraw  /api/transfer
/api/history
```

### Frontend files

- `web/index.html`: redirects visitors to the authentication page.
- `web/auth.html` and `web/auth.js`: provide registration and login.
- `web/dashboard.html` and `web/dashboard.js`: display the account and handle banking actions.
- `web/styles.css`: contains the page layout, colors, responsive design, and animations.

The browser stores the current username in `sessionStorage`. This is only a simple local demo session, not secure authentication.

### Data files

- `data/Users.txt`: `username,password`
- `data/Accounts.txt`: `username,balance`
- `data/Transaction.txt`: `dateTime,username,type,target,amount,balanceAfter`

These text files act as the application's database. Do not use real passwords or personal data in them.

### Current limitations

- Passwords are stored in plain text.
- There is no real authentication or server-side session management.
- `transfer()` deducts money from the sender but currently does not credit the recipient.
- The project uses text files instead of a database.
- The Java backend must run locally; GitHub Pages can host only the frontend.

### Quick file summary

```text
Account.java       account model
BankDataStore.java banking logic and file storage
Main.java          console interface
WebServer.java     web server and API
web/               frontend pages, scripts, and styles
data/              saved users, accounts, and transactions
```
