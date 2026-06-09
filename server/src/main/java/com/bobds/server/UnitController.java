package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/units")
public class UnitController {
    
    @Autowired
    private UnitService unitService;

    @GetMapping("/user")
    public ResponseEntity<List<UnitListDTO>> getUserUnits(@RequestAttribute("authenticatedEmail") String email) {
        List<UnitListDTO> units = unitService.getUnitsByUser(email);
        return ResponseEntity.ok(units);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUnit(@RequestBody RegisterUnitDTO datos, @RequestAttribute("authenticatedEmail") String email) {
        datos.setUserEmail(email);
        String result = unitService.registerUnit(datos);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unit registered successfully");
    }

    @DeleteMapping("/DeleteUnit/{idUnidad}")
    public ResponseEntity<String> deleteUnit(@PathVariable String idUnidad) {
        String result = unitService.deleteUnit(idUnidad);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unit deleted successfully");
    }

    @GetMapping("/info/{idUnidad}")
    public ResponseEntity<UnitInfoDTO> getUnitInfo(@PathVariable String idUnidad) {
        UnitInfoDTO info = unitService.getUnitInfo(idUnidad);
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
        return ResponseEntity.badRequest().body("Error generating code");
    }

    @GetMapping("/code-info/{code}")
    public ResponseEntity<?> getUnitInfoByCode(@PathVariable String code, @RequestAttribute("authenticatedEmail") String email) {
        try {
            UnitInfoDTO info = unitService.getUnitInfoByCode(code, email);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/link")
    public ResponseEntity<String> linkUnit(@RequestBody java.util.Map<String, String> payload, @RequestAttribute("authenticatedEmail") String email) {
        String code = payload.get("code");
        String result = unitService.linkUserToUnit(email, code);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unit linked successfully");
    }

    @DeleteMapping("/unlink/{idUnidad}")
    public ResponseEntity<String> unlinkUnit(@PathVariable String idUnidad, @RequestAttribute("authenticatedEmail") String email) {
        String result = unitService.unlinkUserFromUnit(email, idUnidad);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unit unlinked successfully");
    }

    @PutMapping("/update/{idUnidad}")
    public ResponseEntity<String> updateUnit(@PathVariable String idUnidad, @RequestBody java.util.Map<String, String> payload) {
        String nombre = payload.get("nombre");
        String descripcion = payload.get("descripcion");
        String result = unitService.modifyUnit(idUnidad, nombre, descripcion);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unit modified successfully");
    }
}
