package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/orders")
public class OrdenController {

    @Autowired
    private OrdenService ordenService;

    @Autowired
    private UnitService unitService;

    /**
     * Endpoint to get all orders with their id and estado
     * @return List of orders
     */
    @GetMapping
    public ResponseEntity<List<Orden>> obtenerTodasLasOrdenes() {
        List<Orden> ordenes = ordenService.obtenerTodasLasOrdenes();
        return ResponseEntity.ok(ordenes);
    }

    /**
     * Endpoint to get a specific order by id
     * @param idOrden The order ID
     * @return The order
     */
    @GetMapping("/{idOrden}")
    public ResponseEntity<Orden> obtenerOrdenPorId(@PathVariable int idOrden) {
        List<Orden> ordenes = ordenService.obtenerTodasLasOrdenes();
        return ordenes.stream()
                .filter(o -> o.getIdOrden() == idOrden)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}