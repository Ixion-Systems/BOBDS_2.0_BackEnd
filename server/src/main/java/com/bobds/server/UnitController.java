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

    @GetMapping("/info/{idUnidad}")
    public ResponseEntity<UnidadInfoDTO> getUnitInfo(@PathVariable String idUnidad) {
        UnidadInfoDTO info = unitService.getUnidadInfo(idUnidad);
        if (info != null) {
            return ResponseEntity.ok(info);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{idUnidad}/generate-code")
    public ResponseEntity<String> generateCode(@PathVariable String idUnidad) {
        String newCode = unitService.generateNewCode(idUnidad);
        if (newCode != null) {
            return ResponseEntity.ok(newCode);
        }
        return ResponseEntity.badRequest().body("Error al generar código");
    }

    @GetMapping("/code-info/{code}")
    public ResponseEntity<?> getUnitInfoByCode(@PathVariable String code) {
        try {
            UnidadInfoDTO info = unitService.getUnidadInfoByCode(code);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/link")
    public ResponseEntity<String> linkUnit(@RequestBody java.util.Map<String, String> payload) {
        String email = payload.get("email");
        String code = payload.get("code");
        String result = unitService.linkUserToUnit(email, code);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unidad vinculada exitosamente");
    }

    @DeleteMapping("/unlink/{idUnidad}")
    public ResponseEntity<String> unlinkUnit(@PathVariable String idUnidad, @RequestParam String email) {
        String result = unitService.unlinkUserFromUnit(email, idUnidad);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unidad desvinculada exitosamente");
    }

    @PutMapping("/update/{idUnidad}")
    public ResponseEntity<String> updateUnit(@PathVariable String idUnidad, @RequestBody java.util.Map<String, String> payload) {
        String nombre = payload.get("nombre");
        String descripcion = payload.get("descripcion");
        String result = unitService.modificarUnidad(idUnidad, nombre, descripcion);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unidad modificada exitosamente");
    }
}
