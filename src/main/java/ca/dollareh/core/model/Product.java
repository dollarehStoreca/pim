package ca.dollareh.core.model;

public record Product(Category category,
                      String code,
                      String title,
                      String description,
                      float price,
                      String[] imageUrls) {
}
