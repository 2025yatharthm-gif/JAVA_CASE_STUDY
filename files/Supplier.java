public class Supplier {
    private final String id, name, phone, email;

    public Supplier(String id, String name, String phone, String email) {
        this.id = id; this.name = name; this.phone = phone; this.email = email;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    @Override public String toString() { return id + " - " + name; }
}
