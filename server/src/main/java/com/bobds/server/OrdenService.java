package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
public class OrdenService {
    private final String ordenesFile = "../data/ordenes.json";
    private final String ordenUnidadFile = "../data/ordenUnidad.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Semaphore mutex = new Semaphore(1);

    @Autowired
    private RobotClient robotClient;

    public String registrarOrden(RegistroOrdenDTO datos) {
        if (datos.getIdUnidad() == null || datos.getIdUnidad().trim().isEmpty())
            return "Error: ID de unidad requerido.";
        if (datos.getIdUnidad().length() > 20)
            return "Error: El ID de unidad no puede exceder los 20 caracteres.";
        if (datos.getOrden() == null || datos.getOrden().trim().isEmpty())
            return "Error: La orden no puede estar vacía.";
        if (datos.getOrden().length() > 50)
            return "Error: La orden no puede exceder los 50 caracteres.";
        if (datos.getNotas() != null && datos.getNotas().length() > 200)
            return "Error: Las notas no pueden exceder los 200 caracteres.";

        try {
            mutex.acquire();
            try {
                List<Orden> todasOrdenes = cargarOrdenes();

                int maxId = 0;
                for (Orden o : todasOrdenes) {
                    if (o.getIdOrden() > maxId) maxId = o.getIdOrden();
                }
                int nextId = maxId + 1;

                Orden nuevaOrden = new Orden(
                    nextId,
                    datos.getOrden(),
                    datos.getNotas() != null ? datos.getNotas() : "",
                    "En Cola"
                );
                todasOrdenes.add(nuevaOrden);
                guardarOrdenes(todasOrdenes);

                List<OrdenUnidad> vinculos = cargarOrdenUnidad();
                vinculos.add(new OrdenUnidad(nextId, datos.getIdUnidad()));
                guardarOrdenUnidad(vinculos);

                // Avisar al simulador FUERA del semáforo para no bloquearlo
                mutex.release();
                robotClient.enviarOrden(datos.getIdUnidad(), datos.getOrden());
                return "OK";

            } catch (IOException e) {
                return "Error interno al guardar la orden: " + e.getMessage();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: operación interrumpida.";
        } finally {
            // Solo libera si el semáforo sigue adquirido (no se liberó antes)
            if (mutex.availablePermits() == 0) {
                mutex.release();
            }
        }
    }

    public String cambiarEstadoOrden(String idUnidad, String nuevoEstado) {
        try {
            mutex.acquire();
            try {
                List<Orden> ordenes = cargarOrdenes();
                List<OrdenUnidad> vinculos = cargarOrdenUnidad();

                boolean encontrado = false;
                for (OrdenUnidad ov : vinculos) {
                    if (ov.getIdUnidad().equals(idUnidad)) {
                        for (Orden o : ordenes) {
                            if (o.getIdOrden() == ov.getIdOrden()
                                    && o.getEstado().equals("En Cola")) {
                                o.setEstado(nuevoEstado);
                                encontrado = true;
                                break;
                            }
                        }
                    }
                    if (encontrado) break;
                }

                if (!encontrado)
                    return "Error: No se encontró orden activa para unidad " + idUnidad;

                guardarOrdenes(ordenes);
                return "OK";
            } catch (IOException e) {
                return "Error: " + e.getMessage();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: operación interrumpida.";
        }
    }

    public List<Orden> cargarOrdenes() throws IOException {
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