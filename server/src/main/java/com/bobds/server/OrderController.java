package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/orders")
/* controlador de ordenes */
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UnitService unitService;

    /* metodos de autorizacion interna */
    private boolean isUserAuthorizedForUnit(String email, String unitId) {
        return unitService.getUnitsByUser(email).stream()
                .anyMatch(u -> u.getUnitId().equals(unitId));
    }

    /* creacion y transmision de ordenes */
    @PostMapping("/register")
    public ResponseEntity<String> registerOrder(@RequestBody RegisterOrderDTO data, @RequestAttribute("authenticatedEmail") String email) {
        if (!isUserAuthorizedForUnit(email, data.getUnitId())) {
            return ResponseEntity.status(403).body("Error: No autorizado para esta unidad");
        }
        String result = orderService.registerOrder(data, email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Order transmitted and registered successfully");
    }

    /* actualizacion de estado del hardware */
    @PostMapping("/status")
    public ResponseEntity<String> updateOrderStatus(@RequestParam int idOrden, @RequestParam String status) {

        String result = orderService.changeOrderStatusById(idOrden, status);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Status updated to " + status);
    }

    /* consultas de historial global */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(@RequestAttribute("authenticatedEmail") String email) {

        List<String> authorizedUnits = unitService.getUnitsByUser(email).stream()
            .map(UnitListDTO::getUnitId).toList();

        List<Order> orders = orderService.getAllOrders(email).stream()

            .filter(o -> {

                return true; 
            }).toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable int orderId, @RequestAttribute("authenticatedEmail") String email) {
        List<Order> orders = orderService.getAllOrders(email);
        return orders.stream()
            .filter(o -> o.getOrderId() == orderId)
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /* consultas filtradas por unidad */
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<List<Order>> getOrdersByUnit(@PathVariable String unitId, @RequestAttribute("authenticatedEmail") String email) {
        if (!isUserAuthorizedForUnit(email, unitId)) {
            return ResponseEntity.status(403).build();
        }
        List<Order> orders = orderService.getOrdersByUnit(unitId, email);
        return ResponseEntity.ok(orders);
    }

    /* eliminacion de historial */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<String> deleteOrder(@PathVariable int orderId, @RequestAttribute("authenticatedEmail") String email) {

        String result = orderService.deleteOrder(orderId, email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Order deleted successfully");
    }

    /* cancelacion de ordenes en curso */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable int orderId, @RequestAttribute("authenticatedEmail") String email) {
        String result = orderService.cancelOrder(orderId, email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Order cancelled successfully");
    }
}
