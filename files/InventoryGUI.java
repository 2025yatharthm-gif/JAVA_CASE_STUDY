import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Swing front-end for the inventory system. */
public class InventoryGUI extends JFrame {
    private interface Task { void run() throws Exception; }

    private final InventoryService svc;
    private final JLabel status = new JLabel(" ");
    private final Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    // Products
    private final DefaultTableModel pModel = model("ID", "Name", "Category", "Price (Rs)", "Qty", "Supplier", "Stock");
    private final JTable pTable = new JTable(pModel);
    private final JTextField pId = new JTextField(6), pName = new JTextField(12), pPrice = new JTextField(6),
            pQty = new JTextField(5), pSup = new JTextField(6), searchField = new JTextField(12);
    private final JComboBox<String> pCat = new JComboBox<>(InventoryService.CATEGORIES);
    private final JComboBox<String> searchBy = new JComboBox<>(new String[]{"ID", "Name", "Category"});
    private final JComboBox<String> sortBy = new JComboBox<>(new String[]{"ID", "Name", "Price", "Quantity"});

    // Suppliers
    private final DefaultTableModel sModel = model("ID", "Name", "Phone", "Email");
    private final JTable sTable = new JTable(sModel);
    private final JTextField sId = new JTextField(6), sName = new JTextField(14), sPhone = new JTextField(10), sEmail = new JTextField(16);

    // Purchases
    private final DefaultTableModel prModel = model("#", "Date", "Product", "Supplier", "Qty", "Unit Cost", "Total");
    private final JTextField prProd = new JTextField(6), prSup = new JTextField(6), prQty = new JTextField(5), prCost = new JTextField(7);

    // Sales / billing
    private final DefaultTableModel saModel = model("#", "Product", "Qty", "Price", "Amount");
    private final JTextField saProd = new JTextField(6), saQty = new JTextField(5);
    private final JLabel saTotal = new JLabel(" ");
    private final List<Sale> cart = new ArrayList<>();

    // History & report
    private final DefaultTableModel hModel = model("Time", "Type", "Product", "Qty Change", "Note");
    private final JTextArea reportArea = new JTextArea();

