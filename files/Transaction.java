import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One entry in the inventory transaction history (stored in a LinkedList). */
public class Transaction {
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
    private final LocalDateTime time = LocalDateTime.now();
    private final String type, productId, note;
    private final int quantity;

    public Transaction(String type, String productId, int quantity, String note) {
        this.type = type; this.productId = productId; this.quantity = quantity; this.note = note;
    }

    public String getTime() { return FMT.format(time); }
    public String getType() { return type; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getNote() { return note; }
}
