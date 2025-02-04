package com.techatpark.practices;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class MultiCraftTest {

    @Test
    void testGetCategories() throws IOException {
        MultiCraft multiCraft = new MultiCraft();
        System.out.println(multiCraft.getCategories());
    }

}