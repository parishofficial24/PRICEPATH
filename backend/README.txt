PRICEPATH — LOCAL RUN GUIDE
===========================

Requirements
- Windows 10/11
- JDK 17 (java and javac available in VS Code terminal)
- MySQL Server running
- Existing database named pricepath_new with the finalized tables

IMPORTANT DATABASE SAFETY
- Do NOT run/import ../schema.sql against your existing database. It is a reference schema only.
- The app connects to localhost:3306/pricepath_new as MySQL user root.
- Set PRICEPATH_DB_PASSWORD in the same PowerShell terminal before running.
- Never commit or share your password.

Run with the bundled Connector/J (recommended for the demo)
1. Open this backend folder in VS Code terminal.
2. In PowerShell: $env:PRICEPATH_DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
3. Run: .\run.bat
4. Keep terminal open and visit http://localhost:8080/login.html

Build with Maven (optional; requires Maven installed and internet for first dependency download)
- From backend folder: mvn clean compile
- Run: mvn exec:java
- Maven is a build/dependency tool; Java HttpServer is still the API server and JDBC connects to MySQL.

API smoke tests
- http://localhost:8080/api/products
- http://localhost:8080/api/categories
- http://localhost:8080/api/products/1/compare (replace 1 with an existing ProductID)

The product listing supports q and category query parameters. Offer responses include stock status,
price components, finalPrice, and eligible. Out-of-stock offers are shown but marked ineligible.
Discount is currently treated as a flat currency amount in the final-cost formula. Confirm this matches
your actual SellerProducts data before presenting totals.

Troubleshooting
- Access denied: confirm PRICEPATH_DB_PASSWORD and MySQL user permissions.
- Port already in use: stop the other server with Ctrl+C.
- Build errors: read the first javac error and fix that before testing the browser.
- Empty products: test /api/products directly; if it returns an error, inspect the terminal.
