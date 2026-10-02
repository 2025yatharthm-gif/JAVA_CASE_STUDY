import java.time.LocalDateTime;

public class Purchase {
    private static int counter = 1;
    private final int id;
    private final String productId, supplierId;
    private final int quantity;
    private final double unitCost;
    private final LocalDateTime date = LocalDateTime.now();

    public Purchase(String productId, String supplierId, int quantity, double unitCost) {
        this.id = counter++;
        this.productId = productId; this.supplierId = supplierId;
        this.quantity = quantity; this.unitCost = unitCost;
    }

    public int getId() { return id; }
    public String getProductId() { return productId; }
    public String getSupplierId() { return supplierId; }
    public int getQuantity() { return quantity; }
    public double getUnitCost() { return unitCost; }
    public double getTotal() { return quantity * unitCost; }
    public String getDate() { return Transaction.FMT.format(date); }
}
