package ca.dollareh.core.model;

import java.util.List;

public record Category(Category parent, String code, List<Category> categories, List<Product> products) {
    
}
