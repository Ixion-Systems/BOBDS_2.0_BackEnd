package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
public class LogService {
    private final String logsFile = "../data/logs.json";
    private final String logGravedadFile = "../data/logsgravedad.json";
    private final String logUsuarioFile = "../data/logsusuario.json";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Semaphore wrt = new Semaphore(1); // Mutex para escritura concurrente

    @Autowired
    private SseService sseService;

    private boolean isAdminUser(String email) {
        if (email == null) return false;
        try {
            File file = new File("../DATA/usuario.json");
            if (!file.exists()) return false;
            User[] users = objectMapper.readValue(file, User[].class);
            for (User u : users) {
                if (email.equals(u.getEmail()) && u.isAdmin()) return true;
            }
        } catch(Exception e) {}
        return false;
    }

    public void registerLog(String email, int idGravedad, String descripcion, String tipoEntidad, String entidadId) {
        if ("ADMIN_OVERRIDE".equals(email) || isAdminUser(email)) return;

        try {
            wrt.acquire();
            try {
                List<Log> logs = loadLogs();
                List<LogGravedad> logsGravedad = loadLogGravedades();
                List<LogUsuario> logsUsuario = loadLogUsuarios();

                int nextId = logs.isEmpty() ? 1 : logs.stream().mapToInt(Log::getIdLog).max().orElse(0) + 1;

                Log newLog = new Log();
                newLog.setIdLog(nextId);
                newLog.setDescripcion(descripcion);
                newLog.setFechaHora(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                newLog.setTipoEntidad(tipoEntidad);
                newLog.setEntidadId(entidadId);

                LogGravedad newLogGrav = new LogGravedad();
                newLogGrav.setIdLog(nextId);
                newLogGrav.setIdGravedad(idGravedad);

                LogUsuario newLogUsr = new LogUsuario();
                newLogUsr.setIdLog(nextId);
                newLogUsr.setEmail(email != null && !email.trim().isEmpty() ? email : "SYSTEM");

                logs.add(newLog);
                logsGravedad.add(newLogGrav);
                logsUsuario.add(newLogUsr);

                saveLogs(logs);
                saveLogGravedades(logsGravedad);
                saveLogUsuarios(logsUsuario);
                
                String userEmail = email != null && !email.trim().isEmpty() ? email : "SYSTEM";
                String sseMessage = newLog.getFechaHora() + " > " + userEmail + " > " + descripcion;
                if (sseService != null) {
                    sseService.sendEventToEmail("ADMIN_CHANNEL", "admin_log", sseMessage);
                }

            } finally {
                wrt.release();
            }
        } catch (InterruptedException | IOException e) {
            System.err.println("Error saving log: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private List<Log> loadLogs() throws IOException {
        File file = new File(logsFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        Log[] arr = objectMapper.readValue(file, Log[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private List<LogGravedad> loadLogGravedades() throws IOException {
        File file = new File(logGravedadFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        LogGravedad[] arr = objectMapper.readValue(file, LogGravedad[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private List<LogUsuario> loadLogUsuarios() throws IOException {
        File file = new File(logUsuarioFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        LogUsuario[] arr = objectMapper.readValue(file, LogUsuario[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private void saveLogs(List<Log> logs) throws IOException {
        objectMapper.writeValue(new File(logsFile), logs);
    }

    private void saveLogGravedades(List<LogGravedad> data) throws IOException {
        objectMapper.writeValue(new File(logGravedadFile), data);
    }

    private void saveLogUsuarios(List<LogUsuario> logUsuarios) throws IOException {
        objectMapper.writeValue(new File(logUsuarioFile), logUsuarios);
    }

    public List<LogDetailDTO> getAllLogsDetailed() {
        try {
            List<Log> logs = loadLogs();
            List<LogGravedad> logsGravedad = loadLogGravedades();
            List<LogUsuario> logsUsuario = loadLogUsuarios();

            List<LogDetailDTO> result = new ArrayList<>();
            for (Log l : logs) {
                int id = l.getIdLog();
                LogGravedad grav = logsGravedad.stream().filter(g -> g.getIdLog() == id).findFirst().orElse(null);
                LogUsuario usr = logsUsuario.stream().filter(u -> u.getIdLog() == id).findFirst().orElse(null);

                int idGrav = grav != null ? grav.getIdGravedad() : 0;
                String email = usr != null ? usr.getEmail() : "SYSTEM";

                result.add(new LogDetailDTO(
                    l.getIdLog(),
                    l.getDescripcion(),
                    l.getFechaHora(),
                    l.getTipoEntidad(),
                    l.getEntidadId(),
                    idGrav,
                    email
                ));
            }
            return result;
        } catch (IOException e) {
            System.err.println("Error reading logs: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
