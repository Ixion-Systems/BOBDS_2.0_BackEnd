package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class OrdenService {
    private final String ordenesFile = "../data/ordenes.json";
    private final String ordenUnidadFile = "../data/ordenUnidad.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public String registrarOrden(RegistroOrdenDTO datos) {
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

        lock.writeLock().lock();
        try {
            List<Orden> todasOrdenes = cargarOrdenesInterno();
            
            int maxId = 0;
            for (Orden o : todasOrdenes) {
                if (o.getIdOrden() > maxId) {
                    maxId = o.getIdOrden();
                }
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
            OrdenUnidad nuevoVinculo = new OrdenUnidad(nextId, datos.getIdUnidad());
            
            vinculos.add(nuevoVinculo);
            guardarOrdenUnidad(vinculos);

            return "OK";
        } catch (IOException e) {
            return "Error interno al guardar la orden: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Orden> cargarOrdenes() {
        lock.readLock().lock();
        try {
            return cargarOrdenesInterno();
        } catch (IOException e) {
            System.err.println("Error leyendo archivo de órdenes: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private List<Orden> cargarOrdenesInterno() throws IOException {
        File file = new File(ordenesFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        Orden[] arr = objectMapper.readValue(file, Orden[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    public void eliminarOrdenesPorUnidad(String idUnidad) {
        lock.writeLock().lock();
        try {
            List<OrdenUnidad> vinculos = cargarOrdenUnidad();
            List<Integer> ordenesAEliminar = vinculos.stream()
                .filter(v -> v.getIdUnidad() != null && v.getIdUnidad().equals(idUnidad))
                .map(OrdenUnidad::getIdOrden)
                .toList();
            
            if (!ordenesAEliminar.isEmpty()) {
                List<Orden> ordenes = cargarOrdenesInterno();
                ordenes.removeIf(o -> ordenesAEliminar.contains(o.getIdOrden()));
                guardarOrdenes(ordenes);
                
                vinculos.removeIf(v -> v.getIdUnidad() != null && v.getIdUnidad().equals(idUnidad));
                guardarOrdenUnidad(vinculos);
            }
        } catch (IOException e) {
            System.err.println("Error eliminando órdenes en cascada: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Orden> obtenerOrdenesPorUnidad(String idUnidad) {
        lock.readLock().lock();
        try {
            List<OrdenUnidad> vinculos = cargarOrdenUnidad();
            List<Integer> ids = vinculos.stream()
                .filter(v -> v.getIdUnidad() != null && v.getIdUnidad().equals(idUnidad))
                .map(OrdenUnidad::getIdOrden)
                .toList();

            List<Orden> todas = cargarOrdenesInterno();
            return todas.stream()
                .filter(o -> ids.contains(o.getIdOrden()))
                .map(o -> new Orden(o.getIdOrden(), o.getOrden(), null, o.getEstado()))
                .toList();
        } catch (IOException e) {
            System.err.println("Error obteniendo órdenes por unidad: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String eliminarOrden(int idOrden) {
        lock.writeLock().lock();
        try {
            List<Orden> ordenes = cargarOrdenesInterno();
            boolean removido = ordenes.removeIf(o -> o.getIdOrden() == idOrden);
            if (removido) {
                guardarOrdenes(ordenes);
            }

            List<OrdenUnidad> vinculos = cargarOrdenUnidad();
            boolean removidoVinculo = vinculos.removeIf(v -> v.getIdOrden() == idOrden);
            if (removidoVinculo) {
                guardarOrdenUnidad(vinculos);
            }

            if (!removido && !removidoVinculo) {
                return "Error: No se encontró la orden.";
            }
            return "OK";
        } catch (IOException e) {
            return "Error interno al eliminar la orden: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
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