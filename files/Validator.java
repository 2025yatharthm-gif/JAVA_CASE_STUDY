/** Central place for all input validation. */
public final class Validator {
    private Validator() {}

    public static void productId(String id) throws InvalidInputException {
        if (id == null || !id.matches("P\\d{3,6}"))
            throw new InvalidInputException("Product ID must be 'P' followed by 3-6 digits (e.g. P001).");
    }

    public static void supplierId(String id) throws InvalidInputException {
        if (id == null || !id.matches("S\\d{3,6}"))
            throw new InvalidInputException("Supplier ID must be 'S' followed by 3-6 digits (e.g. S001).");
    }

    public static void text(String v, String field) throws InvalidInputException {
        if (v == null || v.trim().isEmpty()) throw new InvalidInputException(field + " cannot be empty.");
    }

    public static int quantity(String s, boolean allowZero) throws InvalidInputException {
        int q;
        try { q = Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { throw new InvalidInputException("Quantity must be a whole number."); }
        quantity(q, allowZero);
        return q;
    }

    public static void quantity(int q, boolean allowZero) throws InvalidInputException {
        if (q < 0 || (q == 0 && !allowZero))
            throw new InvalidInputException(allowZero ? "Quantity cannot be negative." : "Quantity must be greater than zero.");
    }

    public static double price(String s) throws InvalidInputException {
        double p;
        try { p = Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { throw new InvalidInputException("Price must be a valid number."); }
        price(p);
        return p;
    }

    public static void price(double p) throws InvalidInputException {
        if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0)
            throw new InvalidInputException("Price must be greater than zero.");
    }

    public static void phone(String s) throws InvalidInputException {
        if (s == null || !s.matches("\\d{10}")) throw new InvalidInputException("Phone must be exactly 10 digits.");
    }

    public static void email(String s) throws InvalidInputException {
        if (s == null || !s.matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")) throw new InvalidInputException("Email address is not valid.");
    }
}
