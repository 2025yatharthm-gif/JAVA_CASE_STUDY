import java.time.LocalDateTime;

public class Sale {
    private static int counter = 1;
    private final int id;
    private final String productId;
    private final int quantity;
    private final double unitPrice;
    private final LocalDateTime date = LocalDateTime.now();

    public Sale(String productId, int quantity, double unitPrice) {
        this.id = counter++;
        this.productId = productId; this.quantity = quantity; this.unitPrice = unitPrice;
    }

    public int getId() { return id; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotal() { return quantity * unitPrice; }
    public String getDate() { return Transaction.FMT.format(date); }
}
