package ca.dollareh.core.model;

public record Product(String code,
                      String title,
                      String description,
                      Long upc,
                      Integer inventryQuantity,
                      Float price,
                      Float discount,
                      String[] imageUrls) {

    public Product merge(final Product product1) {
        return new Product(
                product1.code == null ? code : product1.code,
                product1.title == null ? title : product1.title,
                product1.description == null ? description : product1.description,
                product1.upc == null ? upc : product1.upc,
                product1.inventryQuantity == null ? inventryQuantity : product1.inventryQuantity,
                product1.price == null ? price : product1.price,
                product1.discount == null ? discount : product1.discount,
                product1.imageUrls == null ? imageUrls : product1.imageUrls

        );
    }
}
