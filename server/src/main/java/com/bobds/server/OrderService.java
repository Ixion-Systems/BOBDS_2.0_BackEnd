package com.bobds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class OrderService {
    private final String ordersFile = "../data/ordenes.json";
    private final String orderUnitFile = "../data/ordenUnidad.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Autowired
    private RobotClient robotClient;

    public String registerOrder(RegisterOrderDTO data) {
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

        lock.writeLock().lock();
        boolean success = false;
        try {
            List<Order> allOrders = loadOrdersInternal();
            
            int maxId = 0;
            for (Order o : allOrders) {
                if (o.getOrderId() > maxId) {
                    maxId = o.getOrderId();
                }
            }
            int nextId = maxId + 1;

            Order newOrder = new Order();
            newOrder.setOrderId(nextId);
            newOrder.setCommand(data.getCommand());
            newOrder.setNotes(data.getNotes() != null ? data.getNotes() : "");
            newOrder.setStatus("En Cola");
            
            allOrders.add(newOrder);
            saveOrders(allOrders);

            List<OrderUnit> links = loadOrderUnits();
            OrderUnit newLink = new OrderUnit();
            newLink.setOrderId(nextId);
            newLink.setUnitId(data.getUnitId());
            
            links.add(newLink);
            saveOrderUnits(links);

            success = true;
        } catch (IOException e) {
            return "Internal error saving order: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }

        if (success) {
            try {
                robotClient.enviarOrden(data.getUnitId(), data.getCommand());
            } catch (Exception e) {
                System.err.println("Warning: Could not connect to robot simulator: " + e.getMessage());
            }
            return "OK";
        }
        
        return "Error: operation not completed.";
    }

    public String changeOrderStatus(String unitId, String newStatus) {
        lock.writeLock().lock();
        try {
            List<Order> orders = loadOrdersInternal();
            List<OrderUnit> links = loadOrderUnits();

            boolean found = false;
            for (OrderUnit ou : links) {
                if (ou.getUnitId() != null && ou.getUnitId().equals(unitId)) {
                    for (Order o : orders) {
                        if (o.getOrderId() == ou.getOrderId() && "En Cola".equals(o.getStatus())) {
                            o.setStatus(newStatus);
                            found = true;
                            break;
                        }
                    }
                }
                if (found) break;
            }

            if (!found) {
                return "Error: No active order found for unit " + unitId;
            }

            saveOrders(orders);
            return "OK";
        } catch (IOException e) {
            return "Internal error changing order status: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Order> loadOrders() {
        lock.readLock().lock();
        try {
            return loadOrdersInternal();
        } catch (IOException e) {
            System.err.println("Error reading orders file: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private List<Order> loadOrdersInternal() throws IOException {
        File file = new File(ordersFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        Order[] arr = objectMapper.readValue(file, Order[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    public void deleteOrdersByUnit(String unitId) {
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
    }

    public List<Order> getOrdersByUnit(String unitId) {
        lock.readLock().lock();
        try {
            List<OrderUnit> links = loadOrderUnits();
            List<Integer> ids = links.stream()
                .filter(v -> v.getUnitId() != null && v.getUnitId().equals(unitId))
                .map(OrderUnit::getOrderId)
                .toList();

            List<Order> all = loadOrdersInternal();
            return all.stream()
                .filter(o -> ids.contains(o.getOrderId()))
                .map(o -> {
                    Order mapped = new Order();
                    mapped.setOrderId(o.getOrderId());
                    mapped.setCommand(o.getCommand());
                    mapped.setStatus(o.getStatus());
                    return mapped;
                })
                .toList();
        } catch (IOException e) {
            System.err.println("Error fetching orders by unit: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String deleteOrder(int orderId) {
        lock.writeLock().lock();
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
            return "OK";
        } catch (IOException e) {
            return "Internal error deleting order: " + e.getMessage();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<OrderUnit> loadOrderUnits() throws IOException {
        File file = new File(orderUnitFile);
        if (!file.exists() || file.length() == 0) return new ArrayList<>();
        OrderUnit[] arr = objectMapper.readValue(file, OrderUnit[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    private void saveOrders(List<Order> orders) throws IOException {
        File file = new File(ordersFile);
        objectMapper.writeValue(file, orders);
    }

    private void saveOrderUnits(List<OrderUnit> links) throws IOException {
        File file = new File(orderUnitFile);
        objectMapper.writeValue(file, links);
    }
}
