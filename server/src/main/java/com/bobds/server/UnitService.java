package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class UnitService {
    private final String unidadesFile = "data/unidades.json";
    private final String usuarioUnidadesFile = "data/usuarioUnidades.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<UnidadListadoDTO> getUnidadesByUsuario(String email) {
        List<UnidadListadoDTO> result = new ArrayList<>();
        try {
            List<UsuarioUnidad> vinculos = cargarUsuarioUnidades();
            List<Unidad> todasUnidades = cargarUnidades();

            for (UsuarioUnidad uu : vinculos) {
                if (email != null && email.equals(uu.getEmail())) {
                    // Buscar la unidad correspondiente por ID
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
        }
        return result;
    }

    public String registrarUnidad(RegistroUnidadDTO datos) {
        try {
            List<Unidad> todasUnidades = cargarUnidades();
            
            // Verificar si el IDUnidad ya existe
            for (Unidad u : todasUnidades) {
                if (u.getIdUnidad() != null && u.getIdUnidad().equals(datos.getIdUnidad())) {
                    return "Error: El IDUnidad '" + datos.getIdUnidad() + "' ya está registrado. Por favor intente con otro ID.";
                }
            }

            // Crear y guardar la unidad
            Unidad nueva = new Unidad();
            nueva.setNombre(datos.getNombre());
            nueva.setIdUnidad(datos.getIdUnidad());
            nueva.setDescripcion(datos.getDescripcion());
            nueva.setEstado("Inactivo");
            nueva.setCodVinculacion(datos.getCodVinculacion());
            
            todasUnidades.add(nueva);
            guardarUnidades(todasUnidades);

            // Crear y guardar el vínculo de usuario
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
        }
    }

    public String eliminarUnidad(String idUnidad) {
        try {
            // Eliminar de unidades.json
            List<Unidad> todasUnidades = cargarUnidades();
            boolean removidoUnidad = todasUnidades.removeIf(u -> u.getIdUnidad() != null && u.getIdUnidad().equals(idUnidad));
            if (removidoUnidad) {
                guardarUnidades(todasUnidades);
            }

            // Eliminar de usuarioUnidades.json (limpiar todos los vínculos)
            List<UsuarioUnidad> vinculos = cargarUsuarioUnidades();
            boolean removidoVinculo = vinculos.removeIf(uu -> uu.getIdUnidad() != null && uu.getIdUnidad().equals(idUnidad));
            if (removidoVinculo) {
                guardarUsuarioUnidades(vinculos);
            }

            if (!removidoUnidad && !removidoVinculo) {
                return "Error: No se encontró la unidad con ID " + idUnidad;
            }
            return "OK";
        } catch (IOException e) {
            return "Error interno al eliminar los datos: " + e.getMessage();
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
