import java.time.LocalDateTime;
import java.util.*;

/** All business logic: CRUD, stock, purchases, sales, search, sort, alerts, billing, reports. */
public class InventoryService {
    // Arrays: categories and their low-stock (reorder) thresholds
    public static final String[] CATEGORIES = {"Grocery", "Electronics", "Clothing", "Stationery", "Household"};
    public static final int[] THRESHOLDS    = {20,        5,             10,         25,           8};
    public static final double GST = 0.18;
    private static final String LINE = "------------------------------------------------\n";

    private final Map<String, Product> products = new HashMap<>();     // Product ID -> Product
    private final List<Supplier> suppliers = new ArrayList<>();
    private final List<Purchase> purchases = new ArrayList<>();
    private final List<Sale> sales = new ArrayList<>();
    private final LinkedList<Transaction> history = new LinkedList<>(); // newest first

    // ---------- helpers ----------
    public int thresholdFor(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equalsIgnoreCase(category)) return THRESHOLDS[i];
        return 10;
    }
    public boolean isLow(Product p) { return p.getQuantity() <= thresholdFor(p.getCategory()); }
    public Product getProduct(String id) { return products.get(id); }
    public Supplier findSupplier(String id) {
        for (Supplier s : suppliers) if (s.getId().equals(id)) return s;
        return null;
    }
    private Product requireProduct(String id) throws InvalidInputException {
        Product p = products.get(id);
        if (p == null) throw new InvalidInputException("Product not found: " + id);
        return p;
    }
    private Supplier requireSupplier(String id) throws InvalidInputException {
        Supplier s = findSupplier(id);
        if (s == null) throw new InvalidInputException("Supplier not found: " + id);
        return s;
    }
    private void validate(Product p) throws InvalidInputException {
        Validator.productId(p.getId());
        Validator.text(p.getName(), "Product name");
        Validator.price(p.getPrice());
        Validator.quantity(p.getQuantity(), true);
        boolean okCat = false;
        for (String c : CATEGORIES) if (c.equalsIgnoreCase(p.getCategory())) okCat = true;
        if (!okCat) throw new InvalidInputException("Unknown category: " + p.getCategory());
        Validator.supplierId(p.getSupplierId());
        requireSupplier(p.getSupplierId());
    }
    private void validate(Supplier s) throws InvalidInputException {
        Validator.supplierId(s.getId());
        Validator.text(s.getName(), "Supplier name");
        Validator.phone(s.getPhone());
        Validator.email(s.getEmail());
    }

    // ---------- Product CRUD ----------
    public void addProduct(Product p) throws InvalidInputException {
        validate(p);
        if (products.containsKey(p.getId())) throw new InvalidInputException("Product ID already exists: " + p.getId());
        products.put(p.getId(), p);
        history.addFirst(new Transaction("NEW", p.getId(), p.getQuantity(), "Product created"));
    }
    public void updateProduct(Product p) throws InvalidInputException {
        Product old = requireProduct(p.getId());
        validate(p);
        products.put(p.getId(), p);
        if (old.getQuantity() != p.getQuantity())
            history.addFirst(new Transaction("ADJUST", p.getId(), p.getQuantity() - old.getQuantity(), "Manual stock update"));
    }
    public void deleteProduct(String id) throws InvalidInputException {
        requireProduct(id);
        products.remove(id);
        history.addFirst(new Transaction("DELETE", id, 0, "Product removed"));
    }

    // ---------- Supplier CRUD ----------
    public void addSupplier(Supplier s) throws InvalidInputException {
        validate(s);
        if (findSupplier(s.getId()) != null) throw new InvalidInputException("Supplier ID already exists: " + s.getId());
        suppliers.add(s);
    }
    public void updateSupplier(Supplier s) throws InvalidInputException {
        Supplier old = requireSupplier(s.getId());
        validate(s);
        suppliers.set(suppliers.indexOf(old), s);
    }
    public void deleteSupplier(String id) throws InvalidInputException {
        Supplier s = requireSupplier(id);
        for (Product p : products.values())
            if (p.getSupplierId().equals(id)) throw new InvalidInputException("Cannot delete: supplier still supplies " + p.getName());
        suppliers.remove(s);
    }

    // ---------- Stock: purchases & sales ----------
    public Purchase purchase(String productId, String supplierId, int qty, double unitCost) throws InvalidInputException {
        Product p = requireProduct(productId);
        requireSupplier(supplierId);
        Validator.quantity(qty, false);
        Validator.price(unitCost);
        p.setQuantity(p.getQuantity() + qty);
        Purchase pu = new Purchase(productId, supplierId, qty, unitCost);
        purchases.add(pu);
        history.addFirst(new Transaction("PURCHASE", productId, qty, "From " + supplierId));
        return pu;
    }

    public Sale sell(String productId, int qty) throws InvalidInputException, InsufficientStockException {
        Product p = requireProduct(productId);
        Validator.quantity(qty, false);
        if (qty > p.getQuantity())
            throw new InsufficientStockException("Only " + p.getQuantity() + " unit(s) of " + p.getName() + " in stock; requested " + qty + ".");
        p.setQuantity(p.getQuantity() - qty);
        Sale s = new Sale(productId, qty, p.getPrice());
        sales.add(s);
        history.addFirst(new Transaction("SALE", productId, -qty, "Sold at Rs " + p.getPrice()));
        return s;
    }

    // ---------- Search ----------
    public List<Product> search(String by, String query) {
        List<Product> out = new ArrayList<>();
        if (by.equals("ID")) {
            Product p = products.get(query.trim().toUpperCase());
            if (p != null) out.add(p);
            return out;
        }
        String q = query.trim().toLowerCase();
        for (Product p : byId().values()) {
            String field = by.equals("Name") ? p.getName() : p.getCategory();
            if (field.toLowerCase().contains(q)) out.add(p);
        }
        return out;
    }

    // ---------- Sorting (TreeMap views + comparators) ----------
    public TreeMap<String, Product> byId() { return new TreeMap<>(products); }

    public TreeMap<Double, List<Product>> byPrice() {
        TreeMap<Double, List<Product>> m = new TreeMap<>();
        for (Product p : products.values()) m.computeIfAbsent(p.getPrice(), k -> new ArrayList<>()).add(p);
        return m;
    }

    public List<Product> sorted(String key) {
        List<Product> list = new ArrayList<>();
        switch (key) {
            case "Price":
                for (List<Product> group : byPrice().values()) list.addAll(group);
                break;
            case "Name":
                list.addAll(products.values());
                list.sort(Comparator.comparing((Product p) -> p.getName().toLowerCase()));
                break;
            case "Quantity":
                list.addAll(products.values());
                list.sort(Comparator.comparingInt(Product::getQuantity));
                break;
            default:
                list.addAll(byId().values());
        }
        return list;
    }

    // ---------- Low stock ----------
    public List<Product> lowStock() {
        List<Product> out = new ArrayList<>();
        for (Product p : products.values()) if (isLow(p)) out.add(p);
        out.sort(Comparator.comparingInt(Product::getQuantity));
        return out;
    }

    // ---------- Billing ----------
    public double subtotal(List<Sale> items) {
        double t = 0;
        for (Sale s : items) t += s.getTotal();
        return t;
    }

    public String bill(List<Sale> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("           RETAIL STORE - TAX INVOICE\n");
        sb.append("Date: ").append(LocalDateTime.now().format(Transaction.FMT)).append("\n").append(LINE);
        sb.append(String.format("%-22s %4s %9s %10s\n", "Item", "Qty", "Price", "Amount")).append(LINE);
        for (Sale s : items) {
            Product p = products.get(s.getProductId());
            String name = p == null ? s.getProductId() : p.getName();
            if (name.length() > 22) name = name.substring(0, 22);
            sb.append(String.format("%-22s %4d %9.2f %10.2f\n", name, s.getQuantity(), s.getUnitPrice(), s.getTotal()));
        }
        double sub = subtotal(items), tax = sub * GST;
        sb.append(LINE);
        sb.append(String.format("%-37s %10.2f\n", "Subtotal", sub));
        sb.append(String.format("%-37s %10.2f\n", "GST (18%)", tax));
        sb.append(String.format("%-37s %10.2f\n", "TOTAL (Rs)", sub + tax));
        sb.append(LINE).append("         Thank you for shopping with us!\n");
        return sb.toString();
    }

    // ---------- Report ----------
    public String report() {
        double stockValue = 0, spent = 0, revenue = 0;
        TreeMap<String, Double> byCategory = new TreeMap<>();
        for (Product p : products.values()) {
            double v = p.getPrice() * p.getQuantity();
            stockValue += v;
            byCategory.merge(p.getCategory(), v, Double::sum);
        }
        for (Purchase pu : purchases) spent += pu.getTotal();
        for (Sale s : sales) revenue += s.getTotal();

        StringBuilder sb = new StringBuilder("===== INVENTORY REPORT =====\n\n");
        sb.append(String.format("Products: %d    Suppliers: %d\n", products.size(), suppliers.size()));
        sb.append(String.format("Total stock value : Rs %,.2f\n", stockValue));
        sb.append(String.format("Total purchases   : Rs %,.2f  (%d orders)\n", spent, purchases.size()));
        sb.append(String.format("Total sales       : Rs %,.2f  (%d lines)\n", revenue, sales.size()));
        sb.append("\n--- Stock value by category ---\n");
        for (Map.Entry<String, Double> e : byCategory.entrySet())
            sb.append(String.format("%-14s Rs %,.2f\n", e.getKey(), e.getValue()));
        sb.append("\n--- Low stock products ---\n");
        List<Product> low = lowStock();
        if (low.isEmpty()) sb.append("None. All products sufficiently stocked.\n");
        for (Product p : low)
            sb.append(String.format("%-6s %-22s qty %3d (reorder level %d)\n", p.getId(), p.getName(), p.getQuantity(), thresholdFor(p.getCategory())));
        sb.append("\n--- Last 5 transactions ---\n");
        int n = 0;
        for (Transaction t : history) {
            if (n++ == 5) break;
            sb.append(String.format("%s  %-8s %-6s %+d\n", t.getTime(), t.getType(), t.getProductId(), t.getQuantity()));
        }
        return sb.toString();
    }

    // ---------- Getters ----------
    public List<Supplier> getSuppliers() { return suppliers; }
    public List<Purchase> getPurchases() { return purchases; }
    public LinkedList<Transaction> getHistory() { return history; }

    // ---------- Sample data ----------
    public void seedSampleData() {
        try {
            addSupplier(new Supplier("S001", "FreshFarm Traders", "9820012345", "sales@freshfarm.in"));
            addSupplier(new Supplier("S002", "TechSource India", "9820067890", "orders@techsource.in"));
            addSupplier(new Supplier("S003", "Metro General Supplies", "9820011122", "hello@metrosupplies.in"));
            addProduct(new Product("P001", "Basmati Rice 5kg", "Grocery", 480, 45, "S001"));
            addProduct(new Product("P002", "Sunflower Oil 1L", "Grocery", 165, 12, "S001"));
            addProduct(new Product("P003", "Wireless Mouse", "Electronics", 599, 4, "S002"));
            addProduct(new Product("P004", "USB-C Cable", "Electronics", 249, 30, "S002"));
            addProduct(new Product("P005", "Cotton T-Shirt", "Clothing", 399, 25, "S003"));
            addProduct(new Product("P006", "Notebook A4", "Stationery", 60, 18, "S003"));
            addProduct(new Product("P007", "Dish Wash Liquid", "Household", 95, 40, "S003"));
        } catch (InvalidInputException e) {
            throw new IllegalStateException(e);
        }
    }
}
