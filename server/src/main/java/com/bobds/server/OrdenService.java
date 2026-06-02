package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OrdenService {
    private final String ordenesFile = "../data/ordenes.json";
    private final String ordenUnidadFile = "../data/ordenUnidad.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String registrarOrden(RegistroOrdenDTO datos) {
        // Validaciones previas
        if (datos.getIdUnidad() == null || datos.getIdUnidad().trim().isEmpty()) {
            return "Error: ID de unidad requerido.";
        }
        if (datos.getOrden() == null || datos.getOrden().trim().isEmpty()) {
            return "Error: La orden no puede estar vacía.";
        }
        if (datos.getOrden().length() > 50) {
            return "Error: La orden no puede exceder los 50 caracteres.";
        }
        if (datos.getNotas() != null && datos.getNotas().length() > 200) {
            return "Error: Las notas no pueden exceder los 200 caracteres.";
        }

        try {
            List<Orden> todasOrdenes = cargarOrdenes();
            
            // Auto-increment idOrden
            int maxId = 0;
            for (Orden o : todasOrdenes) {
                if (o.getIdOrden() > maxId) {
                    maxId = o.getIdOrden();
                }
            }
            int nextId = maxId + 1;

            // Crear y guardar la orden
            Orden nuevaOrden = new Orden(
                nextId,
                datos.getOrden(),
                datos.getNotas() != null ? datos.getNotas() : "",
                "En Cola"
            );
            
            todasOrdenes.add(nuevaOrden);
            guardarOrdenes(todasOrdenes);

            // Crear y guardar el vínculo OrdenUnidad
            List<OrdenUnidad> vinculos = cargarOrdenUnidad();
            OrdenUnidad nuevoVinculo = new OrdenUnidad(nextId, datos.getIdUnidad());
            
            vinculos.add(nuevoVinculo);
            guardarOrdenUnidad(vinculos);

            return "OK";
        } catch (IOException e) {
            return "Error interno al guardar la orden: " + e.getMessage();
        }
    }

    private List<Orden> cargarOrdenes() throws IOException {
        File file = new File(ordenesFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        Orden[] arr = objectMapper.readValue(file, Orden[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private List<OrdenUnidad> cargarOrdenUnidad() throws IOException {
        File file = new File(ordenUnidadFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        OrdenUnidad[] arr = objectMapper.readValue(file, OrdenUnidad[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private void guardarOrdenes(List<Orden> ordenes) throws IOException {
        File file = new File(ordenesFile);
        objectMapper.writeValue(file, ordenes);
    }

    private void guardarOrdenUnidad(List<OrdenUnidad> vinculos) throws IOException {
        File file = new File(ordenUnidadFile);
        objectMapper.writeValue(file, vinculos);
    }
}
