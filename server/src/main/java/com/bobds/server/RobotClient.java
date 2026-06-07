package com.bobds.server;

import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class RobotClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String ROBOT_URL = "http://localhost:7777/robot/ejecutar";

    public void enviarOrden(String idUnidad, String orden) {
        try {
            String body = "idUnidad=" + idUnidad +
                          "&orden=" + orden;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ROBOT_URL))
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