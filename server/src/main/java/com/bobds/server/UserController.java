package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://localhost:5173", "https://bobds.aguilucho.ar"}, allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, String> body, @RequestAttribute("authenticatedEmail") String email) {
        String newUsername = body.get("newUsername");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        String result = userService.updateUsername(email, newUsername);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Username updated successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> body, @RequestAttribute("authenticatedEmail") String email) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current and new passwords are required"));
        }

        String result = userService.updatePassword(email, currentPassword, newPassword);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result));
        }
    }

    @PutMapping("/preferences/animations")
    public ResponseEntity<?> updateAnimationPreference(@RequestBody Map<String, Boolean> body, @RequestAttribute("authenticatedEmail") String email) {
        Boolean enabled = body.get("animationsEnabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "animationsEnabled is required"));
        }

        String result = userService.updateAnimationPreference(email, enabled);
        if ("OK".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Animation preferences updated"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result));
        }
    }
}
