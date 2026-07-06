package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "https://bobds.aguilucho.ar"}, allowCredentials = "true")
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

    private boolean checkAdmin(String email) {
        User u = userService.getUserByEmail(email);
        return u != null && u.isAdmin();
    }

    @GetMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamAdminEvents(@RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return null;
        return sseService.createEmitter("ADMIN_CHANNEL");
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogDetailDTO>> getAllLogs(@RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(logService.getAllLogsDetailed());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(@RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUser(@PathVariable int userId, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        User u = userService.getUserById(userId);
        if (u != null) return ResponseEntity.ok(u);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/units")
    public ResponseEntity<List<Unit>> getSystemUnits(@RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(unitService.getSystemUnits());
    }

    @GetMapping("/units/{unitId}")
    public ResponseEntity<Unit> getUnit(@PathVariable String unitId, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Unit u = unitService.getUnitById(unitId);
        if (u != null) return ResponseEntity.ok(u);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/users/{userEmail}/units")
    public ResponseEntity<List<UnitListDTO>> getUserUnits(@PathVariable String userEmail, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(unitService.getUnitsByUser(userEmail));
    }

    @GetMapping("/units/{unitId}/orders")
    public ResponseEntity<List<Order>> getUnitOrders(@PathVariable String unitId, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(orderService.getOrdersByUnit(unitId, "ADMIN_OVERRIDE"));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getSystemOrders(@RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(orderService.getSystemOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable int orderId, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Order o = orderService.getOrderById(orderId);
        if (o != null) return ResponseEntity.ok(o);
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable int userId, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String result = userService.deleteUser(userId, email);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result));
    }

    @DeleteMapping("/units/{unitId}")
    public ResponseEntity<Map<String, String>> deleteUnit(@PathVariable String unitId, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String result = unitService.forceDeleteUnit(unitId, email);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Unidad eliminada"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable int orderId, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String result = orderService.forceDeleteOrder(orderId, email);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Orden eliminada"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Map<String, String>> cancelOrder(@PathVariable int orderId, @RequestAttribute("authenticatedEmail") String email) {
        if (!checkAdmin(email)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        String result = orderService.cancelOrder(orderId, email);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Orden cancelada"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result));
    }
}
