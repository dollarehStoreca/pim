package ca.dollareh.core.model;

import java.util.List;

public record Category(Category parent, String code, List<Category> categories, List<Product> products) {
    @Override
    public String toString() {
        return code;
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