    public InventoryGUI(InventoryService svc) {
        super("Retail Inventory Management System");
        this.svc = svc;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Products", productTab());
        tabs.addTab("Suppliers", supplierTab());
        tabs.addTab("Purchases", purchaseTab());
        tabs.addTab("Sales & Billing", salesTab());
        tabs.addTab("History", wrap(new JLabel("Inventory transaction history (newest first)"), new JTable(hModel)));
        tabs.addTab("Report", reportTab());
        add(tabs, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(status, BorderLayout.SOUTH);

        refreshAll();
        if (!svc.lowStock().isEmpty()) SwingUtilities.invokeLater(() -> lowStockAlert(false));
    }

    // ---------- tabs ----------
    private JPanel productTab() {
        pTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) comp.setBackground("LOW".equals(t.getValueAt(r, 6)) ? new Color(255, 220, 220) : Color.WHITE);
                return comp;
            }
        });
        pTable.getSelectionModel().addListSelectionListener(e -> {
            int r = pTable.getSelectedRow();
            if (e.getValueIsAdjusting() || r < 0) return;
            pId.setText(cell(pTable, r, 0)); pName.setText(cell(pTable, r, 1)); pCat.setSelectedItem(cell(pTable, r, 2));
            pPrice.setText(cell(pTable, r, 3)); pQty.setText(cell(pTable, r, 4)); pSup.setText(cell(pTable, r, 5));
        });

        JPanel top = new JPanel(new GridLayout(0, 1));
        top.add(form("ID", pId, "Name", pName, "Category", pCat, "Price", pPrice, "Qty", pQty, "Supplier", pSup));
        top.add(form(
                btn("Add", () -> svc.addProduct(readProduct())),
                btn("Update", () -> svc.updateProduct(readProduct())),
                btn("Delete", () -> {
                    if (confirm("Delete product " + pId.getText().trim() + "?")) svc.deleteProduct(pId.getText().trim().toUpperCase());
                }),
                btn("Clear", this::clearProductForm),
                view("Low Stock Alert", () -> { showProducts(svc.lowStock()); lowStockAlert(true); })));
        top.add(form("Search by", searchBy, searchField, view("Search", this::searchProducts),
                btn("Show All", () -> { }), "   Sort by", sortBy,
                view("Sort", () -> showProducts(svc.sorted((String) sortBy.getSelectedItem())))));
        return wrap(top, pTable);
    }

    private JPanel supplierTab() {
        sTable.getSelectionModel().addListSelectionListener(e -> {
            int r = sTable.getSelectedRow();
            if (e.getValueIsAdjusting() || r < 0) return;
            sId.setText(cell(sTable, r, 0)); sName.setText(cell(sTable, r, 1)); sPhone.setText(cell(sTable, r, 2)); sEmail.setText(cell(sTable, r, 3));
        });
        JPanel top = new JPanel(new GridLayout(0, 1));
        top.add(form("ID", sId, "Name", sName, "Phone", sPhone, "Email", sEmail));
        top.add(form(
                btn("Add", () -> svc.addSupplier(readSupplier())),
                btn("Update", () -> svc.updateSupplier(readSupplier())),
                btn("Delete", () -> {
                    if (confirm("Delete supplier " + sId.getText().trim() + "?")) svc.deleteSupplier(sId.getText().trim().toUpperCase());
                }),
                btn("Clear", () -> { sId.setText(""); sName.setText(""); sPhone.setText(""); sEmail.setText(""); })));
        return wrap(top, sTable);
    }

    private JPanel purchaseTab() {
        JPanel top = form("Product ID", prProd, "Supplier ID", prSup, "Quantity", prQty, "Unit cost", prCost,
                btn("Record Purchase", () -> {
                    svc.purchase(prProd.getText().trim().toUpperCase(), prSup.getText().trim().toUpperCase(),
                            Validator.quantity(prQty.getText(), false), Validator.price(prCost.getText()));
                    prQty.setText("");
                }));
        return wrap(top, new JTable(prModel));
    }

    private JPanel salesTab() {
        JPanel top = form("Product ID", saProd, "Quantity", saQty,
                btn("Sell Item", this::sellItem), btn("Generate Bill", this::generateBill));
        JPanel p = wrap(top, new JTable(saModel));
        saTotal.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        p.add(saTotal, BorderLayout.SOUTH);
        return p;
    }

    private JPanel reportTab() {
        reportArea.setEditable(false);
        reportArea.setFont(mono);
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(form(btn("Refresh Report", () -> { })), BorderLayout.NORTH);
        p.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return p;
    }

    // ---------- actions ----------
    private Product readProduct() throws InvalidInputException {
        String id = pId.getText().trim().toUpperCase();
        Validator.productId(id);
        Validator.text(pName.getText(), "Product name");
        double price = Validator.price(pPrice.getText());
        int qty = Validator.quantity(pQty.getText(), true);
        return new Product(id, pName.getText().trim(), (String) pCat.getSelectedItem(), price, qty, pSup.getText().trim().toUpperCase());
    }

    private Supplier readSupplier() {
        return new Supplier(sId.getText().trim().toUpperCase(), sName.getText().trim(), sPhone.getText().trim(), sEmail.getText().trim());
    }

    private void clearProductForm() {
        pId.setText(""); pName.setText(""); pPrice.setText(""); pQty.setText(""); pSup.setText("");
        pCat.setSelectedIndex(0);
        pTable.clearSelection();
    }

    private void searchProducts() throws InvalidInputException {
        Validator.text(searchField.getText(), "Search text");
        List<Product> found = svc.search((String) searchBy.getSelectedItem(), searchField.getText());
        showProducts(found);
        status.setText("  " + found.size() + " product(s) found");
        status.setForeground(Color.DARK_GRAY);
    }

    private void sellItem() throws Exception {
        String pid = saProd.getText().trim().toUpperCase();
        Sale s = svc.sell(pid, Validator.quantity(saQty.getText(), false));
        cart.add(s);
        saQty.setText("");
        Product p = svc.getProduct(pid);
        if (svc.isLow(p))
            JOptionPane.showMessageDialog(this, "LOW STOCK: " + p.getName() + " has only " + p.getQuantity()
                    + " unit(s) left.\nPlease reorder soon.", "Low Stock Alert", JOptionPane.WARNING_MESSAGE);
    }

    private void generateBill() throws InvalidInputException {
        if (cart.isEmpty()) throw new InvalidInputException("No items on the current bill. Sell an item first.");
        JTextArea ta = new JTextArea(svc.bill(cart), 18, 50);
        ta.setFont(mono);
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Bill", JOptionPane.INFORMATION_MESSAGE);
        cart.clear();
    }

    private void lowStockAlert(boolean showWhenNone) {
        List<Product> low = svc.lowStock();
        if (low.isEmpty()) {
            if (showWhenNone) JOptionPane.showMessageDialog(this, "All products are sufficiently stocked.", "Low Stock Alert", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder("These products are at or below their reorder level:\n\n");
        for (Product p : low)
            sb.append(String.format("%s  %-20s qty %d (reorder at %d)\n", p.getId(), p.getName(), p.getQuantity(), svc.thresholdFor(p.getCategory())));
        JOptionPane.showMessageDialog(this, sb.toString(), "Low Stock Alert", JOptionPane.WARNING_MESSAGE);
    }

    // ---------- refresh ----------
    private void showProducts(List<Product> list) {
        pModel.setRowCount(0);
        for (Product p : list)
            pModel.addRow(new Object[]{p.getId(), p.getName(), p.getCategory(), String.format("%.2f", p.getPrice()),
                    p.getQuantity(), p.getSupplierId(), svc.isLow(p) ? "LOW" : "OK"});
    }

    private void refreshAll() {
        showProducts(svc.sorted("ID"));
        sModel.setRowCount(0);
        for (Supplier s : svc.getSuppliers()) sModel.addRow(new Object[]{s.getId(), s.getName(), s.getPhone(), s.getEmail()});
        prModel.setRowCount(0);
        for (Purchase p : svc.getPurchases())
            prModel.addRow(new Object[]{p.getId(), p.getDate(), p.getProductId(), p.getSupplierId(), p.getQuantity(),
                    String.format("%.2f", p.getUnitCost()), String.format("%.2f", p.getTotal())});
        saModel.setRowCount(0);
        for (Sale s : cart)
            saModel.addRow(new Object[]{s.getId(), s.getProductId(), s.getQuantity(), String.format("%.2f", s.getUnitPrice()), String.format("%.2f", s.getTotal())});
        double sub = svc.subtotal(cart);
        saTotal.setText(String.format("Current bill: Rs %.2f + GST Rs %.2f = Rs %.2f", sub, sub * InventoryService.GST, sub * (1 + InventoryService.GST)));
        hModel.setRowCount(0);
        for (Transaction t : svc.getHistory()) hModel.addRow(new Object[]{t.getTime(), t.getType(), t.getProductId(), t.getQuantity(), t.getNote()});
        reportArea.setText(svc.report());
        reportArea.setCaretPosition(0);
        int low = svc.lowStock().size();
        status.setText(low == 0 ? "  All products sufficiently stocked" : "  WARNING: " + low + " product(s) at or below reorder level");
        status.setForeground(low == 0 ? new Color(0, 120, 0) : Color.RED);
    }

    // ---------- small UI helpers ----------
    private void run(Task t, boolean refresh) {
        try {
            t.run();
        } catch (InsufficientStockException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Insufficient Stock", JOptionPane.ERROR_MESSAGE);
        } catch (InvalidInputException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid Input", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + e, "Error", JOptionPane.ERROR_MESSAGE);
        }
        if (refresh) refreshAll();
    }

    /** Button that changes data, so all views refresh afterwards. */
    private JButton btn(String text, Task t) {
        JButton b = new JButton(text);
        b.addActionListener(e -> run(t, true));
        return b;
    }

    /** Button that only changes what is displayed (search/sort), so no refresh. */
    private JButton view(String text, Task t) {
        JButton b = new JButton(text);
        b.addActionListener(e -> run(t, false));
        return b;
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private JPanel form(Object... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (Object o : items) p.add(o instanceof String ? new JLabel((String) o) : (Component) o);
        return p;
    }

    private JPanel wrap(JComponent top, JTable table) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private static String cell(JTable t, int r, int c) { return String.valueOf(t.getValueAt(r, c)); }

    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
