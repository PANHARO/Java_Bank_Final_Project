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


## Detailed 
System Map
Your project is a small banking system with two interfaces using the same storage:
Console app: src/Main.java
Web app backend: src/WebServer.java
Shared data layer: src/BankDataStore.java
Account model: src/Account.java
Frontend pages/scripts/styles: web/
The real flow is:
User signs in through console or browser.
App calls BankDataStore.
BankDataStore reads/writes data/Users.txt, data/Accounts.txt, data/Transaction.txt.
Updated data is shown back in console or browser.

Backend Files
Account.java
This is the account object used in memory.
It contains:
username
password
balance
It provides:
getters for those fields
deposit(amount) to add money
withdraw(amount) to subtract money if enough balance exists
It does not touch files. It is only a simple data object plus balance operations.
BankDataStore.java
This is the most important file in the whole project. It is the storage and business-logic layer.
It owns three files:
Users.txt: usernames and passwords
Accounts.txt: usernames and balances
Transaction.txt: transaction history
Main responsibilities:
initialize storage files
create accounts
authenticate users
load accounts
update balances
record transactions
return history
Important methods:
initializeFiles()
creates missing files and writes header rows
userExists(username)
checks if a username already exists
createAccount(account)
writes new user to both Users.txt and Accounts.txt
authenticate(username, password)
resolves username, checks password, returns Account if valid
getAccount(username)
loads a stored account and current balance
updateBalance(username, newBalance)
rewrites the user’s balance in Accounts.txt
recordTransaction(...)
appends a row to Transaction.txt
getTransactionHistory(username)
reads that user’s transaction rows and formats them into readable strings
Money operations:
deposit(username, amount)
validates amount > 0
loads user
increases balance
updates file
records DEPOSIT
withdraw(username, amount)
validates amount > 0
checks enough balance
updates file
records WITHDRAW
transfer(senderName, recipientName, amount)
validates recipient and amount
prevents self-transfer
subtracts from sender
records TRANSFER_OUT
Important issue:
transfer() does not credit the recipient
so transfer is incomplete as a banking operation
Technical note:
most methods are synchronized
that is good because the web server can receive multiple requests
Main.java
This is the console interface.
Startup:
creates BankDataStore
initializes files
shows a looped main menu
Main menu options:
create account
login
exit
After login, the account menu offers:
deposit
withdraw
transfer
view balance
view transaction history
logout
Key methods:
createAccount(...)
reads name/password from terminal
validates input
creates account
loginUser(...)
checks credentials
opens logged-in menu
loggedInMenu(...)
handles post-login actions
deposit(...), withdraw(...), transfer(...)
call BankDataStore
then sync in-memory account object
viewTransactionHistory(...)
prints history entries
readInt() / readDouble()
keep asking until input is valid
Why syncAccount(...) exists:
the current session keeps an Account object
storage methods return updated account objects
syncAccount updates the old object so console balance stays correct
WebServer.java
This is the browser backend.
What it does:
starts an HTTP server on port 8080
serves static files from web/
exposes banking API endpoints
Startup flow:
create BankDataStore
initialize files
start HttpServer
register routes
API routes:
/api/register
/api/login
/api/account
/api/deposit
/api/withdraw
/api/transfer
/api/history
Shared API helper:
ApiHandler
parses form/query data
parses amounts
sends JSON
sends error JSON
enforces POST when needed
Specific handlers:
RegisterHandler
creates account and returns account data
LoginHandler
validates login and returns account data
AccountHandler
returns current account snapshot
DepositHandler
deposits money and returns updated snapshot
WithdrawHandler
withdraws money and returns updated snapshot
TransferHandler
sends money from sender and returns updated sender snapshot
HistoryHandler
returns account plus full history
StaticFileHandler
serves files under web/
if request path is missing, falls back to web/index.html
Important detail:
local Java web app uses web/
root HTML/JS/CSS copies are not used by the Java server

Frontend Files
web/index.html
Purpose:
immediately redirects to auth.html
This is just the web entry file.
web/auth.html
Purpose:
login/register page
Structure:
decorative top welcome banner
CSS-built bank logo
register form
sign-in form
status message area
Important ids:
register-form
login-form
auth-status
web/auth.js
Purpose:
frontend logic for auth page
What it does:
checks sessionStorage for existing username
redirects to dashboard if already “logged in”
intercepts both form submits
sends POST requests to:
/api/register
/api/login
stores username in sessionStorage
redirects to dashboard on success
updates the status box on success/failure
Important limitation:
this is not real secure auth
browser session is only tracked through sessionStorage
web/dashboard.html
Purpose:
main banking workspace
Main sections:
top navigation bar
hero section with welcome and balance
summary tiles
4 action cards:
deposit
withdraw
transfer
history
hidden history feed section
Important ids:
welcome
dashboard-status
balance
active-user
history-count
sidebar-user
sidebar-balance
recent-type
last-action
history-note
history-list
web/dashboard.js
Purpose:
dashboard behavior and API communication
Startup:
reads username from sessionStorage
redirects to auth page if missing
binds event handlers
loads account immediately
User actions wired here:
deposit form -> /api/deposit
withdraw form -> /api/withdraw
transfer form -> /api/transfer
refresh button -> /api/account
logout button -> clear session and redirect
history button -> reveal timeline
Main functions:
submitAction(...)
sends action request and handles response
loadAccount()
refreshes account snapshot from backend
updateState(data)
updates page with returned account info
renderHistory()
builds transaction timeline from history strings
parseHistoryEntry(...)
parses a history line into type/title/meta
friendlyType(...)
converts raw types into readable labels
updateInsights()
updates recent activity summary
showHistoryPanel()
unhides history panel and scrolls to it
animateMoney(from, to)
animates balance changes
pulseBalanceChange(...)
green pulse on increase, red pulse on decrease
This file is what makes the dashboard feel interactive.
web/styles.css
Purpose:
all visual styling for auth page and dashboard
What it includes:
CSS variables for color system
layout utilities
dark theme styling
form styling
dashboard card styling
banner/logo styling
action icon styling
timeline styling
responsive breakpoints
money pulse animations
Notable parts:
auth logo is built with CSS, not image files
dashboard balance animation styles live here
responsive behavior for phone/tablet/laptop is here

