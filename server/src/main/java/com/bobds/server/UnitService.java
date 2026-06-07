package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UnitService {
    private final String unidadesFile = "../data/unidades.json";
    private final String usuarioUnidadesFile = "../data/usuarioUnidades.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Autowired
    private OrdenService ordenService;

    public List<UnidadListadoDTO> getUnidadesByUsuario(String email) {
        lock.readLock().lock();
        List<UnidadListadoDTO> result = new ArrayList<>();
        try {
            List<UsuarioUnidad> vinculos = cargarUsuarioUnidades();
            List<Unidad> todasUnidades = cargarUnidades();

            for (UsuarioUnidad uu : vinculos) {
                if (email != null && email.equals(uu.getEmail())) {
                    Optional<Unidad> unitOpt = todasUnidades.stream()
                        .filter(u -> u.getIdUnidad() != null && u.getIdUnidad().equals(uu.getIdUnidad()))
                        .findFirst();

                    if (unitOpt.isPresent()) {
                        Unidad u = unitOpt.get();
                        result.add(new UnidadListadoDTO(
                            u.getNombre(),
                            u.getIdUnidad(),
                            u.getEstado(),
                            uu.getRol()
                        ));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo archivos JSON: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    public String registrarUnidad(RegistroUnidadDTO datos) {
        lock.writeLock().lock();
        try {
            List<Unidad> todasUnidades = cargarUnidades();
            
            for (Unidad u : todasUnidades) {
                if (u.getIdUnidad() != null && u.getIdUnidad().equals(datos.getIdUnidad())) {
                    return "Error: El IDUnidad '" + datos.getIdUnidad() + "' ya está registrado. Por favor intente con otro ID.";
                }
            }

            if (datos.getCodVinculacion() != null && !datos.getCodVinculacion().trim().isEmpty()) {
                boolean codeExists = todasUnidades.stream()
                    .anyMatch(u -> datos.getCodVinculacion().equals(u.getCodVinculacion()));
                if (codeExists) {
                    return "Error: El código de vinculación ya está en uso por otra unidad. Genere uno nuevo.";
                }
            }

            Unidad nueva = new Unidad();
            nueva.setNombre(datos.getNombre());
            nueva.setIdUnidad(datos.getIdUnidad());
            nueva.setDescripcion(datos.getDescripcion());
            nueva.setEstado("Inactivo");
            nueva.setCodVinculacion(datos.getCodVinculacion());
            nueva.setCodGeneradoEnMs(System.currentTimeMillis());
            nueva.setCodUsado(false);
            
            todasUnidades.add(nueva);
            guardarUnidades(todasUnidades);

            List<UsuarioUnidad> vinculos = cargarUsuarioUnidades();
            UsuarioUnidad nuevoVinculo = new UsuarioUnidad();
            nuevoVinculo.setIdUnidad(datos.getIdUnidad());
            nuevoVinculo.setEmail(datos.getEmail());
            nuevoVinculo.setRol(datos.getRol() != null && !datos.getRol().isEmpty() ? datos.getRol() : "Propietario");
            
            vinculos.add(nuevoVinculo);
            guardarUsuarioUnidades(vinculos);

            return "OK";
        } catch (IOException e) {
            return "Error interno al guardar los datos: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String generateNewCode(String idUnidad) {
        lock.writeLock().lock();
        try {
            List<Unidad> todasUnidades = cargarUnidades();
            Optional<Unidad> unitOpt = todasUnidades.stream()
                .filter(u -> u.getIdUnidad() != null && u.getIdUnidad().equals(idUnidad))
                .findFirst();

            if (unitOpt.isPresent()) {
                Unidad u = unitOpt.get();
                String newCode = generarCodigoAleatorio(todasUnidades);
                u.setCodVinculacion(newCode);
                u.setCodGeneradoEnMs(System.currentTimeMillis());
                u.setCodUsado(false);
                guardarUnidades(todasUnidades);
                return newCode;
            }
            return null;
        } catch (IOException e) {
            System.err.println("Error generando código: " + e.getMessage());
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String generarCodigoAleatorio(List<Unidad> todasUnidades) {
        String letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String alfaNum = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String numeros = "0123456789";
        Random r = new Random();
        
        while (true) {
            StringBuilder part1 = new StringBuilder();
            for (int i=0; i<2; i++) part1.append(letras.charAt(r.nextInt(letras.length())));
            
            StringBuilder part2 = new StringBuilder();
            for (int i=0; i<4; i++) part2.append(alfaNum.charAt(r.nextInt(alfaNum.length())));
            
            StringBuilder part3 = new StringBuilder();
            for (int i=0; i<2; i++) part3.append(numeros.charAt(r.nextInt(numeros.length())));
            
            String code = part1.toString() + "-" + part2.toString() + "-" + part3.toString();
            boolean exists = todasUnidades.stream().anyMatch(u -> code.equals(u.getCodVinculacion()));
            if (!exists) return code;
        }
    }

    public UnidadInfoDTO getUnidadInfo(String idUnidad) {
        lock.readLock().lock();
        try {
            List<Unidad> todasUnidades = cargarUnidades();
            List<UsuarioUnidad> vinculos = cargarUsuarioUnidades();

            Optional<Unidad> unitOpt = todasUnidades.stream()
                .filter(u -> u.getIdUnidad() != null && u.getIdUnidad().equals(idUnidad))
                .findFirst();

            if (unitOpt.isPresent()) {
                Unidad u = unitOpt.get();
                
                String propietario = "Desconocido";
                String ownerEmail = null;
                for (UsuarioUnidad uu : vinculos) {
                    if (uu.getIdUnidad() != null && uu.getIdUnidad().equals(idUnidad) && "Propietario".equals(uu.getRol())) {
                        ownerEmail = uu.getEmail();
                        break;
                    }
                }
                
                if (ownerEmail != null) {
                    propietario = ownerEmail;
                    try {
                        File userFile = new File("../data/usuario.json");
                        if (userFile.exists() && userFile.length() > 0) {
                            Usuario[] usuariosArray = objectMapper.readValue(userFile, Usuario[].class);
                            for (Usuario usr : usuariosArray) {
                                if (ownerEmail.equals(usr.getEmail())) {
                                    propietario = usr.getNombreUsuario();
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error leyendo usuario.json: " + e.getMessage());
                    }
                }

                String estadoCodigo = "Vacío";
                if (u.getCodVinculacion() != null && !u.getCodVinculacion().trim().isEmpty()) {
                    if (u.isCodUsado()) {
                        estadoCodigo = "Vencido por uso";
                    } else if (u.getCodGeneradoEnMs() != null) {
                        long diff = System.currentTimeMillis() - u.getCodGeneradoEnMs();
                        long diezDiasEnMs = 10L * 24 * 60 * 60 * 1000;
                        if (diff > diezDiasEnMs) {
                            estadoCodigo = "Vencido por tiempo";
                        } else {
                            estadoCodigo = "Activo";
                        }
                    } else {
                        estadoCodigo = "Activo";
                    }
                }

                return new UnidadInfoDTO(
                    u.getNombre(),
                    u.getIdUnidad(),
                    u.getDescripcion(),
                    propietario,
                    u.getCodVinculacion(),
                    estadoCodigo
                );
            }
        } catch (IOException e) {
            System.err.println("Error obteniendo info de unidad: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public String eliminarUnidad(String idUnidad) {
        lock.writeLock().lock();
        try {
            List<Unidad> todasUnidades = cargarUnidades();
            boolean removidoUnidad = todasUnidades.removeIf(u -> u.getIdUnidad() != null && u.getIdUnidad().equals(idUnidad));
            if (removidoUnidad) {
                guardarUnidades(todasUnidades);
            }

            List<UsuarioUnidad> vinculos = cargarUsuarioUnidades();
            boolean removidoVinculo = vinculos.removeIf(uu -> uu.getIdUnidad() != null && uu.getIdUnidad().equals(idUnidad));
            if (removidoVinculo) {
                guardarUsuarioUnidades(vinculos);
            }

            if (!removidoUnidad && !removidoVinculo) {
                return "Error: No se encontró la unidad con ID " + idUnidad;
            }

            ordenService.eliminarOrdenesPorUnidad(idUnidad);

            return "OK";
        } catch (IOException e) {
            return "Error interno al eliminar los datos: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String modificarUnidad(String idUnidad, String nuevoNombre, String nuevaDescripcion) {
        lock.writeLock().lock();
        try {
            List<Unidad> todasUnidades = cargarUnidades();
            Optional<Unidad> unitOpt = todasUnidades.stream()
                .filter(u -> u.getIdUnidad() != null && u.getIdUnidad().equals(idUnidad))
                .findFirst();

            if (unitOpt.isPresent()) {
                Unidad u = unitOpt.get();
                if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
                    u.setNombre(nuevoNombre);
                }
                if (nuevaDescripcion != null) {
                    u.setDescripcion(nuevaDescripcion);
                }
                guardarUnidades(todasUnidades);
                return "OK";
            }
            return "Error: No se encontró la unidad con ID " + idUnidad;
        } catch (IOException e) {
            return "Error interno al modificar los datos: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public UnidadInfoDTO getUnidadInfoByCode(String code) throws Exception {
        lock.readLock().lock();
        try {
            List<Unidad> todasUnidades = cargarUnidades();
            Optional<Unidad> unitOpt = todasUnidades.stream()
                .filter(u -> code.equals(u.getCodVinculacion()))
                .findFirst();

            if (unitOpt.isPresent()) {
                Unidad u = unitOpt.get();
                if (u.isCodUsado()) {
                    throw new Exception("El código ya ha sido utilizado.");
                }
                if (u.getCodGeneradoEnMs() != null) {
                    long diff = System.currentTimeMillis() - u.getCodGeneradoEnMs();
                    long diezDiasEnMs = 10L * 24 * 60 * 60 * 1000;
                    if (diff > diezDiasEnMs) {
                        throw new Exception("El código ha expirado.");
                    }
                }
                return getUnidadInfo(u.getIdUnidad());
            }
            throw new Exception("Unidad no encontrada con ese código.");
        } finally {
            lock.readLock().unlock();
        }
    }

    public String linkUserToUnit(String email, String code) {
        lock.writeLock().lock();
        try {
            List<Unidad> todasUnidades = cargarUnidades();
            Optional<Unidad> unitOpt = todasUnidades.stream()
                .filter(u -> code.equals(u.getCodVinculacion()))
                .findFirst();
                
            if (!unitOpt.isPresent()) return "Error: Código no encontrado.";
            Unidad u = unitOpt.get();
            
            if (u.isCodUsado()) return "Error: El código ya ha sido utilizado.";
            if (u.getCodGeneradoEnMs() != null && (System.currentTimeMillis() - u.getCodGeneradoEnMs()) > 10L * 24 * 60 * 60 * 1000) {
                return "Error: El código ha expirado.";
            }
            
            List<UsuarioUnidad> vinculos = cargarUsuarioUnidades();
            boolean alreadyLinked = vinculos.stream()
                .anyMatch(uu -> uu.getIdUnidad().equals(u.getIdUnidad()) && uu.getEmail().equals(email));
                
            if (alreadyLinked) return "Error: Ya te encuentras vinculado a esta unidad.";
            
            UsuarioUnidad nuevoVinculo = new UsuarioUnidad();
            nuevoVinculo.setIdUnidad(u.getIdUnidad());
            nuevoVinculo.setEmail(email);
            nuevoVinculo.setRol("Invitado");
            vinculos.add(nuevoVinculo);
            guardarUsuarioUnidades(vinculos);
            
            u.setCodUsado(true);
            guardarUnidades(todasUnidades);
            
            return "OK";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String unlinkUserFromUnit(String email, String idUnidad) {
        lock.writeLock().lock();
        try {
            List<UsuarioUnidad> vinculos = cargarUsuarioUnidades();
            Optional<UsuarioUnidad> vinculoOpt = vinculos.stream()
                .filter(uu -> uu.getIdUnidad().equals(idUnidad) && uu.getEmail().equals(email))
                .findFirst();
                
            if (!vinculoOpt.isPresent()) return "Error: No estás vinculado a esta unidad.";
            if ("Propietario".equals(vinculoOpt.get().getRol())) return "Error: Un Propietario no puede desvincularse, debe eliminar la unidad o transferirla.";
            
            vinculos.removeIf(uu -> uu.getIdUnidad().equals(idUnidad) && uu.getEmail().equals(email));
            guardarUsuarioUnidades(vinculos);
            return "OK";
        } catch (Exception e) {
            return "Error interno: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<UsuarioUnidad> cargarUsuarioUnidades() throws IOException {
        File file = new File(usuarioUnidadesFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        UsuarioUnidad[] arr = objectMapper.readValue(file, UsuarioUnidad[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private List<Unidad> cargarUnidades() throws IOException {
        File file = new File(unidadesFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        Unidad[] arr = objectMapper.readValue(file, Unidad[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private void guardarUnidades(List<Unidad> unidades) throws IOException {
        File file = new File(unidadesFile);
        objectMapper.writeValue(file, unidades);
    }

    private void guardarUsuarioUnidades(List<UsuarioUnidad> vinculos) throws IOException {
        File file = new File(usuarioUnidadesFile);
        objectMapper.writeValue(file, vinculos);
    }
}