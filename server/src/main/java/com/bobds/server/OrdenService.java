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
    private final String ordenesFile = "data/ordenes.json";
    private final String ordenUnidadFile = "data/ordenUnidad.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Orden> obtenerTodasLasOrdenes() {
        List<Orden> result = new ArrayList<>();
        try {
            File file = new File(ordenesFile);
            if (!file.exists() || file.length() == 0) {
                result = generarOrdenesSimuladas();
                guardarOrdenes(result);
            } else {
                Orden[] arr = objectMapper.readValue(file, Orden[].class);
                result = new ArrayList<>(Arrays.asList(arr));
            }
        } catch (IOException e) {
            System.err.println("Error leyendo archivo de órdenes: " + e.getMessage());
        }
        return result;
    }

    public List<Orden> generarOrdenesSimuladas() {
        List<Orden> ordenes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Orden orden = new Orden(i, "Pendiente"); // Default estado
            ordenes.add(orden);
        }
        try {
            guardarOrdenes(ordenes);
            File file = new File(ordenUnidadFile);
            if (!file.exists() || file.length() == 0) {
                objectMapper.writeValue(file, new ArrayList<>());
            }
            List<OrdenUnidadLink> links = cargarOrdenUnidadLinks();
            for (Orden orden : ordenes) {
                boolean exists = links.stream()
                        .anyMatch(l -> l.getIdOrden() == orden.getIdOrden() && l.getIdUnidad().equals("UNI001"));
                if (!exists) {
                    OrdenUnidadLink link = new OrdenUnidadLink(orden.getIdOrden(), "UNI001");
                    links.add(link);
                }
            }
            guardarOrdenUnidadLinks(links);
        } catch (IOException e) {
            System.err.println("Error inicializando ordenUnidad.json: " + e.getMessage());
        }
        return ordenes;
    }

    public void VincularOrdenUnidad(int idOrden, String idUnidad) {
        try {
            List<OrdenUnidadLink> links = cargarOrdenUnidadLinks();
            boolean exists = links.stream()
                    .anyMatch(l -> l.getIdOrden() == idOrden && l.getIdUnidad().equals(idUnidad));
            if (!exists) {
                OrdenUnidadLink link = new OrdenUnidadLink(idOrden, idUnidad);
                links.add(link);
                guardarOrdenUnidadLinks(links);
            }
        } catch (IOException e) {
            System.err.println("Error vinculando orden y unidad: " + e.getMessage());
        }
    }

    private List<OrdenUnidadLink> cargarOrdenUnidadLinks() throws IOException {
        File file = new File(ordenUnidadFile);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        OrdenUnidadLink[] arr = objectMapper.readValue(file, OrdenUnidadLink[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private void guardarOrdenUnidadLinks(List<OrdenUnidadLink> links) throws IOException {
        File file = new File(ordenUnidadFile);
        objectMapper.writeValue(file, links);
    }

    private void guardarOrdenes(List<Orden> ordenes) throws IOException {
        File file = new File(ordenesFile);
        objectMapper.writeValue(file, ordenes);
    }

    // Inner class for the link
    public static class OrdenUnidadLink {
        private int idOrden;
        private String idUnidad;

        public OrdenUnidadLink() {}

        public OrdenUnidadLink(int idOrden, String idUnidad) {
            this.idOrden = idOrden;
            this.idUnidad = idUnidad;
        }

        public int getIdOrden() {
            return idOrden;
        }

        public void setIdOrden(int idOrden) {
            this.idOrden = idOrden;
        }

        public String getIdUnidad() {
            return idUnidad;
        }

        public void setIdUnidad(String idUnidad) {
            this.idUnidad = idUnidad;
        }
    }
}