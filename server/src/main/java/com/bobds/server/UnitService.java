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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UnitService {
    private final String unitsFile = "../data/unidades.json";
    private final String userUnitsFile = "../data/usuarioUnidades.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    public List<UnitListDTO> getUnitsByUser(String email) {
        lock.readLock().lock();
        List<UnitListDTO> result = new ArrayList<>();
        try {
            List<UserUnit> links = loadUserUnits();
            List<Unit> allUnits = loadUnits();

            for (UserUnit uu : links) {
                if (email != null && email.equals(uu.getEmail())) {
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
        } catch (IOException e) {
            System.err.println("Error reading JSON files: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    public String registerUnit(RegisterUnitDTO data) {
        lock.writeLock().lock();
        try {
            List<Unit> allUnits = loadUnits();
            
            for (Unit u : allUnits) {
                if (u.getUnitId() != null && u.getUnitId().equals(data.getIdUnidad())) {
                    return "Error: The Unit ID is already registered.";
                }
            }

            String newId = data.getIdUnidad();
            
            Unit newUnit = new Unit();
            newUnit.setName(data.getNombre());
            newUnit.setUnitId(newId);
            newUnit.setDescription(data.getDescripcion());
            newUnit.setStatus("Inactivo");
            
            String assignedCode = data.getCodVinculacion() != null ? data.getCodVinculacion() : generateRandomCode(allUnits);
            newUnit.setLinkCode(assignedCode);
            
            newUnit.setCodeGeneratedAtMs(System.currentTimeMillis());
            newUnit.setCodeUsed(false);
            
            allUnits.add(newUnit);
            saveUnits(allUnits);

            List<UserUnit> links = loadUserUnits();
            UserUnit newLink = new UserUnit();
            newLink.setUnitId(newId);
            newLink.setEmail(data.getUserEmail());
            newLink.setRole("Propietario");
            
            links.add(newLink);
            saveUserUnits(links);

            return "OK";
        } catch (IOException e) {
            return "Internal error saving data: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String generateNewCode(String unitId) {
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
    }

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

    public UnitInfoDTO getUnitInfo(String unitId) {
        lock.readLock().lock();
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
                    // FIX CONCURRENCY FLAW: Using UserService lock
                    userService.getLock().readLock().lock();
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
                        userService.getLock().readLock().unlock();
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
                    codeStatus
                );
            }
        } catch (IOException e) {
            System.err.println("Error fetching unit info: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public String deleteUnit(String unitId) {
        lock.writeLock().lock();
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

            return "OK";
        } catch (IOException e) {
            return "Internal error deleting data: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String modifyUnit(String unitId, String newName, String newDescription) {
        lock.writeLock().lock();
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
                if (newDescription != null) {
                    u.setDescription(newDescription);
                }
                saveUnits(allUnits);
                return "OK";
            }
            return "Error: Unit with ID " + unitId + " not found.";
        } catch (IOException e) {
            return "Internal error modifying data: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public UnitInfoDTO getUnitInfoByCode(String code, String email) throws Exception {
        lock.readLock().lock();
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
                return getUnitInfo(u.getUnitId());
            }
            throw new Exception("Unidad no encontrada con ese código.");
        } finally {
            lock.readLock().unlock();
        }
    }

    public String linkUserToUnit(String email, String code) {
        lock.writeLock().lock();
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
            links.add(newLink);
            saveUserUnits(links);
            
            u.setCodeUsed(true);
            saveUnits(allUnits);
            
            return "OK";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String unlinkUserFromUnit(String email, String unitId) {
        lock.writeLock().lock();
        try {
            List<UserUnit> links = loadUserUnits();
            Optional<UserUnit> linkOpt = links.stream()
                .filter(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(email))
                .findFirst();
                
            if (!linkOpt.isPresent()) return "Error: No estás vinculado a esta unidad.";
            if ("Propietario".equals(linkOpt.get().getRole())) return "Error: Un Propietario no puede desvincularse, debe eliminar la unidad o transferirla.";
            
            links.removeIf(uu -> uu.getUnitId().equals(unitId) && uu.getEmail().equals(email));
            saveUserUnits(links);
            return "OK";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Autowired
    private SseService sseService;

    public String changeUnitStatus(String unitId, String status) {
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
    }

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
}