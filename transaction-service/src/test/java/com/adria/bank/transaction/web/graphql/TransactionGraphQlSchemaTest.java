package com.adria.bank.transaction.web.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.graphql.execution.GraphQlSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TransactionGraphQlSchemaTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void applicationContextShouldStartWithBigDecimalGraphQlScalar() {
        assertNotNull(context.getBean(GraphQlSource.class));
    }
}
