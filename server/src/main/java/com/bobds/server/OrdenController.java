package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;

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

    /**
     * Endpoint to get all orders with their id and estado
     * @return List of orders
     */
    @GetMapping
    public ResponseEntity<List<Orden>> obtenerTodasLasOrdenes() {
        try {
            List<Orden> ordenes = ordenService.cargarOrdenes();
            return ResponseEntity.ok(ordenes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint to get a specific order by id
     * @param idOrden The order ID
     * @return The order
     */
    @GetMapping("/{idOrden}")
    public ResponseEntity<Orden> obtenerOrdenPorId(@PathVariable int idOrden) {
        try {
            List<Orden> ordenes = ordenService.cargarOrdenes();
            return ordenes.stream()
                    .filter(o -> o.getIdOrden() == idOrden)
                    .findFirst()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}