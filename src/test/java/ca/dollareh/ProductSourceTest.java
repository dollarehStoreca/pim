package ca.dollareh;

import ca.dollareh.vendor.MultiCraft;
import ca.dollareh.vendor.ProductSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

class ProductSourceTest {

    @Test
    void testMultiCraft() throws IOException, URISyntaxException {
        ProductSource productSource = ProductSource
                .from(MultiCraft.class)
                        .onNew(newProduct -> {
                            System.out.println("New Product Found " + newProduct);
                        })
                        .onModified(updatedProduct -> {
                            System.out.println("Product Modified " + updatedProduct);
                        })
                .build();

         productSource.browse();


      //  productSource.enrich();

    }

}