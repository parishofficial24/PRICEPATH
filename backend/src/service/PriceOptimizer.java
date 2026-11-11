
package service;

import java.util.List;
import java.util.Map;

public class PriceOptimizer {

    public double calculateFinalPrice(
            double price, double discount,
            double deliveryCharge, double platformFee) {

        if (price < 0 || discount < 0 || discount > price
                || deliveryCharge < 0 || platformFee < 0) {
            throw new IllegalArgumentException(
                "Invalid price or fee values."
            );
        }

        return price - discount
                + deliveryCharge + platformFee;
    }

    public Map<String, Object> findCheapestOffer(
            List<Map<String, Object>> offers) {

        Map<String, Object> cheapest = null;
        double lowestPrice = Double.MAX_VALUE;

        for (Map<String, Object> offer : offers) {
            Object value = offer.get("finalPrice");

            if (value instanceof Number) {
                double total = ((Number) value).doubleValue();

                if (total < lowestPrice) {
                    lowestPrice = total;
                    cheapest = offer;
                }
            }
        }

        return cheapest;
    }
}
