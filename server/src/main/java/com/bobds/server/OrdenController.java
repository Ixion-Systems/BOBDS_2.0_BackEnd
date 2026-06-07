package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
     * Endpoint to get a specific order by id
     * @param idOrden The order ID
     * @return The order
     */
    @GetMapping("/{idOrden}")
    public ResponseEntity<Orden> obtenerOrdenPorId(@PathVariable int idOrden) {
        List<Orden> ordenes = ordenService.cargarOrdenes();
        return ordenes.stream()
                .filter(o -> o.getIdOrden() == idOrden)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint to get all orders for a specific unit
     * @param idUnidad The unit ID
     * @return List of orders
     */
    @GetMapping("/unit/{idUnidad}")
    public ResponseEntity<List<Orden>> obtenerOrdenesPorUnidad(@PathVariable String idUnidad) {
        List<Orden> ordenes = ordenService.obtenerOrdenesPorUnidad(idUnidad);
        return ResponseEntity.ok(ordenes);
    }
    /**
     * Endpoint to delete an order
     * @param idOrden The order ID
     * @return Result message
     */
    @DeleteMapping("/{idOrden}")
    public ResponseEntity<String> deleteOrder(@PathVariable int idOrden) {
        String result = ordenService.eliminarOrden(idOrden);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Orden eliminada exitosamente");
    }
}