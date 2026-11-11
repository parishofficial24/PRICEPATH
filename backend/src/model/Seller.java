
package model;

public class Seller {

    private int sellerId;
    private String sellerName;
    private double rating;
    private int deliveryDaysAvg;

    public Seller(int sellerId, String sellerName,
                  double rating, int deliveryDaysAvg) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.rating = rating;
        this.deliveryDaysAvg = deliveryDaysAvg;
    }

    public int getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public double getRating() {
        return rating;
    }

    public int getDeliveryDaysAvg() {
        return deliveryDaysAvg;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void setDeliveryDaysAvg(int deliveryDaysAvg) {
        this.deliveryDaysAvg = deliveryDaysAvg;
    }
}
