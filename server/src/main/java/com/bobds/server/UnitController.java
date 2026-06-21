package com.bobds.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/units")
/* controlador de unidades */
public class UnitController {

    @Autowired
    private UnitService unitService;

    /* consulta de unidades de usuario */
    @GetMapping("/user")
    public ResponseEntity<List<UnitListDTO>> getUserUnits(@RequestAttribute("authenticatedEmail") String email) {
        List<UnitListDTO> units = unitService.getUnitsByUser(email);
        return ResponseEntity.ok(units);
    }

    /* registro de nuevas unidades */
    @PostMapping("/register")
    public ResponseEntity<String> registerUnit(@RequestBody RegisterUnitDTO datos, @RequestAttribute("authenticatedEmail") String email) {
        datos.setUserEmail(email);
        String result = unitService.registerUnit(datos);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unit registered successfully");
    }

    /* eliminacion de unidades */
    @DeleteMapping("/DeleteUnit/{idUnidad}")
    public ResponseEntity<String> deleteUnit(@PathVariable String idUnidad, @RequestAttribute("authenticatedEmail") String email) {
        String result = unitService.deleteUnit(idUnidad, email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unit deleted successfully");
    }

    /* consulta de informacion especifica */
    @GetMapping("/info/{idUnidad}")
    public ResponseEntity<UnitInfoDTO> getUnitInfo(@PathVariable String idUnidad, @RequestAttribute("authenticatedEmail") String email) {
        UnitInfoDTO info = unitService.getUnitInfo(idUnidad, email);
        if (info != null) {
            return ResponseEntity.ok(info);
        }
        return ResponseEntity.notFound().build();
    }

    /* generacion de codigos de vinculacion */
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

    /* vinculacion de unidades */
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

    /* modificacion de unidades */
    @PutMapping("/update/{idUnidad}")
    public ResponseEntity<String> updateUnit(@PathVariable String idUnidad, @RequestBody java.util.Map<String, String> payload, @RequestAttribute("authenticatedEmail") String email) {
        String nombre = payload.get("nombre");
        String descripcion = payload.get("descripcion");
        String result = unitService.modifyUnit(idUnidad, nombre, descripcion, email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Unit modified successfully");
    }

    /* actualizacion de estado del hardware */
    @PostMapping("/status")
    public ResponseEntity<String> updateUnitStatus(@RequestParam String idUnidad, @RequestParam String status) {

        String result = unitService.changeUnitStatus(idUnidad, status);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Status updated to " + status);
    }

    /* gestion de permisos y vinculaciones */
    @GetMapping("/{idUnidad}/users")
    public ResponseEntity<?> getUnitUsers(@PathVariable String idUnidad, @RequestAttribute("authenticatedEmail") String email) {
        List<java.util.Map<String, Object>> users = unitService.getUsersByUnit(idUnidad, email);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{idUnidad}/users/{targetEmail}/role")
    public ResponseEntity<String> changeRole(
            @PathVariable String idUnidad, 
            @PathVariable String targetEmail, 
            @RequestBody java.util.Map<String, String> payload, 
            @RequestAttribute("authenticatedEmail") String email) {
        String newRole = payload.get("role");
        String result = unitService.changeUserRole(idUnidad, targetEmail, newRole, email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("Role updated successfully");
    }

    @DeleteMapping("/{idUnidad}/users/{targetEmail}")
    public ResponseEntity<String> removeUser(
            @PathVariable String idUnidad, 
            @PathVariable String targetEmail, 
            @RequestAttribute("authenticatedEmail") String email) {
        String result = unitService.unlinkUserFromUnitAdmin(idUnidad, targetEmail, email);
        if (result.startsWith("Error")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("User removed successfully");
    }
}
