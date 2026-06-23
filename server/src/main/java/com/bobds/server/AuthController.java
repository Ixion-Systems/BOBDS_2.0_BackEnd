package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://localhost:5173", "https://bobds.aguilucho.ar"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
/* controlador de autenticacion */
public class AuthController {
    public AuthController() {
        System.out.println("CONTROLLER LOADED");
    }

    @Autowired
    private UserService userService;

    /* endpoints de prueba */
    @GetMapping("/test")
    public String test(){
        System.out.println("ENTERED");
        return "Server running";
    }

    /* endpoints de registro */
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestParam String nombre, @RequestParam String password, @RequestParam String email){
        String result = userService.signUp(nombre, password, email);
        if(result.startsWith("Error")){
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @Autowired
    private JwtUtil jwtUtil;

    /* endpoints de inicio de sesion */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password, jakarta.servlet.http.HttpServletResponse response){
        String result = userService.login(email, password);
        if(result.startsWith("Error")){
            return ResponseEntity.badRequest().body(result);
        }

        String token = jwtUtil.generateToken(email);
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400); 
        response.addCookie(cookie);

        return ResponseEntity.ok(result);
    }

    /* endpoints de cierre de sesion */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletResponse response) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); 
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out");
    }

    /* endpoints de verificacion */
    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String email, @RequestParam String token) {
        String result = userService.verifyEmail(email, token);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/resend")
    public ResponseEntity<String> resend(@RequestParam String email) {
        String result = userService.resendCode(email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /* endpoints de recuperacion de cuenta */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        String result = userService.requestPasswordReset(email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<String> verifyResetCode(@RequestParam String email, @RequestParam String token) {
        String result = userService.verifyResetCode(email, token);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String email, @RequestParam String token, @RequestParam String nuevaContrasena) {
        String result = userService.changePassword(email, token, nuevaContrasena);
        if (result.startsWith("Error") || result.startsWith("Validation Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
