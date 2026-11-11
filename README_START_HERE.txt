PRICEPATH — FINAL CONSOLIDATED PROJECT

STACK
- Java 17 built-in HTTP server
- JDBC + included MySQL Connector/J
- Existing MySQL database: pricepath_new
- Static HTML/CSS/JavaScript frontend served by Java

IMPORTANT
- Do NOT run schema.sql. It is retained as documentation/reference only.
- Do not drop, recreate, or reset pricepath_new.
- Database password is read from PRICEPATH_DB_PASSWORD.
- Discounts in the sample implementation are treated as flat rupee amounts. Confirm this matches your dataset.
- Sample seller prices are demonstration data, not live retailer prices.

RUN IN VS CODE (Windows PowerShell)
1. Open the Pricepath_new folder.
2. Ensure MySQL Server is running and pricepath_new already exists.
3. Open Terminal and run: cd backend (only if terminal is at the project root).
4. Set password for this terminal: $env:PRICEPATH_DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
5. Start: .\run.bat
6. Keep terminal open.
7. Open http://localhost:8080/login.html

API CHECKS
- http://localhost:8080/api/products (may require database connection)
- http://localhost:8080/api/categories
- http://localhost:8080/api/health

API routes are hosted by the Java server. No separate Node.js server is required. Maven is optional; run.bat compiles with the included Connector/J jar.

DATABASE TABLES USED
Users, Products, Sellers, SellerProducts, ShoppingLists, ShoppingListItems, Recommendations, RecommendationItems, Orders, PriceHistory.

KNOWN LIMITATION
The project must be smoke-tested against your local MySQL instance. This environment cannot access the MySQL server running on your computer, so a successful Java compile does not prove your local database credentials/data are valid.

OPTIONAL DEMO DATA: demo_catalog_and_history.sql adds sample products, seller offers, and SYNTHETIC historical snapshots. Run only if you want demonstration data; it does not drop tables. Historical records are illustrative, not real retailer history.


CATEGORY / PRODUCT DATA SETUP
-----------------------------
The Products page loads categories dynamically from GET /api/categories.
That endpoint queries SELECT DISTINCT Category FROM Products, so the visible
category chips and dropdown are populated from your MySQL pricepath_new database.
To add Clothing, Food, Home Decor, Vehicle Accessories and the other sample
categories, review and run demo_catalog_and_history.sql in MySQL Workbench
against pricepath_new. It inserts sample products, seller offers, and synthetic
price-history snapshots without dropping or recreating tables. The history
records are generated demo data, not real historical retailer prices.

After adding products, restart/reload the website and visit /products.html.
The category chips will appear automatically from database values. If a category
does not appear, confirm that at least one product exists in that category:
SELECT Category, COUNT(*) FROM Products GROUP BY Category;
