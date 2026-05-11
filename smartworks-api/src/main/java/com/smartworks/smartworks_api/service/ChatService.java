package com.smartworks.smartworks_api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartworks.smartworks_api.ai.OpenAiClient;
import com.smartworks.smartworks_api.dto.ChatRequest;
import com.smartworks.smartworks_api.dto.ChatResponse;
import com.smartworks.smartworks_api.dto.CreateOrderRequest;
import com.smartworks.smartworks_api.dto.CreateOrderRequest.Line;
import com.smartworks.smartworks_api.entity.Customer;
import com.smartworks.smartworks_api.entity.Order;
import com.smartworks.smartworks_api.entity.OrderLine;
import com.smartworks.smartworks_api.entity.Product;
import com.smartworks.smartworks_api.repository.CustomerRepository;
import com.smartworks.smartworks_api.repository.OrderRepository;
import com.smartworks.smartworks_api.repository.ProductRepository;

@Service
public class ChatService {

    private final OpenAiClient openAi;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final OrderService orderService;
    private final ObjectMapper om = new ObjectMapper();

    private final Map<String, List<ChatMemoryMessage>> conversations = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_MESSAGES = 12;

    public ChatService(
            OpenAiClient openAi,
            ProductRepository productRepo,
            OrderRepository orderRepo,
            CustomerRepository customerRepo,
            OrderService orderService) {
        this.openAi = openAi;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.orderService = orderService;
    }

