
package model;

public class Product {

    private int productId;
    private String name;
    private String category;
    private String brand;
    private String description;

    public Product(int productId, String name, String category,
                   String brand, String description) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.description = description;
    }

    public int getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
