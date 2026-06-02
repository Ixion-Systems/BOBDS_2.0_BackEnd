package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/orders")
public class OrdenController {
    
    @Autowired
    private OrdenService ordenService;

    @PostMapping("/register")
    public ResponseEntity<String> registerOrder(@RequestBody RegistroOrdenDTO datos) {
        String result = ordenService.registrarOrden(datos);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Orden transmitida y registrada exitosamente");
    }
}
