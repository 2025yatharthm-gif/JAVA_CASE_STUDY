/** Thrown when a sale requests more units than are in stock. */
public class InsufficientStockException extends Exception {
    public InsufficientStockException(String message) { super(message); }
}
