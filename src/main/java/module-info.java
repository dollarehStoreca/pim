module my.module {
    requires org.jsoup;
    requires com.fasterxml.jackson.databind;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires com.opencsv;
    requires org.slf4j;

    requires java.net.http;
    requires com.google.common;
    requires jakarta.validation;

    opens ca.dollareh.core.model;

    exports ca.dollareh.core.model;
}