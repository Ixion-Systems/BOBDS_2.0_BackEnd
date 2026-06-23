package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.security.SecureRandom;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;

@Service
/* servicio logico de unidades */
public class UnitService {
    /* dependencias y rutas locales */
    private final String unitsFile = "../data/unidades.json";
    private final String userUnitsFile = "../data/usuarioUnidades.json";
    private final ObjectMapper objectMapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    private final Semaphore wrt = new Semaphore(1);
    private final Semaphore mutex = new Semaphore(1);
    private int readCount = 0;
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastUnitsQueryLog = new java.util.concurrent.ConcurrentHashMap<>();

    public void acquireRead() {
        try {
            mutex.acquire();
            readCount++;
            if (readCount == 1) wrt.acquire();
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operacion interrumpida", e);
        }
    }

    public void releaseRead() {
        try {
            mutex.acquire();
            readCount--;
            if (readCount == 0) wrt.release();
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void acquireWrite() {
        try {
            wrt.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operacion interrumpida", e);
        }
    }

    public void releaseWrite() {
        wrt.release();
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private SseService sseService;

    @Autowired
    private LogService logService;

    /* consultas    /* listado para el frontend */
    public List<UnitListDTO> getUnitsByUser(String userEmail) {
        acquireRead();
        try {
            List<UnitListDTO> result = new ArrayList<>();
            List<UserUnit> links = loadUserUnits();
            List<Unit> allUnits = loadUnits();

            for (UserUnit uu : links) {
                if (userEmail != null && userEmail.equals(uu.getEmail())) {
                    Optional<Unit> unitOpt = allUnits.stream()
                        .filter(u -> u.getUnitId() != null && u.getUnitId().equals(uu.getUnitId()))
                        .findFirst();

                    if (unitOpt.isPresent()) {
                        Unit u = unitOpt.get();
                        result.add(new UnitListDTO(
                            u.getName(),
                            u.getUnitId(),
                            u.getStatus(),
                            uu.getRole()
                        ));
                    }
                }
            }
            return result;
        } catch (IOException e) {
            System.err.println("Error fetching user units: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            releaseRead();
            if (userEmail != null) {
                long now = System.currentTimeMillis();
                long last = lastUnitsQueryLog.getOrDefault(userEmail + "_byUser", 0L);
                if (now - last > 3000) {
                    lastUnitsQueryLog.put(userEmail + "_byUser", now);
                    try { logService.registerLog(userEmail, 4, "Consulta de lista de unidades", "Unidad", null); } catch (Exception ignore) {}
                }
            }
        }
    }

    public List<UnitListDTO> getAllUnits(String userEmail) {
        acquireRead();
        try {
            List<Unit> allUnits = loadUnits();
            List<UserUnit> allLinks = loadUserUnits();

            return allUnits.stream()
                .filter(u -> allLinks.stream().anyMatch(link -> link.getUnitId().equals(u.getUnitId()) && link.getEmail().equals(userEmail)))
                .map(u -> {
                    String role = allLinks.stream().filter(l -> l.getUnitId().equals(u.getUnitId()) && l.getEmail().equals(userEmail)).findFirst().map(UserUnit::getRole).orElse("N/A");
                    return new UnitListDTO(u.getName(), u.getUnitId(), u.getStatus(), role);
                })
                .toList();

        } catch (IOException e) {
            System.err.println("Error fetching units for user: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            releaseRead();
            if (userEmail != null) {
                long now = System.currentTimeMillis();
                long last = lastUnitsQueryLog.getOrDefault(userEmail + "_all", 0L);
                if (now - last > 3000) {
                    lastUnitsQueryLog.put(userEmail + "_all", now);
                    try { logService.registerLog(userEmail, 4, "Consulta de lista de unidades", "Unidad", null); } catch (Exception ignore) {}
                }
            }
        }
    }

    public List<Unit> getSystemUnits() {
        acquireRead();
        try {
            return loadUnits();
        } catch (IOException e) {
            return new ArrayList<>();
        } finally {
            releaseRead();
        }
    }

    /* alta de nuevo hardware */
    public String registerUnit(RegisterUnitDTO datos) {
        acquireWrite();
        try {
            List<Unit> allUnits = loadUnits();
            String newUnitId = datos.getIdUnidad();

            for (Unit u : allUnits) {
                if (u.getUnitId() != null && u.getUnitId().equals(newUnitId)) {
                    return "Error: The Unit ID is already registered.";
                }
            }

            Unit newUnit = new Unit();
            newUnit.setName(datos.getNombre());
            newUnit.setUnitId(newUnitId);
            newUnit.setDescription(datos.getDescripcion());
            newUnit.setStatus("Inactivo");

            String assignedCode = datos.getCodVinculacion() != null ? datos.getCodVinculacion() : generateRandomCode(allUnits);
            newUnit.setLinkCode(assignedCode);

            newUnit.setCodeGeneratedAtMs(System.currentTimeMillis());
            newUnit.setCodeUsed(false);
            newUnit.setCreatedAtMs(System.currentTimeMillis());

            allUnits.add(newUnit);
            saveUnits(allUnits);

            List<UserUnit> links = loadUserUnits();
            UserUnit newLink = new UserUnit();
            newLink.setUnitId(newUnitId);
            newLink.setEmail(datos.getUserEmail());
            newLink.setRole("Propietario");
            newLink.setVinculadoEnMs(System.currentTimeMillis());
            links.add(newLink);
            saveUserUnits(links);

            try { logService.registerLog(datos.getUserEmail(), 1, "Unidad registrada: " + newUnitId, "Unidad", newUnitId); } catch (Exception ignore) {}

            return "OK";
        } catch (IOException e) {
            return "Error interno: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* renovacion de codigos */
    public String generateNewCode(String unitId) {
        acquireWrite();
        try {
            List<Unit> allUnits = loadUnits();
            Optional<Unit> unitOpt = allUnits.stream()
                .filter(u -> u.getUnitId() != null && u.getUnitId().equals(unitId))
                .findFirst();

            if (unitOpt.isPresent()) {
                Unit u = unitOpt.get();
                String newCode = generateRandomCode(allUnits);
                u.setLinkCode(newCode);
                u.setCodeGeneratedAtMs(System.currentTimeMillis());
                u.setCodeUsed(false);
                saveUnits(allUnits);
                return newCode;
            }
            return null;
        } catch (IOException e) {
            System.err.println("Error generating code: " + e.getMessage());
            return null;
        } finally {
            releaseWrite();
        }
    }

    /* utilidades aleatorias */
    private String generateRandomCode(List<Unit> allUnits) {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String alphaNum = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String numbers = "0123456789";
        SecureRandom r = new SecureRandom();

        while (true) {
            StringBuilder part1 = new StringBuilder();
            for (int i=0; i<2; i++) part1.append(letters.charAt(r.nextInt(letters.length())));

            StringBuilder part2 = new StringBuilder();
            for (int i=0; i<4; i++) part2.append(alphaNum.charAt(r.nextInt(alphaNum.length())));

            StringBuilder part3 = new StringBuilder();
            for (int i=0; i<2; i++) part3.append(numbers.charAt(r.nextInt(numbers.length())));

            String code = part1.toString() + "-" + part2.toString() + "-" + part3.toString();
            boolean exists = allUnits.stream().anyMatch(u -> code.equals(u.getLinkCode()));
            if (!exists) return code;
        }
    }

    /* extraccion de informacion puntual */
    public UnitInfoDTO getUnitInfo(String unitId, String email) {
        acquireRead();
        try {
            List<Unit> allUnits = loadUnits();
            List<UserUnit> links = loadUserUnits();

            Optional<Unit> unitOpt = allUnits.stream()
                .filter(u -> u.getUnitId() != null && u.getUnitId().equals(unitId))
                .findFirst();

            if (unitOpt.isPresent()) {
                Unit u = unitOpt.get();

                String owner = "Desconocido";
                String ownerEmail = null;
                for (UserUnit uu : links) {
                    if (uu.getUnitId() != null && uu.getUnitId().equals(unitId) && "Propietario".equals(uu.getRole())) {
                        ownerEmail = uu.getEmail();
                        break;
                    }
                }

                if (ownerEmail != null) {
                    owner = ownerEmail;

                    userService.acquireRead();
                    try {
                        File userFile = new File("../data/usuario.json");
                        if (userFile.exists() && userFile.length() > 0) {
                            User[] usersArray = objectMapper.readValue(userFile, User[].class);
                            for (User usr : usersArray) {
                                if (ownerEmail.equals(usr.getEmail())) {
                                    owner = usr.getUsername();
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error reading usuario.json: " + e.getMessage());
                    } finally {
                        userService.releaseRead();
                    }
                }

                String codeStatus = "Vacío";
                if (u.getLinkCode() != null && !u.getLinkCode().trim().isEmpty()) {
                    if (u.isCodeUsed()) {
                        codeStatus = "Vencido por uso";
                    } else if (u.getCodeGeneratedAtMs() != null) {
                        long diff = System.currentTimeMillis() - u.getCodeGeneratedAtMs();
                        long tenDaysMs = 10L * 24 * 60 * 60 * 1000;
                        if (diff > tenDaysMs) {
                            codeStatus = "Vencido por tiempo";
                        } else {
                            codeStatus = "Activo";
                        }
                    } else {
                        codeStatus = "Activo";
                    }
                }

                return new UnitInfoDTO(
                    u.getName(),
                    u.getUnitId(),
                    u.getDescription(),
                    owner,
                    u.getLinkCode(),
                    codeStatus,
                    u.getCreatedAtMs()
                );
            }
        } catch (IOException e) {
            System.err.println("Error fetching unit info: " + e.getMessage());
        } finally {
            releaseRead();
            try { logService.registerLog(email, 4, "Consulta de info de unidad: " + unitId, "Unidad", unitId); } catch (Exception ignore) {}
        }
        return null;
    }

    public Unit getUnitById(String unitId) {
        acquireRead();
        try {
            return loadUnits().stream().filter(u -> unitId.equals(u.getUnitId())).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        } finally {
            releaseRead();
        }
    }

    /* eliminacion total de unidades */
    public String deleteUnit(String unitId, String email) {
        acquireWrite();
        try {
            List<Unit> allUnits = loadUnits();
            boolean removedUnit = allUnits.removeIf(u -> u.getUnitId() != null && u.getUnitId().equals(unitId));
            if (removedUnit) {
                saveUnits(allUnits);
            }

            List<UserUnit> links = loadUserUnits();
            boolean removedLink = links.removeIf(uu -> uu.getUnitId() != null && uu.getUnitId().equals(unitId));
            if (removedLink) {
                saveUserUnits(links);
            }

            if (!removedUnit && !removedLink) {
                return "Error: Unit with ID " + unitId + " not found.";
            }

            orderService.deleteOrdersByUnit(unitId);
            try { logService.registerLog(email, 5, "Unidad eliminada: " + unitId, "Unidad", unitId); } catch (Exception ignore) {}

            return "OK";
        } catch (IOException e) {
            return "Internal error deleting data: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    public String forceDeleteUnit(String unitId, String adminEmail) {
        acquireWrite();
        try {
            List<Unit> allUnits = loadUnits();
            boolean removedUnit = allUnits.removeIf(u -> unitId.equals(u.getUnitId()));

            List<UserUnit> allLinks = loadUserUnits();
            boolean removedLink = allLinks.removeIf(link -> unitId.equals(link.getUnitId()));

            if (!removedUnit && !removedLink) {
                return "Error: Unit not found.";
            }

            orderService.deleteOrdersByUnit(unitId);
            
            saveUnits(allUnits);
            saveUserUnits(allLinks);

            orderService.deleteOrdersByUnit(unitId);
            try { logService.registerLog(adminEmail, 5, "Unidad eliminada por admin: " + unitId, "Unidad", unitId); } catch (Exception ignore) {}

            return "OK";
        } catch (IOException e) {
            return "Internal error force deleting unit: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* modificacion parcial de unidad */
    public String modifyUnit(String unitId, String newName, String newDesc, String email) {
        acquireWrite();
        try {
            List<Unit> allUnits = loadUnits();
            Optional<Unit> unitOpt = allUnits.stream()
                .filter(u -> u.getUnitId() != null && u.getUnitId().equals(unitId))
                .findFirst();

            if (unitOpt.isPresent()) {
                Unit u = unitOpt.get();
                if (newName != null && !newName.trim().isEmpty()) {
                    u.setName(newName);
                }
                if (newDesc != null) {
                    u.setDescription(newDesc);
                }
                saveUnits(allUnits);
                try { logService.registerLog(email, 3, "Unidad modificada: " + unitId, "Unidad", unitId); } catch (Exception ignore) {}
                return "OK";
            }
            return "Error: Unit with ID " + unitId + " not found.";
        } catch (IOException e) {
            return "Internal error modifying data: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* busqueda para vinculacion */
    public UnitInfoDTO getUnitInfoByCode(String code, String email) throws Exception {
        acquireRead();
        try {
            List<Unit> allUnits = loadUnits();
            Optional<Unit> unitOpt = allUnits.stream()
                .filter(u -> code.equals(u.getLinkCode()))
                .findFirst();

            if (unitOpt.isPresent()) {
                Unit u = unitOpt.get();

                List<UserUnit> links = loadUserUnits();
                boolean alreadyLinked = links.stream()
                    .anyMatch(uu -> uu.getUnitId().equals(u.getUnitId()) && uu.getEmail().trim().equalsIgnoreCase(email.trim()));

                if (alreadyLinked) {
                    throw new Exception("Ya te encuentras vinculado a esta unidad.");
                }

                if (u.isCodeUsed()) {
                    throw new Exception("El código ya ha sido utilizado.");
                }
                if (u.getCodeGeneratedAtMs() != null) {
                    long diff = System.currentTimeMillis() - u.getCodeGeneratedAtMs();
                    long tenDaysMs = 10L * 24 * 60 * 60 * 1000;
                    if (diff > tenDaysMs) {
                        throw new Exception("El código ha expirado.");
                    }
                }
                return getUnitInfo(u.getUnitId(), email);
            }
            throw new Exception("Unidad no encontrada con ese código.");
        } finally {
            releaseRead();
        }
    }

    /* autorizacion de invitados */
    public String linkUserToUnit(String email, String code) {
        acquireWrite();
        try {
            List<Unit> allUnits = loadUnits();
            Optional<Unit> unitOpt = allUnits.stream()
                .filter(u -> code.equals(u.getLinkCode()))
                .findFirst();

            if (!unitOpt.isPresent()) return "Error: Código no encontrado.";
            Unit u = unitOpt.get();

            if (u.isCodeUsed()) return "Error: El código ya ha sido utilizado.";
            if (u.getCodeGeneratedAtMs() != null && (System.currentTimeMillis() - u.getCodeGeneratedAtMs()) > 10L * 24 * 60 * 60 * 1000) {
                return "Error: El código ha expirado.";
            }

            List<UserUnit> links = loadUserUnits();
            boolean alreadyLinked = links.stream()
                .anyMatch(uu -> uu.getUnitId().equals(u.getUnitId()) && uu.getEmail().trim().equalsIgnoreCase(email.trim()));

            if (alreadyLinked) return "Error: Ya te encuentras vinculado a esta unidad.";

            UserUnit newLink = new UserUnit();
            newLink.setUnitId(u.getUnitId());
            newLink.setEmail(email);
            newLink.setRole("Invitado");
            newLink.setVinculadoEnMs(System.currentTimeMillis());
            links.add(newLink);
            saveUserUnits(links);

            u.setCodeUsed(true);
            saveUnits(allUnits);
            notifyUsersAboutUnitUpdate(u.getUnitId());
            try { logService.registerLog(email, 2, "Vinculacion a unidad: " + u.getUnitId(), "Unidad", u.getUnitId()); } catch (Exception ignore) {}

            return "OK";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* desvinculacion de usuarios */
    public String unlinkUserFromUnit(String email, String unitId) {
        acquireWrite();
        try {
            List<UserUnit> links = loadUserUnits();
            Optional<UserUnit> linkOpt = links.stream()
                .filter(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(email))
                .findFirst();

            if (!linkOpt.isPresent()) return "Error: No estás vinculado a esta unidad.";
            if ("Propietario".equals(linkOpt.get().getRole())) return "Error: Un Propietario no puede desvincularse, debe eliminar la unidad o transferirla.";

            links.removeIf(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(email));
            saveUserUnits(links);
            notifyUsersAboutUnitUpdate(unitId);
            sseService.sendEventToEmail(email, "unit_update", unitId);
            return "OK";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }



    /* actualizacion de estado en vivo */
    public String changeUnitStatus(String unitId, String status) {
        acquireWrite();
        try {
            List<Unit> allUnits = loadUnits();
            boolean found = false;
            for (Unit u : allUnits) {
                if (u.getUnitId() != null && u.getUnitId().equals(unitId)) {
                    u.setStatus(status);
                    found = true;
                    break;
                }
            }
            if (found) {
                saveUnits(allUnits);
                notifyUsersAboutUnitUpdate(unitId);
                return "OK";
            }
            return "Error: Unit ID not found";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* notificaciones sse */
    private void notifyUsersAboutUnitUpdate(String unitId) {
        try {
            List<UserUnit> arr = loadUserUnits();
            for (UserUnit uu : arr) {
                if (uu.getUnitId() != null && uu.getUnitId().equals(unitId)) {
                    sseService.sendEventToEmail(uu.getEmail(), "unit_update", unitId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error notifying unit updates: " + e.getMessage());
        }
    }

    /* persistencia de datos json */
    private List<UserUnit> loadUserUnits() throws IOException {
        File file = new File(userUnitsFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        UserUnit[] arr = objectMapper.readValue(file, UserUnit[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private List<Unit> loadUnits() throws IOException {
        File file = new File(unitsFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        Unit[] arr = objectMapper.readValue(file, Unit[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private void saveUnits(List<Unit> units) throws IOException {
        File file = new File(unitsFile);
        objectMapper.writeValue(file, units);
    }

    private void saveUserUnits(List<UserUnit> links) throws IOException {
        File file = new File(userUnitsFile);
        objectMapper.writeValue(file, links);
    }

    /* gestion de permisos y usuarios vinculados */
    public List<java.util.Map<String, Object>> getUsersByUnit(String unitId, String requesterEmail) {
        acquireRead();
        try {
            List<UserUnit> links = loadUserUnits();
            boolean isLinked = links.stream().anyMatch(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(requesterEmail));
            if (!isLinked) return new ArrayList<>();

            List<java.util.Map<String, Object>> result = new ArrayList<>();
            userService.acquireRead();
            try {
                File userFile = new File("../data/usuario.json");
                User[] usersArray = userFile.exists() ? objectMapper.readValue(userFile, User[].class) : new User[0];
                for (UserUnit uu : links) {
                    if (uu.getUnitId().equals(unitId)) {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("email", uu.getEmail());
                        map.put("role", uu.getRole());
                        map.put("vinculadoEnMs", uu.getVinculadoEnMs());
                        
                        String name = uu.getEmail();
                        for (User u : usersArray) {
                            if (u.getEmail().equals(uu.getEmail())) {
                                name = u.getUsername();
                                break;
                            }
                        }
                        map.put("name", name);
                        result.add(map);
                    }
                }
            } finally {
                userService.releaseRead();
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            releaseRead();
        }
    }

    public String changeUserRole(String unitId, String targetEmail, String newRole, String requesterEmail) {
        acquireWrite();
        try {
            if (targetEmail.equals(requesterEmail)) {
                return "Error: No puedes modificar tu propio rol.";
            }
            List<UserUnit> links = loadUserUnits();
            
            boolean isPropietario = links.stream().anyMatch(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(requesterEmail) && "Propietario".equals(uu.getRole()));
            if (!isPropietario) return "Error: Solo el Propietario puede modificar roles.";

            Optional<UserUnit> targetOpt = links.stream().filter(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(targetEmail)).findFirst();
            if (!targetOpt.isPresent()) return "Error: Usuario no encontrado en esta unidad.";
            
            if ("Propietario".equals(targetOpt.get().getRole())) {
                return "Error: No se puede modificar el rol de un Propietario.";
            }

            targetOpt.get().setRole(newRole);
            saveUserUnits(links);
            notifyUsersAboutUnitUpdate(unitId);
            try { logService.registerLog(requesterEmail, 2, "Cambio de rol a " + newRole + " para " + targetEmail, "Unidad", unitId); } catch (Exception ignore) {}
            return "OK";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    public String unlinkUserFromUnitAdmin(String unitId, String targetEmail, String requesterEmail) {
        acquireWrite();
        try {
            if (targetEmail.equals(requesterEmail)) {
                return "Error: Utiliza la funcion de desvincularte a ti mismo.";
            }
            List<UserUnit> links = loadUserUnits();
            
            boolean isPropietario = links.stream().anyMatch(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(requesterEmail) && "Propietario".equals(uu.getRole()));
            if (!isPropietario) return "Error: Solo el Propietario puede desvincular usuarios.";

            Optional<UserUnit> targetOpt = links.stream().filter(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(targetEmail)).findFirst();
            if (!targetOpt.isPresent()) return "Error: Usuario no encontrado en esta unidad.";
            
            if ("Propietario".equals(targetOpt.get().getRole())) {
                return "Error: No se puede desvincular a un Propietario.";
            }

            links.removeIf(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(targetEmail));
            saveUserUnits(links);
            notifyUsersAboutUnitUpdate(unitId);
            sseService.sendEventToEmail(targetEmail, "unit_update", unitId);
            try { logService.registerLog(requesterEmail, 4, "Usuario " + targetEmail + " desvinculado", "Unidad", unitId); } catch (Exception ignore) {}
            return "OK";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }
}