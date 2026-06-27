package com.aftersales.platform.business.controller;

import com.aftersales.platform.business.service.OrderService;
import com.aftersales.platform.business.service.ProductService;
import com.aftersales.platform.business.service.TicketService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessController {
    private final OrderService orders; private final ProductService products; private final TicketService tickets;
    public BusinessController(OrderService orders, ProductService products, TicketService tickets) { this.orders = orders; this.products = products; this.tickets = tickets; }
    @GetMapping("/orders/{id}") public Object order(@PathVariable Long id) { return orders.getOrder(id); }
    @GetMapping("/users/{userId}/orders/latest") public Object latest(@PathVariable Long userId) { return orders.latestOrder(userId); }
    @GetMapping("/products/{id}") public Object product(@PathVariable Long id) { return products.getProduct(id); }
    @GetMapping("/products/search") public Object search(@RequestParam String keyword) { return products.search(keyword); }
    @PostMapping("/products/index/rebuild") public Object rebuild() { products.rebuildIndex(); return Map.of("message", "ok"); }
    @GetMapping("/tickets/{id}") public Object ticket(@PathVariable Long id) { return tickets.get(id); }
    @GetMapping("/orders/{orderId}/tickets") public Object orderTickets(@PathVariable Long orderId) { return tickets.byOrder(orderId); }
}
