package com.renko.service.impl;

import com.renko.client.PeerServiceClient;
import com.renko.configuration.JwtProvider;
import com.renko.domain.UserRole;
import com.renko.entities.UserEntity;
import com.renko.payload.dto.UserDto;
import com.renko.repository.UserRepository;
import com.renko.service.DemoSeedService;
import com.renko.service.DevCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class DemoSeedServiceImpl implements DemoSeedService
{
    public static final String OWNER_EMAIL = "owner@renko.demo";
    public static final String CASHIER_EMAIL = "cashier@renko.demo";
    public static final String MANAGER_EMAIL = "manager@renko.demo";
    public static final String DEMO_PASSWORD = "Demo1234!";

    private final DevCleanupService devCleanupService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PeerServiceClient peers;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public Map<String, Object> demoInfo()
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("password", DEMO_PASSWORD);
        body.put("accounts", List.of(
                Map.of("role", "CASHIER", "email", CASHIER_EMAIL, "path", "/pos"),
                Map.of("role", "OWNER", "email", OWNER_EMAIL, "path", "/admin"),
                Map.of("role", "STORE_MANAGER", "email", MANAGER_EMAIL, "path", "/admin")
        ));
        body.put("hint", "Use Reset demo on the landing page to wipe each service DB and recreate this dataset.");
        return body;
    }

    @Override
    public Map<String, Object> resetAndSeed()
    {
        List<String> truncated = new ArrayList<>(devCleanupService.clearAllTables());
        peers.clearAllPeers();

        UserEntity owner = saveUser("Demo Owner", OWNER_EMAIL, "0888000100", UserRole.OWNER, null);
        String ownerJwt = jwtFor(owner);

        Map<String, Object> storeBody = Map.of(
                "brandName", "Renko Demo Market",
                "description", "Demo store with sample catalog, branches, and staff.",
                "storeType", "RETAIL",
                "contact", Map.of("email", OWNER_EMAIL, "phone", "0888000100", "address", "100 Market Street")
        );
        Map<String, Object> store = peers.post(peers.store(), "/api/stores", storeBody, bearer(ownerJwt), Map.class);
        Long storeId = ((Number) store.get("id")).longValue();

        owner.setStoreId(storeId);
        userRepository.save(owner);
        ownerJwt = jwtFor(owner);

        // Activate subscription via billing (store create already tries trial)
        try
        {
            peers.post(peers.billing(), "/api/billing/subscription/" + storeId + "/trial?plan=STARTER",
                    Map.of(), bearer(ownerJwt), Map.class);
        }
        catch(Exception ignored) {}

        List<Long> branchIds = new ArrayList<>();
        for(String[] b : new String[][]{
                {"Main Floor", "100 Market Street", "0888000201"},
                {"Cafe Corner", "100 Market Street — Cafe", "0888000202"},
                {"Warehouse Pickup", "12 Dock Road", "0888000203"}
        })
        {
            Map<String, Object> branch = peers.post(peers.store(), "/api/branches", Map.of(
                    "name", b[0], "address", b[1], "phone", b[2],
                    "email", b[0].toLowerCase().replace(' ', '.') + "@renko.demo",
                    "storeId", storeId,
                    "workdays", List.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"),
                    "openTime", "09:00:00",
                    "closeTime", "21:00:00"
            ), bearer(ownerJwt), Map.class);
            branchIds.add(((Number) branch.get("id")).longValue());
        }

        List<Long> categoryIds = new ArrayList<>();
        for(String name : List.of("Beverages", "Snacks", "Merchandise"))
        {
            Map<String, Object> cat = peers.post(peers.catalog(), "/api/categories", Map.of(
                    "name", name, "storeId", storeId
            ), bearer(ownerJwt), Map.class);
            categoryIds.add(((Number) cat.get("id")).longValue());
        }

        Object[][] products = {
                {"House Espresso", "DEMO-ESP", "Renko", 12.5, 9.99, 80, 10, categoryIds.get(0)},
                {"Cold Brew", "DEMO-CBR", "Renko", 10.0, 7.5, 60, 8, categoryIds.get(0)},
                {"Trail Mix", "DEMO-TRM", "TrailCo", 7.0, 5.25, 45, 5, categoryIds.get(1)},
                {"Branded Tote", "DEMO-TOT", "Renko", 24.0, 19.0, 14, 15, categoryIds.get(2)},
        };
        List<Long> productIds = new ArrayList<>();
        List<Long> inventoryIds = new ArrayList<>();
        for(Object[] p : products)
        {
            Map<String, Object> prod = peers.post(peers.catalog(), "/api/products", Map.of(
                    "name", p[0], "sku", p[1], "brand", p[2],
                    "maxRetailPrice", p[3], "sellingPrice", p[4],
                    "description", "Demo product — " + p[0],
                    "discountPercentage", 0.0,
                    "storeId", storeId,
                    "categoryId", p[7],
                    "imageUrl", ""
            ), bearer(ownerJwt), Map.class);
            Long productId = ((Number) prod.get("id")).longValue();
            productIds.add(productId);
            Map<String, Object> inv = peers.post(peers.catalog(), "/api/inventories", Map.of(
                    "storeId", storeId, "productId", productId,
                    "quantity", p[5], "lowStockThreshold", p[6]
            ), bearer(ownerJwt), Map.class);
            inventoryIds.add(((Number) inv.get("id")).longValue());
        }

        List<Long> customerIds = new ArrayList<>();
        for(String[] c : new String[][]{
                {"Walk-in Guest", "guest@renko.demo", "0888111001"},
                {"Ada Lovelace", "ada@renko.demo", "0888111002"},
                {"Alan Turing", "alan@renko.demo", "0888111003"}
        })
        {
            Map<String, Object> cust = peers.post(peers.sales(), "/api/customers", Map.of(
                    "fullName", c[0], "email", c[1], "phone", c[2], "storeId", storeId
            ), bearer(ownerJwt), Map.class);
            customerIds.add(((Number) cust.get("id")).longValue());
        }

        UserEntity cashier = saveUser("Demo Cashier", CASHIER_EMAIL, "0888000300", UserRole.CASHIER, storeId);
        UserEntity manager = saveUser("Demo Manager", MANAGER_EMAIL, "0888000400", UserRole.STORE_MANAGER, storeId);
        String cashierJwt = jwtFor(cashier);

        List<Long> orderIds = new ArrayList<>();
        String[] payments = {"CASH", "CARD", "UPI"};
        for(int i = 0; i < 7; i++)
        {
            Long productId = productIds.get(i % productIds.size());
            Long branchId = branchIds.get(i % branchIds.size());
            Long customerId = customerIds.get(i % customerIds.size());
            int qty = new int[]{2,1,3,1,2,1,2}[i];
            Map<String, Object> orderReq = new LinkedHashMap<>();
            orderReq.put("branchId", branchId);
            orderReq.put("customerId", customerId);
            orderReq.put("paymentType", payments[i % payments.length]);
            orderReq.put("taxRate", i % 2 == 0 ? 8.0 : 0.0);
            orderReq.put("orderDiscountPercent", i == 2 ? 5.0 : 0.0);
            if("CARD".equals(payments[i % payments.length]))
            {
                orderReq.put("stripePaymentIntentId", "demo_seed_" + i);
            }
            orderReq.put("items", List.of(Map.of("productId", productId, "quantity", qty)));
            try
            {
                Map<String, Object> order = peers.post(peers.sales(), "/api/orders", orderReq, bearer(cashierJwt), Map.class);
                if(order != null && order.get("id") != null)
                {
                    orderIds.add(((Number) order.get("id")).longValue());
                }
            }
            catch(Exception e)
            {
                System.out.println("Demo order seed failed: " + e.getMessage());
            }
        }

        try
        {
            peers.post(peers.report(), "/api/audit-logs", Map.of(
                    "storeId", storeId,
                    "action", "PRODUCT_UPDATE",
                    "entityType", "Product",
                    "entityId", String.valueOf(productIds.get(0)),
                    "details", "Demo seed sample price change",
                    "beforeState", "name=House Espresso; sellingPrice=9.99",
                    "afterState", "name=House Espresso; sellingPrice=9.49"
            ), bearer(ownerJwt), Map.class);
        }
        catch(Exception ignored) {}

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Demo dataset reset and seeded across services");
        body.put("truncatedTables", truncated.size());
        body.put("storeId", storeId);
        body.put("branchIds", branchIds);
        body.put("productIds", productIds);
        body.put("inventoryIds", inventoryIds);
        body.put("customerIds", customerIds);
        body.put("orderIds", orderIds);
        body.put("password", DEMO_PASSWORD);
        body.put("accounts", List.of(
                Map.of("role", "CASHIER", "email", CASHIER_EMAIL, "userId", cashier.getId()),
                Map.of("role", "OWNER", "email", OWNER_EMAIL, "userId", owner.getId()),
                Map.of("role", "STORE_MANAGER", "email", MANAGER_EMAIL, "userId", manager.getId())
        ));
        return body;
    }

    private UserEntity saveUser(String name, String email, String phone, UserRole role, Long storeId)
    {
        UserEntity user = new UserEntity();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setRole(role);
        user.setStoreId(storeId);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private String jwtFor(UserEntity user)
    {
        var details = customUserDetailsService.loadUserByUsername(user.getEmail());
        var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        return jwtProvider.generateToken(auth, user.getId(), user.getStoreId());
    }

    private String bearer(String jwt)
    {
        return jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;
    }
}
