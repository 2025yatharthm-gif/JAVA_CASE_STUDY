public class Product {
    private String id, name, category, supplierId;
    private double price;
    private int quantity;

    public Product(String id, String name, String category, double price, int quantity, String supplierId) {
        this.id = id; this.name = name; this.category = category;
        this.price = price; this.quantity = quantity; this.supplierId = supplierId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getSupplierId() { return supplierId; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override public String toString() { return id + " - " + name; }
}
