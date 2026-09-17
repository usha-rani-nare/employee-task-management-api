package com.usharani.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Just a context load test for now - confirms the app wires up correctly
// (beans, JPA config, etc.) which is enough to catch most CI-breaking issues.
// Would add proper service-layer unit tests with a real test DB if I had
// more time.
@SpringBootTest
class TaskManagerApplicationTests {

    @Test
    void contextLoads() {
    }

}
