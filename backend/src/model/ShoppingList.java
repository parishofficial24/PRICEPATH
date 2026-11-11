
package model;

public class ShoppingList {

    private int listId;
    private int userId;
    private String listName;

    public ShoppingList(int listId, int userId, String listName) {
        this.listId = listId;
        this.userId = userId;
        this.listName = listName;
    }

    public int getListId() {
        return listId;
    }

    public int getUserId() {
        return userId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }
}
