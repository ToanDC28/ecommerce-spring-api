package com.ecommerce.sportcenter.config;

import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import com.ecommerce.sportcenter.module.role.entity.Permission;
import com.ecommerce.sportcenter.module.role.entity.Role;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.role.repository.PermissionRepository;
import com.ecommerce.sportcenter.module.role.repository.RoleRepository;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds RBAC permissions + roles and an initial admin user on startup.
 * Internal app: ADMIN (chủ) + staff (SALES_STAFF, WAREHOUSE_STAFF, ACCOUNTANT).
 * Suppliers are NOT users — see Supplier domain.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedRolesAndAdmin(PermissionRepository permissionRepository,
                                        RoleRepository roleRepository,
                                        UserRepository userRepository,
                                        WarehouseRepository warehouseRepository,
                                        PasswordEncoder passwordEncoder) {
        return args -> {
            Map<String, String> permissionDescriptions = Map.ofEntries(
                    Map.entry("PRODUCT_READ", "View catalog"),
                    Map.entry("PRODUCT_WRITE", "Manage catalog"),
                    Map.entry("INVENTORY_READ", "View inventory"),
                    Map.entry("INVENTORY_WRITE", "Manage inventory"),
                    Map.entry("SUPPLIER_READ", "View suppliers"),
                    Map.entry("SUPPLIER_WRITE", "Manage suppliers"),
                    Map.entry("ORDER_READ", "View orders"),
                    Map.entry("ORDER_WRITE", "Manage orders"),
                    Map.entry("CUSTOMER_READ", "View customers"),
                    Map.entry("CUSTOMER_WRITE", "Manage customers"),
                    Map.entry("INVOICE_READ", "View invoices"),
                    Map.entry("INVOICE_WRITE", "Manage invoices"),
                    Map.entry("PAYMENT_MANAGE", "Manage payments"),
                    Map.entry("PAYROLL_READ", "View payroll"),
                    Map.entry("PAYROLL_WRITE", "Manage payroll"),
                    Map.entry("USER_READ", "View users and roles"),
                    Map.entry("USER_WRITE", "Manage users"),
                    Map.entry("ROLE_MANAGE", "Manage roles")
            );

            // 1. Seed permissions table.
            Map<String, Permission> permissions = permissionDescriptions.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> permissionRepository.findByName(e.getKey()).orElseGet(() ->
                                    permissionRepository.save(Permission.builder()
                                            .name(e.getKey())
                                            .description(e.getValue())
                                            .build()))));

            // 2. Seed roles: chủ (ADMIN, full quyền) + staff (no MANAGER — tiệm nhỏ).
            Map<String, Set<String>> defaults = Map.of(
                    "ADMIN", new HashSet<>(permissionDescriptions.keySet()),
                    "SALES_STAFF", Set.of(
                            "PRODUCT_READ",
                            "ORDER_READ", "ORDER_WRITE",
                            "CUSTOMER_READ", "CUSTOMER_WRITE",
                            "INVENTORY_READ", "INVENTORY_WRITE",
                            "INVOICE_READ", "INVOICE_WRITE"),
                    "WAREHOUSE_STAFF", Set.of(
                            "PRODUCT_READ",
                            "INVENTORY_READ", "INVENTORY_WRITE",
                            "SUPPLIER_READ",
                            "ORDER_READ"),
                    "ACCOUNTANT", Set.of(
                            "INVOICE_READ", "INVOICE_WRITE",
                            "PAYMENT_MANAGE",
                            "PAYROLL_READ", "PAYROLL_WRITE",
                            "CUSTOMER_READ",
                            "ORDER_READ", "SUPPLIER_READ")
            );

            defaults.forEach((name, permissionNames) -> {
                Role role = roleRepository.findByName(name).orElseGet(() ->
                        roleRepository.save(Role.builder()
                                .name(name)
                                .description("Default role: " + name)
                                .permissions(new HashSet<>())
                                .build()));
                Set<Permission> resolved = permissionNames.stream()
                        .map(pn -> {
                            Permission p = permissions.get(pn);
                            if (p == null) {
                                throw new IllegalStateException("Permission not seeded: " + pn);
                            }
                            return p;
                        })
                        .collect(Collectors.toSet());
                if (!resolved.equals(role.getPermissions())) {
                    role.setPermissions(new HashSet<>(resolved));
                    roleRepository.save(role);
                }
            });

            // 3. Default admin: username=admin / password=Admin@123 (change after first login)
            if (!userRepository.existsByUsername("admin")) {
                Role adminRole = roleRepository.findByName("ADMIN")
                        .orElseThrow(() -> new IllegalStateException("ADMIN role not seeded"));
                User admin = User.builder()
                        .username("admin")
                        .email("admin@shop.local")
                        .password(passwordEncoder.encode("Admin@123"))
                        .fullName("System Administrator")
                        .enabled(true)
                        .roles(new HashSet<>(Set.of(adminRole)))
                        .build();
                userRepository.save(admin);
            }

            // 4. Default warehouse (tiệm nhỏ 1 kho): GRN/GIN/quick-sale cần warehouseId sẵn.
            if (!warehouseRepository.existsByCode("WH-XUONG-01")) {
                warehouseRepository.save(Warehouse.builder()
                        .code("WH-XUONG-01")
                        .name("Kho chính")
                        .address("Xưởng")
                        .active(true)
                        .build());
            }
        };
    }
}
