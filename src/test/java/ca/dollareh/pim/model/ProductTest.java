package ca.dollareh.pim.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void merge() {
        Product original = new Product("Code1",
        "Title1",
        "Description1",
                1000L,
        10,
        1.0f,
        1.0f,
        0.5f,
        null);

        Product transform = new Product(null,
                "Title2",
                "Description2",
                1000L,
                10,
                null,
                2.0f,
                0.5f,
                null);

        Product merged = original.merge(transform);

        Assertions.assertEquals("Code1", merged.code() );
        Assertions.assertEquals("Title2", merged.title() );

        Assertions.assertEquals(2.0f, merged.price());

    }
}