    public ChatResponse handle(ChatRequest req) {
        try {
            String conversationId = resolveConversationId(req);
            String userMessage = (req.getMessage() == null) ? "" : req.getMessage().trim();

            if (userMessage.isBlank()) {
                String greeting = "Hola, soy el asistente virtual de SmartWorks. Puedo ayudarte con inventario, clientes y pedidos.";
                addMessageToHistory(conversationId, "assistant", greeting);
                return new ChatResponse(greeting);
            }

            if (isGreeting(userMessage)) {
                String greeting = "Hola, soy el asistente virtual de SmartWorks. Puedo ayudarte con stock, clientes, pedidos y resúmenes del sistema.";
                addMessageToHistory(conversationId, "user", userMessage);
                addMessageToHistory(conversationId, "assistant", greeting);
                return new ChatResponse(greeting);
            }

            String normalizedMessage = normalizeUserMessage(userMessage);
            List<ChatMemoryMessage> history = getConversationHistory(conversationId);

            List<Map<String, Object>> tools = List.of(
                    Map.of(
                            "type", "function",
                            "name", "get_low_stock_products",
                            "description", "Devuelve productos con stock bajo.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)),
                    Map.of(
                            "type", "function",
                            "name", "get_out_of_stock_products",
                            "description", "Devuelve productos agotados o sin stock.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)),
                    Map.of(
                            "type", "function",
                            "name", "get_products_summary",
                            "description", "Devuelve un resumen general del inventario.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)),
                    Map.of(
                            "type", "function",
                            "name", "get_recent_orders",
                            "description", "Devuelve los 5 pedidos más recientes.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)),
                    Map.of(
                            "type", "function",
                            "name", "get_month_orders_summary",
                            "description", "Devuelve un resumen de pedidos del mes actual.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)),
                    Map.of(
                            "type", "function",
                            "name", "find_customer_by_name",
                            "description", "Busca clientes por nombre.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "name", Map.of(
                                                    "type", "string",
                                                    "description", "Nombre o parte del nombre del cliente")),
                                    "required", List.of("name"),
                                    "additionalProperties", false)),
                    Map.of(
                            "type", "function",
                            "name", "find_product_by_name",
                            "description", "Busca productos por nombre.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "name", Map.of(
                                                    "type", "string",
                                                    "description", "Nombre o parte del nombre del producto")),
                                    "required", List.of("name"),
                                    "additionalProperties", false)),
                    Map.of(
                            "type", "function",
                            "name", "create_customer",
                            "description", "Crea un nuevo cliente.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "name", Map.of("type", "string"),
                                            "email", Map.of("type", "string"),
                                            "phone", Map.of("type", "string"),
                                            "address", Map.of("type", "string")),
                                    "required", List.of("name"),
                                    "additionalProperties", false)),
                    Map.of(
                            "type", "function",
                            "name", "create_order",
                            "description", "Crea un nuevo pedido para un cliente. Necesita customerId y líneas con productId y quantity.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "customerId", Map.of("type", "integer"),
                                            "lines", Map.of(
                                                    "type", "array",
                                                    "items", Map.of(
                                                            "type", "object",
                                                            "properties", Map.of(
                                                                    "productId", Map.of("type", "integer"),
                                                                    "quantity", Map.of("type", "integer")),
                                                            "required", List.of("productId", "quantity"),
                                                            "additionalProperties", false))),
                                    "required", List.of("customerId", "lines"),
                                    "additionalProperties", false)));

            String systemPrompt =
                    "Eres el asistente virtual de SmartWorks. " +
                    "Responde siempre en español con tono profesional, claro y breve. " +
                    "Puedes consultar inventario, pedidos y clientes, y también crear clientes y pedidos. " +
                    "Debes tener en cuenta el contexto reciente de la conversación. " +
                    "Si el usuario responde con frases como 'sí', 'vale', 'hazlo', 'adelante', 'ok' o similares, " +
                    "interpreta esa respuesta según la última pregunta o acción pendiente del contexto. " +
                    "No inventes datos. " +
                    "Si faltan datos para crear algo, indícalo claramente. " +
                    "Si una búsqueda devuelve varios resultados, muéstralos de forma ordenada. " +
                    "Si una operación de creación se completa, confirma el resultado de forma clara. " +
                    "Para stock bajo usa get_low_stock_products. " +
                    "Para agotados usa get_out_of_stock_products. " +
                    "Para inventario general usa get_products_summary. " +
                    "Para pedidos recientes usa get_recent_orders. " +
                    "Para pedidos del mes usa get_month_orders_summary. " +
                    "Para localizar clientes usa find_customer_by_name. " +
                    "Para localizar productos usa find_product_by_name. " +
                    "Para crear clientes usa create_customer. " +
                    "Para crear pedidos usa create_order.";

            Map<String, Object> body1 = new HashMap<>();
            body1.put("model", "gpt-4.1-mini");
            body1.put("tool_choice", "auto");
            body1.put("tools", tools);
            body1.put("input", buildInputWithHistory(systemPrompt, history, normalizedMessage));

            addMessageToHistory(conversationId, "user", normalizedMessage);

            Map<String, Object> r1 = openAi.createResponse(body1);

            String direct = extractText(r1);
            Map<String, Object> fc = findFunctionCall(r1);

            if (fc == null) {
                String fallback = (direct == null || direct.isBlank())
                        ? "No he podido interpretar la consulta. Puedes preguntarme por inventario, clientes o pedidos."
                        : direct;
                fallback = limitResponseLength(fallback);
                addMessageToHistory(conversationId, "assistant", fallback);
                return new ChatResponse(fallback);
            }

            String responseId = stringValue(r1.get("id"));
            String callId = stringValue(fc.get("call_id"));
            String fnName = stringValue(fc.get("name"));
            String argumentsJson = stringValue(fc.get("arguments"));

            Object toolResult;

            switch (fnName) {
                case "get_low_stock_products":
                    toolResult = getLowStockProducts();
                    break;
                case "get_out_of_stock_products":
                    toolResult = getOutOfStockProducts();
                    break;
                case "get_products_summary":
                    toolResult = getProductsSummary();
                    break;
                case "get_recent_orders":
                    toolResult = getRecentOrders();
                    break;
                case "get_month_orders_summary":
                    toolResult = getMonthOrdersSummary();
                    break;
                case "find_customer_by_name":
                    toolResult = findCustomerByName(argumentsJson);
                    break;
                case "find_product_by_name":
                    toolResult = findProductByName(argumentsJson);
                    break;
                case "create_customer":
                    toolResult = createCustomer(argumentsJson);
                    break;
                case "create_order":
                    toolResult = createOrder(argumentsJson);
                    break;
                default:
                    toolResult = Map.of("error", "Función no implementada: " + fnName);
                    break;
            }

            Map<String, Object> body2 = new HashMap<>();
            body2.put("model", "gpt-4.1-mini");
            body2.put("previous_response_id", responseId);
            body2.put("tools", tools);
            body2.put(
                    "instructions",
                    "Redacta la respuesta en español con estilo profesional y natural. " +
                    "Usa frases claras. " +
                    "Si hay varios elementos, usa lista con guiones. " +
                    "Si no hay datos, dilo de forma elegante. " +
                    "Si la operación ha creado un cliente o un pedido, confírmalo claramente. " +
                    "Ten en cuenta el contexto reciente de la conversación. " +
                    "No menciones JSON, herramientas, funciones ni base de datos. " +
                    "Máximo aproximado de 120 palabras. " +
                    "Cuando encaje, termina con una frase breve de apoyo.");
            body2.put("input", List.of(
                    Map.of(
                            "type", "function_call_output",
                            "call_id", callId,
                            "output", toJson(toolResult))));

            Map<String, Object> r2 = openAi.createResponse(body2);

            String finalText = extractText(r2);
            if (finalText == null || finalText.isBlank()) {
                String fallback = "He procesado la consulta, pero no he podido generar una respuesta en este momento.";
                addMessageToHistory(conversationId, "assistant", fallback);
                return new ChatResponse(fallback);
            }

            finalText = limitResponseLength(finalText);
            addMessageToHistory(conversationId, "assistant", finalText);
            return new ChatResponse(finalText);

        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse(
                    "Se ha producido un error al procesar la consulta. Inténtalo de nuevo en unos segundos.");
        }
    }

    private String resolveConversationId(ChatRequest req) {
        String conversationId = req.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return "default";
        }
        return conversationId.trim();
    }

    private List<ChatMemoryMessage> getConversationHistory(String conversationId) {
        return conversations.computeIfAbsent(conversationId, k -> new ArrayList<>());
    }

    private void addMessageToHistory(String conversationId, String role, String content) {
        if (content == null || content.isBlank()) return;

        List<ChatMemoryMessage> history = getConversationHistory(conversationId);
        history.add(new ChatMemoryMessage(role, content));

        if (history.size() > MAX_HISTORY_MESSAGES) {
            int removeCount = history.size() - MAX_HISTORY_MESSAGES;
            history.subList(0, removeCount).clear();
        }
    }

    private List<Map<String, Object>> buildInputWithHistory(String systemPrompt, List<ChatMemoryMessage> history, String userMessage) {
        List<Map<String, Object>> input = new ArrayList<>();

        input.add(Map.of(
                "role", "system",
                "content", systemPrompt));

        for (ChatMemoryMessage msg : history) {
            input.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()));
        }

        input.add(Map.of(
                "role", "user",
                "content", userMessage));

        return input;
    }

    public void clearConversation(String conversationId) {
        conversations.remove(conversationId);
    }

    private boolean isGreeting(String message) {
        String msg = message.toLowerCase().trim();
        return msg.equals("hola")
                || msg.equals("buenas")
                || msg.equals("buenos dias")
                || msg.equals("buenos días")
                || msg.equals("buenas tardes")
                || msg.equals("buenas noches")
                || msg.equals("hey")
                || msg.equals("holi");
    }

    private String normalizeUserMessage(String message) {
        if (message == null)
            return "";

        String normalized = message.trim().toLowerCase();

        normalized = normalized.replace("almacen", "inventario");
        normalized = normalized.replace("almacén", "inventario");
        normalized = normalized.replace("existencias", "stock");
        normalized = normalized.replace("fuera de stock", "sin stock");
        normalized = normalized.replace("no quedan", "agotados");
        normalized = normalized.replace("ultimos pedidos", "pedidos recientes");
        normalized = normalized.replace("últimos pedidos", "pedidos recientes");
        normalized = normalized.replace("pedidos nuevos", "pedidos recientes");
        normalized = normalized.replace("actividad del mes", "resumen de pedidos del mes");

        return normalized;
    }

    private String limitResponseLength(String text) {
        if (text == null || text.isBlank())
            return text;
        int maxLength = 700;
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength).trim() + "...";
    }

    private Object getLowStockProducts() {
        List<Product> low = productRepo.findLowStock();
        return low.stream().map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "stock", p.getStock(),
                "lowStockThreshold", p.getLowStockThreshold(),
                "price", p.getPrice())).toList();
    }

    private Object getOutOfStockProducts() {
        List<Product> out = productRepo.findOutOfStock();
        return out.stream().map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "stock", p.getStock(),
                "price", p.getPrice())).toList();
    }

    private Object getProductsSummary() {
        return Map.of(
                "totalProducts", productRepo.countAllProducts(),
                "availableProducts", productRepo.countAvailableProducts(),
                "lowStockProducts", productRepo.countLowStock(),
                "outOfStockProducts", productRepo.countOutOfStock());
    }

    private Object getRecentOrders() {
        List<Order> orders = orderRepo.findTop5ByOrderByCreatedAtDesc();

        return orders.stream().map(o -> Map.of(
                "id", o.getId(),
                "customer", o.getCustomer() != null ? o.getCustomer().getName() : "Sin cliente",
                "status", o.getStatus() != null ? o.getStatus().name() : "UNKNOWN",
                "createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "",
                "linesCount", o.getLines() != null ? o.getLines().size() : 0,
                "products", o.getLines() == null ? List.of()
                        : o.getLines().stream()
                                .map(OrderLine::getProduct)
                                .filter(p -> p != null)
                                .map(Product::getName)
                                .toList()))
                .toList();
    }

    private Object getMonthOrdersSummary() {
        LocalDate now = LocalDate.now();
        LocalDateTime from = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = now.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        return Map.of(
                "month", now.getMonth().name(),
                "totalOrders", orderRepo.countByCreatedAtBetween(from, to),
                "pendingOrders", orderRepo.countByStatusAndCreatedAtBetween(Order.Status.PENDING, from, to),
                "completedOrders", orderRepo.countByStatusAndCreatedAtBetween(Order.Status.COMPLETED, from, to),
                "cancelledOrders", orderRepo.countByStatusAndCreatedAtBetween(Order.Status.CANCELLED, from, to));
    }

    private Object findCustomerByName(String argumentsJson) {
        try {
            Map<String, Object> args = om.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            String name = stringValue(args.get("name"));

            List<Customer> customers = customerRepo.findByNameContainingIgnoreCase(name);

            return customers.stream().map(c -> Map.of(
                    "id", c.getId(),
                    "name", c.getName())).toList();
        } catch (Exception e) {
            return Map.of("error", "No se pudo buscar el cliente.");
        }
    }

    private Object findProductByName(String argumentsJson) {
        try {
            Map<String, Object> args = om.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            String name = stringValue(args.get("name"));

            List<Product> products = productRepo.findByNameContainingIgnoreCase(name);

            return products.stream().map(p -> Map.of(
                    "id", p.getId(),
                    "name", p.getName(),
                    "price", p.getPrice(),
                    "stock", p.getStock())).toList();
        } catch (Exception e) {
            return Map.of("error", "No se pudo buscar el producto.");
        }
    }

    private Object createCustomer(String argumentsJson) {
        try {
            Map<String, Object> args = om.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});

            String name = stringValue(args.get("name"));

            if (name.isBlank()) {
                return Map.of("error", "Falta el nombre del cliente.");
            }

            Customer c = new Customer();
            c.setName(name);

            Customer saved = customerRepo.save(c);

            return Map.of(
                    "success", true,
                    "id", saved.getId(),
                    "name", saved.getName());
        } catch (Exception e) {
            return Map.of("error", "No se pudo crear el cliente.");
        }
    }

    private Object createOrder(String argumentsJson) {
        try {
            Map<String, Object> args = om.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});

            Long customerId = toLong(args.get("customerId"));
            if (customerId == null) {
                return Map.of("error", "Falta el customerId.");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> linesInput = (List<Map<String, Object>>) args.get("lines");

            if (linesInput == null || linesInput.isEmpty()) {
                return Map.of("error", "El pedido debe incluir al menos una línea.");
            }

            CreateOrderRequest request = new CreateOrderRequest();
            request.setCustomerId(customerId);

            List<Line> lines = linesInput.stream().map(item -> {
                Line l = new Line();
                l.setProductId(toLong(item.get("productId")));
                l.setQuantity(toInt(item.get("quantity")));
                return l;
            }).toList();

            request.setLines(lines);

            Order order = orderService.create(request);

            return Map.of(
                    "success", true,
                    "orderId", order.getId(),
                    "customerId", customerId,
                    "status", order.getStatus() != null ? order.getStatus().name() : "UNKNOWN",
                    "linesCount", order.getLines() != null ? order.getLines().size() : 0);
        } catch (Exception e) {
            return Map.of("error", "No se pudo crear el pedido.");
        }
    }

    private Long toLong(Object value) {
        try {
            if (value == null)
                return null;
            if (value instanceof Number n)
                return n.longValue();
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object value) {
        try {
            if (value == null)
                return null;
            if (value instanceof Number n)
                return n.intValue();
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findFunctionCall(Map<String, Object> resp) {
        Object outObj = resp.get("output");
        if (!(outObj instanceof List<?> output))
            return null;

        for (Object o : output) {
            if (!(o instanceof Map<?, ?> m))
                continue;
            Map<String, Object> item = (Map<String, Object>) m;
            if ("function_call".equals(item.get("type"))) {
                return item;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> resp) {
        Object agg = resp.get("output_text");
        if (agg instanceof String s && !s.isBlank())
            return s;

        Object outObj = resp.get("output");
        if (!(outObj instanceof List<?> output))
            return null;

        for (Object o : output) {
            if (!(o instanceof Map<?, ?> m))
                continue;
            Map<String, Object> item = (Map<String, Object>) m;

            if ("message".equals(item.get("type"))) {
                Object contentObj = item.get("content");
                if (contentObj instanceof List<?> contentList) {
                    for (Object c : contentList) {
                        if (!(c instanceof Map<?, ?> cm0))
                            continue;
                        Map<String, Object> cm = (Map<String, Object>) cm0;
                        if ("output_text".equals(cm.get("type")) && cm.get("text") != null) {
                            return cm.get("text").toString();
                        }
                    }
                }
            }

            if ("output_text".equals(item.get("type")) && item.get("text") != null) {
                return item.get("text").toString();
            }
        }

        return null;
    }
}