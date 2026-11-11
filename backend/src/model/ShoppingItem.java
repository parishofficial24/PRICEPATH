
package model;

public class ShoppingItem {

    private int itemId;
    private int listId;
    private int productId;
    private int quantity;

    public ShoppingItem(int itemId, int listId,
                        int productId, int quantity) {
        this.itemId = itemId;
        this.listId = listId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public int getItemId() {
        return itemId;
    }

    public int getListId() {
        return listId;
    }

    public int getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity > 0) {
            this.quantity = quantity;
        }
    }
}