Storage Files
Users.txt
stores usernames and passwords
format: username,password
Accounts.txt
stores usernames and balances
format: username,balance
Transaction.txt
stores all transactions
format:
dateTime,username,type,target,amount,balanceAfter
These are the actual database for the app.

Root Copies
The old root-level frontend copies were removed during cleanup.
The `web/` folder is now the single frontend source used by the project.

Files That Are Not Core Logic
.class files: compiled Java output
bin/: build/generated output
.vscode/: editor settings
README.md: project instructions

Real Strengths
clear separation between model, storage, console UI, web backend, and web frontend
simple file-based persistence
both console and web use the same BankDataStore
responsive frontend is much stronger than the backend complexity would suggest

Important Weak Points
passwords are stored as plain text
no real session/auth security
transfer does not credit recipient
no database, only text files
JSON is manually built as strings
GitHub Pages can only host the frontend, not the Java backend

Best One-Sentence Summary
This project is a file-based Java banking system with both a console UI and a custom web UI, where BankDataStore.java is the real engine and everything else is a way to interact with it.
If you want, I can now do one of these:
explain each file line by line
show the exact flow for “register -> login -> deposit -> history”
review the code like a teacher and point out what is good and what should be improved
9:37 PM
like first tell how it works the bank system and also the files code thanks
9:38 PM
How It Works
Your bank system has 3 main parts:
The bank logic
The saved data files
The user interfaces
The bank logic is the core. It handles things like:
creating accounts
logging in
depositing money
withdrawing money
transferring money
showing balance
showing transaction history
That logic is mainly inside BankDataStore.java and Account.java.
The saved data files act like a simple database:
data/Users.txt: stores username and password
data/Accounts.txt: stores username and balance
data/Transaction.txt: stores transaction history
Then there are 2 ways to use the system:
console app: src/Main.java
web app: src/WebServer.java with HTML/CSS/JS files

Bank System Flow
When the program starts:
Main.java starts the console version
or WebServer.java starts the website version
Both use the same storage class:
BankDataStore
Typical flow:
User creates account
system checks if username already exists
saves username/password to data/Users.txt
saves username/balance to data/Accounts.txt
starting balance is 0.0
User logs in
system checks name and password from data/Users.txt
if correct, loads balance from data/Accounts.txt
User does banking actions
deposit: adds money
withdraw: removes money if enough balance
transfer: removes money from sender
every action updates files
every action is written to data/Transaction.txt
User views history
system reads all matching rows from data/Transaction.txt
shows them as readable records

Main Files
Account.java
This is the account model.
It stores:
username
password
balance
It also has:
deposit(amount)
withdraw(amount)
So this file represents one account object in memory.
BankDataStore.java
This is the most important file.
It is the real engine of the system.
It does:
create files if missing
create accounts
check login
load account balance
update balance
record transactions
return transaction history
Important methods:
initializeFiles()
createAccount(...)
authenticate(...)
getAccount(...)
deposit(...)
withdraw(...)
transfer(...)
getTransactionHistory(...)
This file reads and writes the .txt files.
Main.java
This is the console interface.
It shows menus like:
create account
login
deposit
withdraw
transfer
view balance
view transaction history
It does not store data itself.
It just asks the user for input and then calls BankDataStore.
WebServer.java
This is the web backend.
It starts a local server on port 8080.
It provides API routes like:
/api/register
/api/login
/api/account
/api/deposit
/api/withdraw
/api/transfer
It also serves the frontend files from the web/ folder.
So this file connects:
browser
bank logic
data files

Website Files
web/index.html
This just redirects the user to auth.html.
web/auth.html
This is the first page.
It shows:
welcome banner
create account form
sign in form
status message
web/auth.js
This controls the auth page.
It does:
send register request to /api/register
send login request to /api/login
save logged-in username in sessionStorage
redirect to dashboard.html
web/dashboard.html
This is the banking dashboard.
It shows:
current balance
account holder
total activity
deposit form
withdraw form
transfer form
history section
web/dashboard.js
This controls the dashboard.
It does:
load account info from backend
submit deposit/withdraw/transfer actions
update balance on screen
animate balance increase/decrease
render history records
handle refresh and logout
web/styles.css
This is all the design.
It controls:
colors
spacing
forms
cards
dashboard layout
responsive design
animations

Important Notes
Good:
console app and website use the same bank logic
data is saved permanently in text files
UI is much more polished than a basic school project
Current issue:
transfer only removes money from sender
it does not add money to recipient
Also:
passwords are saved in plain text
there is no real secure login session

Simple Summary
Account.java = one account object
BankDataStore.java = bank logic + file storage
Main.java = console app
WebServer.java = website backend
web/auth.html, web/dashboard.html = website pages
web/auth.js, web/dashboard.js = website behavior
web/styles.css = website design
data/Users.txt, data/Accounts.txt, data/Transaction.txt = saved data
