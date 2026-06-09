package com.bobds.server;

import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class RobotClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final String ROBOT_URL = "http://localhost:7777/robot/ejecutar";

    public void enviarOrden(String idUnidad, String orden) {
        try {
            String body = "idUnidad=" + java.net.URLEncoder.encode(idUnidad, java.nio.charset.StandardCharsets.UTF_8.toString()) +
                          "&orden=" + java.net.URLEncoder.encode(orden, java.nio.charset.StandardCharsets.UTF_8.toString());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ROBOT_URL))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Orden enviada al simulador: " + idUnidad);
        } catch (Exception e) {
            System.err.println("Error al contactar al simulador: " + e.getMessage());
        }
    }
}