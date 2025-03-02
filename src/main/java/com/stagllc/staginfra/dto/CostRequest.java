package com.stagllc.staginfra.dto;

import java.util.List;
import java.util.Map;

public class CostRequest {
    private List<Map<String, Object>> components;

    public List<Map<String, Object>> getComponents() {
        return components;
    }

    public void setComponents(List<Map<String, Object>> components) {
        this.components = components;
    }
}