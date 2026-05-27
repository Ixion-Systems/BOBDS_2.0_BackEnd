package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/units")
public class UnitController {
    
    @Autowired
    private UnitService unitService;

    @GetMapping("/user")
    public ResponseEntity<List<UnidadListadoDTO>> getUserUnits(@RequestParam String email) {
        List<UnidadListadoDTO> units = unitService.getUnidadesByUsuario(email);
        return ResponseEntity.ok(units);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUnit(@RequestBody RegistroUnidadDTO datos) {
        String result = unitService.registrarUnidad(datos);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unidad registrada exitosamente");
    }

    @DeleteMapping("/DeleteUnit/{idUnidad}")
    public ResponseEntity<String> deleteUnit(@PathVariable String idUnidad) {
        String result = unitService.eliminarUnidad(idUnidad);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unidad eliminada exitosamente");
    }
}
