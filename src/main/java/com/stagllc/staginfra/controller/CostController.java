package com.stagllc.staginfra.controller;

import com.stagllc.staginfra.dto.CostRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CostController {
    private List<Map<String, Object>> components = List.of();

    @GetMapping("/cost")
    public ResponseEntity<Map<String, Double>> getCost() {
        double total = 0.0;
        for (Map<String, Object> comp : components) {
            String type = (String) comp.get("type");
            if ("ec2".equals(type)) {
                int instances = (int) comp.getOrDefault("instances", 1);
                String instanceType = (String) comp.getOrDefault("instance_type", "t2.micro");
                // Add instance type based pricing
                double basePrice = getEC2Price(instanceType);
                total += basePrice * instances;
            } else if ("s3".equals(type)) {
                int storage = (int) comp.getOrDefault("storage", 10);
                total += 0.023 * storage;
            } else if ("lambda".equals(type)) {
                int memory = (int) comp.getOrDefault("memory", 128);
                // $0.0000166667 per GB-second, estimated for 100,000 invocations/month
                double memoryGB = memory / 1024.0;
                double invocations = 100000;
                double avgDuration = 0.5; // 500ms
                total += 0.0000166667 * memoryGB * avgDuration * invocations;
            } else if ("rds".equals(type)) {
                String instanceClass = (String) comp.getOrDefault("instance_class", "db.t2.micro");
                int storage = (int) comp.getOrDefault("allocated_storage", 20);
                boolean multiAZ = (boolean) comp.getOrDefault("multi_az", false);

                double instancePrice = getRDSPrice(instanceClass);
                double storagePrice = 0.115 * storage; // $0.115 per GB-month

                total += (instancePrice * (multiAZ ? 2 : 1)) + storagePrice;
            } else if ("dynamodb".equals(type)) {
                String billingMode = (String) comp.getOrDefault("billing_mode", "PROVISIONED");

                if ("PAY_PER_REQUEST".equals(billingMode)) {
                    // Assume 1M reads, 0.5M writes per month
                    total += 0.25 * 1 + 1.25 * 0.5; // $0.25 per 1M reads, $1.25 per 1M writes
                } else {
                    int readCapacity = (int) comp.getOrDefault("read_capacity", 5);
                    int writeCapacity = (int) comp.getOrDefault("write_capacity", 5);
                    total += (0.00013 * readCapacity + 0.00065 * writeCapacity) * 730; // Cost per hour * hours per month
                }
            } else if ("ebs".equals(type)) {
                int size = (int) comp.getOrDefault("size", 20);
                String volumeType = (String) comp.getOrDefault("volume_type", "gp2");

                double gbMonthPrice = getEBSPrice(volumeType);
                total += size * gbMonthPrice;

                if ("io1".equals(volumeType)) {
                    int iops = (int) comp.getOrDefault("iops", 100);
                    total += iops * 0.065; // $0.065 per provisioned IOPS-month
                }
            } else if ("loadBalancer".equals(type)) {
                String lbType = (String) comp.getOrDefault("lb_type", "application");

                if ("application".equals(lbType)) {
                    total += 0.0225 * 730 + 0.008 * 3 * 730; // $0.0225 per hour + $0.008 per LCU-hour (assuming 3 LCUs)
                } else if ("network".equals(lbType)) {
                    total += 0.0225 * 730 + 0.006 * 3 * 730; // $0.0225 per hour + $0.006 per LCU-hour (assuming 3 LCUs)
                } else {
                    total += 0.025 * 730; // $0.025 per hour for classic ELB
                }
            }
            // VPC, subnet, security group components have no cost
        }
        total = Math.round(total * 100.0) / 100.0;
        return ResponseEntity.ok(Map.of("total", total));
    }

    @PostMapping("/cost")
    public ResponseEntity<Void> updateCost(@RequestBody CostRequest request) {
        this.components = request.getComponents() != null ? request.getComponents() : List.of();
        return ResponseEntity.ok().build();
    }

    // Helper methods for pricing
    private double getEC2Price(String instanceType) {
        return switch (instanceType) {
            case "t2.nano" -> 5.0;
            case "t2.micro" -> 8.5;
            case "t2.small" -> 17.0;
            case "t2.medium" -> 34.0;
            case "t2.large" -> 68.0;
            default -> 8.5; // Default to t2.micro pricing
        };
    }

    private double getRDSPrice(String instanceClass) {
        return switch (instanceClass) {
            case "db.t2.micro" -> 12.41;
            case "db.t2.small" -> 24.82;
            case "db.t2.medium" -> 49.64;
            case "db.m5.large" -> 138.7;
            default -> 12.41; // Default to db.t2.micro pricing
        };
    }

    private double getEBSPrice(String volumeType) {
        return switch (volumeType) {
            case "gp2" -> 0.10;
            case "gp3" -> 0.08;
            case "io1" -> 0.125;
            case "st1" -> 0.045;
            case "sc1" -> 0.025;
            default -> 0.10; // Default to gp2 pricing
        };
    }
}