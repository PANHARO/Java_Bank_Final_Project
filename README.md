# J Bank

A Java banking coursework project with console and browser interfaces, file-based accounts, deposits, withdrawals, and transaction history.

Both interfaces share the banking logic in `BankDataStore.java`. The web version uses Java's built-in HTTP server and a static HTML/CSS/JavaScript frontend; no Maven, Gradle, or external Java libraries are required.

## Requirements and clone

Install Git and a **JDK 11 or newer**, with both `java` and `javac` available in your terminal.

```sh
git clone https://github.com/PANHARO/Java_Bank_Final_Project.git
cd Java_Bank_Final_Project
java -version
javac -version
```

## Compile and start the web app

Run from the repository root. Create `data` before starting: the storage code creates files inside it but does not create the directory itself.

Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force data, bin
javac -d bin src\*.java
java -cp bin WebServer
```

macOS / Linux:

```sh
mkdir -p data bin
javac -d bin src/*.java
java -cp bin WebServer
```

Open `http://localhost:8080`. Register a demo account, log in, and use the dashboard to try deposits, withdrawals, and transaction history. Stop the server with Ctrl+C. If port 8080 is occupied, choose another port:

```sh
java -cp bin WebServer 8081
```

Then visit `http://localhost:8081`.

## Run the console app

After compiling with either command set above:

```sh
java -cp bin Main
```

Follow the terminal prompts to register or log in and select an account action. Keep the working directory at the repository root so `data/` and `web/` resolve correctly. Both interfaces use the same stored accounts; stop one before experimenting with the other.

## How the files fit together

| Path | Purpose |
| --- | --- |
| `src/Account.java` | In-memory account model and balance operations |
| `src/BankDataStore.java` | Registration, login, balances, transactions, and text-file storage |
| `src/Main.java` | Console menus and input handling |
| `src/WebServer.java` | Local HTTP server, frontend serving, and API routes |
| `web/index.html` | Redirect to the authentication page |
| `web/auth.html`, `web/auth.js` | Browser registration and login |
| `web/dashboard.html`, `web/dashboard.js` | Account dashboard and actions |
| `web/styles.css` | Frontend styling and animations |
| `data/` | Runtime storage, created locally |
| `bin/` | Generated compiled classes |

The server exposes `/api/register`, `/api/login`, `/api/account`, `/api/deposit`, `/api/withdraw`, `/api/transfer`, and `/api/history`.

The shared storage files use these formats:

- `data/Users.txt`: `username,password`
- `data/Accounts.txt`: `username,balance`
- `data/Transaction.txt`: `dateTime,username,type,target,amount,balanceAfter`

## Current limitations

This is a learning demo. Use fictional accounts and demo passwords.

- Passwords are stored as plain text, and there is no secure server-side authentication or session management. The browser stores the current username in `sessionStorage`.
- **Transfers are incomplete:** the current implementation deducts the sender's balance and records the target, but does not credit a recipient account.
- Text files are used instead of a database.
- GitHub Pages can serve static frontend files but cannot run this Java backend. Start `WebServer` to use the full app locally.

Keep real personal data and credentials out of `data/` and out of commits. Generated `.class` files in `bin/` should not be committed.

## Troubleshooting and changes

If `javac` is missing, install a JDK rather than only a JRE and check your PATH. If saving fails, check that `data/` exists and is writable. Recompile after changing Java sources; refresh the browser after frontend edits.

To contribute, fork if needed, create a branch with `git switch -c your-change`, and describe the behavior changed and how you checked it. Keep the shared storage behavior consistent between the console and web interfaces.
