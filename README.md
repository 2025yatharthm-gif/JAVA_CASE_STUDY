# Inventory Management System for a Retail Store

Java Programming, Case Study 37

A desktop application built with Java Swing that manages products, suppliers, stock, purchases, sales and billing for a retail store. It alerts the store manager when stock runs low.

## Features

- Product and supplier management (add, update, delete)
- Automatic stock tracking from purchases and sales
- Purchase and sales recording
- Low-stock alerts (red rows, startup popup, status bar, popup after a sale)
- Search by ID, name or category
- Sort by ID, name, price or quantity
- Billing with 18% GST
- Inventory report and transaction history
- Input validation and custom exception handling

## Java Concepts Used

| Concept | Usage |
|---|---|
| Classes and constructors | Product, Supplier, Purchase, Sale, Transaction |
| Arrays | Category names and low-stock thresholds |
| ArrayList | Suppliers, purchases, sales |
| LinkedList | Inventory transaction history (newest first) |
| HashMap | Product ID to Product lookup |
| TreeMap | Products sorted by ID and price, category totals |
| CRUD, Searching, Sorting | Products and suppliers |
| Swing | Tabbed GUI with tables and dialogs |
| Exceptions | InvalidInputException, InsufficientStockException |
| Validation | Validator class |

## Project Structure

```
InventorySystem/
├── Main.java                       Entry point
├── InventoryGUI.java               Swing interface
├── InventoryService.java           Business logic
├── Product.java
├── Supplier.java
├── Purchase.java
├── Sale.java
├── Transaction.java
├── Validator.java
├── InvalidInputException.java
└── InsufficientStockException.java
```

## Requirements

- JDK 8 or later

## How to Run

```
javac *.java
java Main
```

The app starts with sample data (3 suppliers, 7 products).

## Using the App

| Tab | Purpose |
|---|---|
| Products | Manage products, search, sort, view low stock |
| Suppliers | Manage suppliers |
| Purchases | Record purchases from suppliers |
| Sales & Billing | Sell items and generate the bill |
| History | View all inventory transactions |
| Report | View the inventory report |

## Validation Rules

- Product ID: `P` plus 3 to 6 digits (e.g. P001)
- Supplier ID: `S` plus 3 to 6 digits (e.g. S001)
- Price: greater than 0
- Quantity: whole number (0 or more for products, above 0 for purchases and sales)
- Phone: 10 digits
- Email: valid format

## Low-Stock Reorder Levels

| Category | Level |
|---|---|
| Grocery | 20 |
| Electronics | 5 |
| Clothing | 10 |
| Stationery | 25 |
| Household | 8 |

## Limitations

- Data is stored in memory only and resets when the app closes.
- Single user, no login.

## Future Enhancements

- File or database storage (JDBC)
- Login with roles
- PDF or CSV export
- Per-product reorder levels
