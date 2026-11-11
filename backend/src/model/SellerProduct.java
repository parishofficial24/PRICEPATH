
package model;

public class SellerProduct {

    private int sellerProductId;
    private int productId;
    private int sellerId;
    private double price;
    private double discount;
    private double deliveryCharge;
    private double platformFee;
    private int deliveryDays;
    private String stockStatus;

    public SellerProduct(int sellerProductId, int productId,
                         int sellerId, double price,
                         double discount, double deliveryCharge,
                         double platformFee, int deliveryDays,
                         String stockStatus) {
        this.sellerProductId = sellerProductId;
        this.productId = productId;
        this.sellerId = sellerId;
        this.price = price;
        this.discount = discount;
        this.deliveryCharge = deliveryCharge;
        this.platformFee = platformFee;
        this.deliveryDays = deliveryDays;
        this.stockStatus = stockStatus;
    }

    public int getSellerProductId() {
        return sellerProductId;
    }

    public int getProductId() {
        return productId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public double getPrice() {
        return price;
    }

    public double getDiscount() {
        return discount;
    }

    public double getDeliveryCharge() {
        return deliveryCharge;
    }

    public double getPlatformFee() {
        return platformFee;
    }

    public int getDeliveryDays() {
        return deliveryDays;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public void setDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }

    public void setPlatformFee(double platformFee) {
        this.platformFee = platformFee;
    }

    public void setDeliveryDays(int deliveryDays) {
        this.deliveryDays = deliveryDays;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }
}
