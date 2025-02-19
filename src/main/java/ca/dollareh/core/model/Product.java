package ca.dollareh.core.model;

public record Product(Category category,
                      String code,
                      String title,
                      String description,
                      float price,
                      float discount,
                      String[] imageUrls) {
    @Override
    public String toString() {
        return code;
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
