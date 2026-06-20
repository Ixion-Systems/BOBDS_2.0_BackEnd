package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") // Para desarrollo
public class AdminController {

    @Autowired
    private LogService logService;

    @Autowired
    private UserService userService;

    @Autowired
    private UnitService unitService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SseService sseService;

    @GetMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamAdminEvents() {
        return sseService.createEmitter("ADMIN_CHANNEL");
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogDetailDTO>> getAllLogs() {
        return ResponseEntity.ok(logService.getAllLogsDetailed());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUser(@PathVariable int userId) {
        User u = userService.getUserById(userId);
        if (u != null) return ResponseEntity.ok(u);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/units")
    public ResponseEntity<List<Unit>> getSystemUnits() {
        return ResponseEntity.ok(unitService.getSystemUnits());
    }

    @GetMapping("/units/{unitId}")
    public ResponseEntity<Unit> getUnit(@PathVariable String unitId) {
        Unit u = unitService.getUnitById(unitId);
        if (u != null) return ResponseEntity.ok(u);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/users/{email}/units")
    public ResponseEntity<List<UnitListDTO>> getUserUnits(@PathVariable String email) {
        return ResponseEntity.ok(unitService.getUnitsByUser(email));
    }

    @GetMapping("/units/{unitId}/orders")
    public ResponseEntity<List<Order>> getUnitOrders(@PathVariable String unitId) {
        // Obtenemos las órdenes de la unidad a través del servicio, pasando un email de admin
        return ResponseEntity.ok(orderService.getOrdersByUnit(unitId, "ADMIN_OVERRIDE"));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getSystemOrders() {
        return ResponseEntity.ok(orderService.getSystemOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable int orderId) {
        Order o = orderService.getOrderById(orderId);
        if (o != null) return ResponseEntity.ok(o);
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable int userId, @RequestParam String adminEmail) {
        String result = userService.deleteUser(userId, adminEmail);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result));
    }

    @DeleteMapping("/units/{unitId}")
    public ResponseEntity<Map<String, String>> deleteUnit(@PathVariable String unitId, @RequestParam String adminEmail) {
        // unitService.deleteUnit normally checks if the user deleting is the owner.
        // We will just call it with adminEmail. Wait, if adminEmail isn't owner, it might fail.
        // Let's create a forceDeleteUnit in UnitService or just use deleteUnit if it doesn't strictly check owner.
        // Looking at UnitService.deleteUnit, it checks if the link exists.
        // For admin, we should probably add forceDeleteUnit to UnitService.
        String result = unitService.forceDeleteUnit(unitId, adminEmail);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Unidad eliminada"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable int orderId, @RequestParam String adminEmail) {
        // Similarly, forceDeleteOrder
        String result = orderService.forceDeleteOrder(orderId, adminEmail);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Orden eliminada"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Map<String, String>> cancelOrder(@PathVariable int orderId, @RequestParam String adminEmail) {
        String result = orderService.cancelOrder(orderId, adminEmail);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Orden cancelada"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result));
    }
}
