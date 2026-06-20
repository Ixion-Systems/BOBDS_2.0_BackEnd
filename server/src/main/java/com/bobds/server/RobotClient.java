package com.bobds.server;

import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
/* cliente http para simulador */
public class RobotClient {

    /* variables y configuracion */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final String ROBOT_URL = "http://localhost:7777/robot/ejecutar";

    /* transmision de comandos por red */
    public void enviarOrden(String idUnidad, int idOrden, String orden) {
        try {
            String body = "idUnidad=" + java.net.URLEncoder.encode(idUnidad, java.nio.charset.StandardCharsets.UTF_8.toString()) +
                          "&idOrden=" + idOrden + 
                          "&orden=" + java.net.URLEncoder.encode(orden, java.nio.charset.StandardCharsets.UTF_8.toString());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ROBOT_URL))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Orden enviada al simulador: " + idUnidad + " (ID: " + idOrden + ")");
        } catch (Exception e) {
            System.err.println("Error al contactar al simulador: " + e.getMessage());
        }
    }

    public void cancelOrder(int idOrden) {
        try {
            String body = "idOrden=" + idOrden;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7777/robot/cancel"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Solicitud de cancelación enviada al simulador (ID: " + idOrden + ")");
        } catch (Exception e) {
            System.err.println("Error al contactar al simulador para cancelar: " + e.getMessage());
        }
    }
}