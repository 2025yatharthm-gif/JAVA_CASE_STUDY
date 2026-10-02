import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventoryService service = new InventoryService();
            service.seedSampleData();
            new InventoryGUI(service).setVisible(true);
        });
    }
}
