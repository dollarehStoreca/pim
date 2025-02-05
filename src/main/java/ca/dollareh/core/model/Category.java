package ca.dollareh.core.model;

import java.util.List;

public record Category(String code, List<Category> categories, List<Product> products) {
    
}
