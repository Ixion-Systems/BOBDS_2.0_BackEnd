package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController

@RequestMapping("/api/auth")

public class AuthController {
    public AuthController() {
        System.out.println("CONTROLLER CARGADO");
    }
    @Autowired
    private UsuarioEntradaService usuarioEntrada;
    @GetMapping("/test")
    public String test(){

    System.out.println("ENTRO");

    return "Servidor funcionando";

}

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestParam String nombre,@RequestParam String password, @RequestParam String email){

        String result =usuarioEntrada.signUp(nombre,password,email);
        if(result.startsWith("Error")){
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String email,@RequestParam String password){

        String result =usuarioEntrada.login(email,password);
        if(result.startsWith("Error")){
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}