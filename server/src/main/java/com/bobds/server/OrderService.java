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
/* servicio logico de ordenes */
public class OrderService {
    /* dependencias y rutas locales */
    private final String ordersFile = "../data/ordenes.json";
    private final String orderUnitFile = "../data/ordenUnidad.json";
    private final ObjectMapper objectMapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    private final Semaphore wrt = new Semaphore(1);
    private final Semaphore mutex = new Semaphore(1);
    private int readCount = 0;

    public void acquireRead() {
        try {
            mutex.acquire();
            readCount++;
            if (readCount == 1) wrt.acquire();
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operacion interrumpida", e);
        }
    }

    public void releaseRead() {
        try {
            mutex.acquire();
            readCount--;
            if (readCount == 0) wrt.release();
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void acquireWrite() {
        try {
            wrt.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operacion interrumpida", e);
        }
    }

    public void releaseWrite() {
        wrt.release();
    }

    @Autowired
    private RobotClient robotClient;



    @Autowired
    private SseService sseService;

    @Autowired
    private LogService logService;

    /* alta y emision de ordenes */
    public String registerOrder(RegisterOrderDTO data, String email) {
        if (data.getUnitId() == null || data.getUnitId().trim().isEmpty()) {
            return "Error: Unit ID is required.";
        }
        if (data.getUnitId().length() > 20) {
            return "Error: Unit ID cannot exceed 20 characters.";
        }
        if (data.getCommand() == null || data.getCommand().trim().isEmpty()) {
            return "Error: Command cannot be empty.";
        }
        if (data.getCommand().length() > 50) {
            return "Error: Command cannot exceed 50 characters.";
        }
        if (data.getNotes() != null && data.getNotes().length() > 200) {
            return "Error: Notes cannot exceed 200 characters.";
        }

        acquireWrite();
        boolean success = false;
        int assignedId = -1;
        try {
            List<Order> allOrders = loadOrdersInternal();

            int maxId = 0;
            for (Order o : allOrders) {
                if (o.getOrderId() > maxId) {
                    maxId = o.getOrderId();
                }
            }
            assignedId = maxId + 1;

            Order newOrder = new Order();
            newOrder.setOrderId(assignedId);
            newOrder.setCommand(data.getCommand());
            newOrder.setStatus("En Cola");
            newOrder.setNotas(data.getNotes() != null ? data.getNotes() : "");
            newOrder.setFechaHora(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            newOrder.setCreatedAtMs(System.currentTimeMillis());
            newOrder.setUserEmail(email);

            allOrders.add(newOrder);
            saveOrders(allOrders);

            List<OrderUnit> links = loadOrderUnits();
            OrderUnit newLink = new OrderUnit();
            newLink.setOrderId(assignedId);
            newLink.setUnitId(data.getUnitId());

            links.add(newLink);
            saveOrderUnits(links);

            success = true;
        } catch (IOException e) {
            return "Internal error saving order: " + e.getMessage();
        } finally {
            releaseWrite();
        }

        if (success) {
            notifyUsersAboutOrderUpdate(data.getUnitId());
            try { logService.registerLog(email, 1, "Orden registrada: " + assignedId, "Orden", String.valueOf(assignedId)); } catch (Exception ignore) {}
            try {
                robotClient.enviarOrden(data.getUnitId(), assignedId, data.getCommand());
            } catch (Exception e) {
                System.err.println("Warning: Could not connect to robot simulator: " + e.getMessage());
            }
            return "OK";
        }

        return "Error: operation not completed.";
    }

    /* actualizacion de estado */
    public String changeOrderStatusById(int orderId, String newStatus) {
        acquireWrite();
        try {
            List<Order> orders = loadOrdersInternal();
            boolean found = false;

            for (Order o : orders) {
                if (o.getOrderId() == orderId) {
                    o.setStatus(newStatus);
                    if ("FINALIZADA".equalsIgnoreCase(newStatus)) {
                        o.setFinishedAtMs(System.currentTimeMillis());
                        if (o.getCreatedAtMs() != null) {
                            o.setDurationMs(o.getFinishedAtMs() - o.getCreatedAtMs());
                        }
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                return "Error: No order found with ID " + orderId;
            }

            saveOrders(orders);

            String unitId = getUnitIdByOrderId(orderId);
            if (unitId != null) {
                notifyUsersAboutOrderUpdate(unitId);
            }

            try { logService.registerLog("SYSTEM", 2, "Estado de orden modificado a " + newStatus + ": " + orderId, "Orden", String.valueOf(orderId)); } catch (Exception ignore) {}

            return "OK";
        } catch (IOException e) {
            return "Internal error changing order status: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    public String cancelOrder(int orderId, String email) {
        acquireWrite();
        try {
            List<Order> orders = loadOrdersInternal();
            boolean found = false;

            for (Order o : orders) {
                if (o.getOrderId() == orderId) {
                    if ("En Cola".equalsIgnoreCase(o.getStatus()) || "En Curso".equalsIgnoreCase(o.getStatus())) {
                        o.setStatus("CANCELADA");
                        o.setFinishedAtMs(System.currentTimeMillis());
                        if (o.getCreatedAtMs() != null) {
                            o.setDurationMs(o.getFinishedAtMs() - o.getCreatedAtMs());
                        }
                        found = true;
                    } else {
                        return "Error: Order cannot be cancelled in state " + o.getStatus();
                    }
                    break;
                }
            }

            if (!found) {
                return "Error: No cancelable order found with ID " + orderId;
            }

            saveOrders(orders);

            String unitId = getUnitIdByOrderId(orderId);
            if (unitId != null) {
                notifyUsersAboutOrderUpdate(unitId);
            }

            robotClient.cancelOrder(orderId);

            try { logService.registerLog(email, 2, "Orden " + orderId + " cancelada por usuario", "Orden", String.valueOf(orderId)); } catch (Exception ignore) {}

            return "OK";
        } catch (IOException e) {
            return "Internal error cancelling order: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    /* consulta de ordenes publicas */
    public List<Order> getAllOrders(String email) {
        acquireRead();
        try {
            return loadOrdersInternal();
        } catch (IOException e) {
            System.err.println("Error fetching all orders: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            releaseRead();
            try { logService.registerLog(email, 4, "Consulta global de ordenes", "Orden", null); } catch (Exception ignore) {}
        }
    }

    public List<Order> getSystemOrders() {
        acquireRead();
        try {
            return loadOrdersInternal();
        } catch (IOException e) {
            return new ArrayList<>();
        } finally {
            releaseRead();
        }
    }

    /* persistencia interna json */
    private List<Order> loadOrdersInternal() throws IOException {
        File file = new File(ordersFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        Order[] arr = objectMapper.readValue(file, Order[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    /* eliminacion en cascada */
    public void deleteOrdersByUnit(String unitId) {
        acquireWrite();
        try {
            List<OrderUnit> links = loadOrderUnits();
            List<Integer> ordersToDelete = links.stream()
                .filter(v -> v.getUnitId() != null && v.getUnitId().equals(unitId))
                .map(OrderUnit::getOrderId)
                .toList();

            if (!ordersToDelete.isEmpty()) {
                List<Order> orders = loadOrdersInternal();
                orders.removeIf(o -> ordersToDelete.contains(o.getOrderId()));
                saveOrders(orders);

                links.removeIf(v -> v.getUnitId() != null && v.getUnitId().equals(unitId));
                saveOrderUnits(links);
            }
        } catch (IOException e) {
            System.err.println("Error cascading delete on orders: " + e.getMessage());
        } finally {
            releaseWrite();
        }
    }

    /* consultas por unidad */
    public List<Order> getOrdersByUnit(String unitId, String email) {
        acquireRead();
        try {
            List<OrderUnit> links = loadOrderUnits();
            List<Integer> ids = links.stream()
                .filter(v -> v.getUnitId() != null && v.getUnitId().equals(unitId))
                .map(OrderUnit::getOrderId)
                .toList();

            List<Order> all = loadOrdersInternal();
            return all.stream()
                .filter(o -> ids.contains(o.getOrderId()))
                .toList();
        } catch (IOException e) {
            System.err.println("Error fetching orders by unit: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            releaseRead();
            try { logService.registerLog(email, 4, "Consulta ordenes de unidad: " + unitId, "Unidad", unitId); } catch (Exception ignore) {}
        }
    }

    public Order getOrderById(int orderId) {
        acquireRead();
        try {
            return loadOrdersInternal().stream().filter(o -> o.getOrderId() == orderId).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        } finally {
            releaseRead();
        }
    }

    /* eliminacion individual */
    public String deleteOrder(int orderId, String email) {
        acquireWrite();
        try {
            List<Order> orders = loadOrdersInternal();
            boolean removed = orders.removeIf(o -> o.getOrderId() == orderId);
            if (removed) {
                saveOrders(orders);
            }

            List<OrderUnit> links = loadOrderUnits();
            boolean removedLink = links.removeIf(v -> v.getOrderId() == orderId);
            if (removedLink) {
                saveOrderUnits(links);
            }

            if (!removed && !removedLink) {
                return "Error: Order not found.";
            }
            try { logService.registerLog(email, 5, "Orden eliminada: " + orderId, "Orden", String.valueOf(orderId)); } catch (Exception ignore) {}
            return "OK";
        } catch (IOException e) {
            return "Internal error deleting order: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    public String forceDeleteOrder(int orderId, String adminEmail) {
        acquireWrite();
        try {
            List<Order> orders = loadOrdersInternal();
            boolean removed = orders.removeIf(o -> o.getOrderId() == orderId);

            List<OrderUnit> links = loadOrderUnits();
            boolean removedLink = links.removeIf(l -> l.getOrderId() == orderId);

            saveOrders(orders);
            saveOrderUnits(links);

            if (!removed && !removedLink) {
                return "Error: Order not found.";
            }
            try { logService.registerLog(adminEmail, 5, "Orden eliminada por admin: " + orderId, "Orden", String.valueOf(orderId)); } catch (Exception ignore) {}
            return "OK";
        } catch (IOException e) {
            return "Internal error force deleting order: " + e.getMessage();
        } finally {
            releaseWrite();
        }
    }

    private List<OrderUnit> loadOrderUnits() throws IOException {
        File file = new File(orderUnitFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        OrderUnit[] arr = objectMapper.readValue(file, OrderUnit[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private String getUnitIdByOrderId(int orderId) {
        try {
            List<OrderUnit> links = loadOrderUnits();
            for (OrderUnit link : links) {
                if (link.getOrderId() == orderId) {
                    return link.getUnitId();
                }
            }
        } catch (IOException e) {
            System.err.println("Error finding unit by order id: " + e.getMessage());
        }
        return null;
    }

    private void saveOrders(List<Order> orders) throws IOException {
        File file = new File(ordersFile);
        objectMapper.writeValue(file, orders);
    }

    private void saveOrderUnits(List<OrderUnit> links) throws IOException {
        File file = new File(orderUnitFile);
        objectMapper.writeValue(file, links);
    }

    /* notificaciones sse */
    private void notifyUsersAboutOrderUpdate(String unitId) {
        try {
            File file = new File("../data/usuarioUnidades.json");
            if (!file.exists() || file.length() == 0) return;
            UserUnit[] arr = objectMapper.readValue(file, UserUnit[].class);
            for (UserUnit uu : arr) {
                if (uu.getUnitId() != null && uu.getUnitId().equals(unitId)) {
                    sseService.sendEventToEmail(uu.getEmail(), "order_update", unitId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error notifying order updates: " + e.getMessage());
        }
    }
}
