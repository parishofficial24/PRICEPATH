
package model;

public class Recommendation {

    private int recommendationId;
    private int userId;
    private String title;
    private String description;

    public Recommendation(int recommendationId, int userId,
                          String title, String description) {
        this.recommendationId = recommendationId;
        this.userId = userId;
        this.title = title;
        this.description = description;
    }

    public int getRecommendationId() {
        return recommendationId;
    }

    public int getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
