package com.stagllc.staginfra.dto;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CostRequestTest {

    @Test
    void testCostRequest_GettersAndSetters() {
        CostRequest request = new CostRequest();
        assertNull(request.getComponents());

        List<Map<String, Object>> components = Collections.singletonList(new HashMap<>());
        request.setComponents(components);
        assertEquals(components, request.getComponents());
    }
}