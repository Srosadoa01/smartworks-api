package com.smartworks.smartworks_api.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import com.smartworks.smartworks_api.dto.MonthlyAnalyticsResponseV2;
import com.smartworks.smartworks_api.entity.GeneratedReport;
import com.smartworks.smartworks_api.repository.GeneratedReportRepository;

@Service
public class ReportService {

    private static final Color NAVY = new Color(6, 26, 45);
    private static final Color BLUE = new Color(26, 166, 255);
    private static final Color GREEN = new Color(0, 150, 136);
    private static final Color ORANGE = new Color(255, 138, 0);
    private static final Color RED = new Color(211, 47, 47);
    private static final Color PURPLE = new Color(124, 77, 255);

    private static final Color PAGE_BG = new Color(244, 248, 252);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color SOFT_BLUE = new Color(234, 246, 255);
    private static final Color SOFT_GREEN = new Color(230, 247, 245);
    private static final Color SOFT_ORANGE = new Color(255, 244, 229);
    private static final Color SOFT_RED = new Color(255, 235, 238);
    private static final Color SOFT_PURPLE = new Color(240, 234, 255);

    private final AnalyticsServiceV2 analyticsService;
    private final GeneratedReportRepository reportRepo;

    public ReportService(
            AnalyticsServiceV2 analyticsService,
            GeneratedReportRepository reportRepo
    ) {
        this.analyticsService = analyticsService;
        this.reportRepo = reportRepo;
    }

    public GeneratedReport generateAndSaveMonthlyReport(Integer year, Integer month) {
        MonthlyAnalyticsResponseV2 data = analyticsService.monthly(year, month);
        byte[] pdf = buildPdf(data);

        GeneratedReport rep = new GeneratedReport();
        rep.setYear(data.year);
        rep.setMonth(data.month);
        rep.setCreatedAt(LocalDateTime.now());
        rep.setFilename("reporte_smartworks_" + data.year + "_" + String.format("%02d", data.month) + ".pdf");
        rep.setPdfBytes(pdf);

        return reportRepo.save(rep);
    }

    public GeneratedReport getOrThrow(Long id) {
        return reportRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado: " + id));
    }

    private byte[] buildPdf(MonthlyAnalyticsResponseV2 data) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Pdf pdf = new Pdf(doc);

            drawCover(pdf, data);
            drawDashboard(pdf, data);
            drawOrders(pdf, data);
            drawProductsAndRecommendations(pdf, data);

            pdf.close();
            drawFooters(pdf, data);

            doc.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }

    private void drawCover(Pdf pdf, MonthlyAnalyticsResponseV2 data) throws Exception {
        pdf.newPage();

        pdf.roundRect(36, 36, 523, 770, 28, CARD);
        pdf.roundRect(36, 642, 523, 164, 28, NAVY);

        pdf.text("SmartWorks", 64, 752, PDType1Font.HELVETICA_BOLD, 30, Color.WHITE);
        pdf.text("Reporte mensual de actividad", 64, 718, PDType1Font.HELVETICA_BOLD, 18, new Color(183, 200, 216));

        pdf.roundRect(64, 668, 190, 36, 18, BLUE);
        pdf.text(monthName(data.month) + " " + data.year, 84, 680, PDType1Font.HELVETICA_BOLD, 13, Color.WHITE);

        pdf.roundRect(432, 704, 88, 58, 18, new Color(12, 43, 70));
        pdf.text("PDF", 460, 727, PDType1Font.HELVETICA_BOLD, 17, Color.WHITE);

        pdf.text("Resumen ejecutivo", 64, 592, PDType1Font.HELVETICA_BOLD, 22, NAVY);

        pdf.paragraph(
                "Este informe resume la actividad mensual de SmartWorks a partir de los datos registrados en la API: pedidos, facturacion, productos vendidos, estados de pedidos y recomendaciones operativas.",
                64,
                562,
                460,
                13,
                PDType1Font.HELVETICA,
                MUTED
        );

        pdf.roundRect(64, 430, 460, 84, 22, SOFT_BLUE);
        pdf.text("Periodo analizado", 92, 482, PDType1Font.HELVETICA_BOLD, 12, NAVY);
        pdf.text(monthName(data.month) + " de " + data.year, 92, 456, PDType1Font.HELVETICA_BOLD, 20, BLUE);
        pdf.text(
                "Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                92,
                438,
                PDType1Font.HELVETICA,
                10,
                MUTED
        );

        pdf.text("Indicadores destacados", 64, 376, PDType1Font.HELVETICA_BOLD, 17, NAVY);

        pdf.coverMetric(64, 296, 210, 64, "Pedidos", String.valueOf(data.ordersThisMonth), BLUE, SOFT_BLUE);
        pdf.coverMetric(314, 296, 210, 64, "Facturacion", money(data.revenueThisMonth), GREEN, SOFT_GREEN);
        pdf.coverMetric(64, 210, 210, 64, "Palets vendidos", String.valueOf(data.unitsSoldThisMonth), ORANGE, SOFT_ORANGE);
        pdf.coverMetric(314, 210, 210, 64, "Ticket medio", money(data.avgOrderValueThisMonth), PURPLE, SOFT_PURPLE);

        pdf.text("SmartWorks API - Informe generado automaticamente", 64, 92, PDType1Font.HELVETICA, 10, MUTED);
    }

    private void drawDashboard(Pdf pdf, MonthlyAnalyticsResponseV2 data) throws Exception {
        pdf.newPageWithHeader(
                "Indicadores principales",
                "Resumen de ventas y actividad mensual"
        );

        float y = 650;

        pdf.bigKpi(
                50,
                y,
                235,
                115,
                "Pedidos del mes",
                String.valueOf(data.ordersThisMonth),
                percent(data.ordersChangePct) + " frente al mes anterior",
                BLUE,
                SOFT_BLUE
        );

        pdf.bigKpi(
                310,
                y,
                235,
                115,
                "Facturacion",
                money(data.revenueThisMonth),
                percent(data.revenueChangePct) + " frente al mes anterior",
                GREEN,
                SOFT_GREEN
        );

        y -= 140;

        pdf.bigKpi(
                50,
                y,
                235,
                115,
                "Palets vendidos",
                String.valueOf(data.unitsSoldThisMonth),
                "Unidades totales registradas",
                ORANGE,
                SOFT_ORANGE
        );

        pdf.bigKpi(
                310,
                y,
                235,
                115,
                "Ticket medio",
                money(data.avgOrderValueThisMonth),
                "Media de facturacion por pedido",
                PURPLE,
                SOFT_PURPLE
        );

        y -= 160;

        pdf.text("Lectura rapida", 50, y, PDType1Font.HELVETICA_BOLD, 17, NAVY);
        y -= 34;

        pdf.roundRect(50, y - 120, 495, 120, 24, CARD);
        pdf.strokeRoundRect(50, y - 120, 495, 120, 24, BORDER);

        pdf.paragraph(
                buildExecutiveSummary(data),
                76,
                y - 30,
                445,
                13,
                PDType1Font.HELVETICA,
                MUTED
        );
    }

    private void drawOrders(Pdf pdf, MonthlyAnalyticsResponseV2 data) throws Exception {
        pdf.newPageWithHeader(
                "Estado de pedidos",
                "Distribucion mensual por estado y tiempo medio de entrega"
        );

        long total = data.pendingThisMonth + data.completedThisMonth + data.cancelledThisMonth;

        float y = 650;

        pdf.statusCard(50, y, 150, 82, "Pendientes", data.pendingThisMonth, ORANGE, SOFT_ORANGE);
        pdf.statusCard(222, y, 150, 82, "Completados", data.completedThisMonth, GREEN, SOFT_GREEN);
        pdf.statusCard(394, y, 150, 82, "Cancelados", data.cancelledThisMonth, RED, SOFT_RED);

        y -= 130;

        pdf.roundRect(50, y - 150, 495, 150, 26, CARD);
        pdf.strokeRoundRect(50, y - 150, 495, 150, 26, BORDER);

        pdf.text("Distribucion visual", 76, y - 34, PDType1Font.HELVETICA_BOLD, 15, NAVY);

        if (total == 0) {
            pdf.text("No hay pedidos suficientes para mostrar una distribucion.", 76, y - 72, PDType1Font.HELVETICA, 12, MUTED);
        } else {
            float barX = 76;
            float barY = y - 82;
            float barW = 443;
            float barH = 22;

            pdf.roundRect(barX, barY, barW, barH, 11, new Color(229, 234, 240));

            float pendingW = (float) (barW * data.pendingThisMonth / total);
            float completedW = (float) (barW * data.completedThisMonth / total);
            float cancelledW = barW - pendingW - completedW;

            float currentX = barX;

            if (pendingW > 0) {
                pdf.roundRect(currentX, barY, pendingW, barH, 11, ORANGE);
                currentX += pendingW;
            }

            if (completedW > 0) {
                pdf.roundRect(currentX, barY, completedW, barH, 11, GREEN);
                currentX += completedW;
            }

            if (cancelledW > 0) {
                pdf.roundRect(currentX, barY, cancelledW, barH, 11, RED);
            }

            pdf.legend(92, y - 118, "Pendientes", ORANGE, pct(data.pendingThisMonth, total));
            pdf.legend(248, y - 118, "Completados", GREEN, pct(data.completedThisMonth, total));
            pdf.legend(424, y - 118, "Cancelados", RED, pct(data.cancelledThisMonth, total));
        }

        y -= 210;

        pdf.roundRect(50, y - 110, 495, 110, 26, CARD);
        pdf.strokeRoundRect(50, y - 110, 495, 110, 26, BORDER);

        pdf.roundRect(76, y - 80, 58, 58, 20, SOFT_BLUE);
        pdf.text("Entrega", 158, y - 33, PDType1Font.HELVETICA_BOLD, 14, NAVY);
        pdf.text(format1(data.avgDeliveryHoursThisMonth) + " horas", 158, y - 60, PDType1Font.HELVETICA_BOLD, 22, BLUE);
        pdf.text(
                "Variacion frente al mes anterior: " + percent(data.avgDeliveryChangePct),
                158,
                y - 82,
                PDType1Font.HELVETICA,
                11,
                MUTED
        );
    }

    private void drawProductsAndRecommendations(Pdf pdf, MonthlyAnalyticsResponseV2 data) throws Exception {
        pdf.newPageWithHeader(
                "Productos y recomendaciones",
                "Ranking mensual y conclusiones automaticas"
        );

        float y = 650;

        pdf.text("Productos mas vendidos", 50, y, PDType1Font.HELVETICA_BOLD, 17, NAVY);
        y -= 30;

        List<MonthlyAnalyticsResponseV2.TopProduct> products = data.topProductsByUnits;

        if (products == null || products.isEmpty()) {
            pdf.roundRect(50, y - 80, 495, 80, 24, CARD);
            pdf.strokeRoundRect(50, y - 80, 495, 80, 24, BORDER);
            pdf.text("Todavia no hay suficientes productos vendidos para generar un ranking.", 76, y - 43, PDType1Font.HELVETICA, 12, MUTED);
            y -= 115;
        } else {
            long max = products.stream()
                    .mapToLong(p -> p.quantity == null ? 0 : p.quantity)
                    .max()
                    .orElse(1);

            int index = 1;

            for (MonthlyAnalyticsResponseV2.TopProduct product : products) {
                long quantity = product.quantity == null ? 0 : product.quantity;
                float percentage = max == 0 ? 0 : (float) quantity / max;

                pdf.productRow(
                        50,
                        y,
                        495,
                        index,
                        product.name,
                        product.productId,
                        quantity,
                        percentage
                );

                y -= 64;
                index++;

                if (index > 5) break;
            }

            y -= 20;
        }

        pdf.text("Recomendaciones", 50, y, PDType1Font.HELVETICA_BOLD, 17, NAVY);
        y -= 32;

        pdf.recommendation(50, y, 495, "Pedidos", recommendationOrders(data), BLUE, SOFT_BLUE);
        y -= 88;

        pdf.recommendation(50, y, 495, "Facturacion", recommendationRevenue(data), GREEN, SOFT_GREEN);
        y -= 88;

        pdf.recommendation(50, y, 495, "Inventario", recommendationProducts(data), ORANGE, SOFT_ORANGE);
        y -= 88;

        pdf.recommendation(50, y, 495, "Entrega", recommendationDelivery(data), PURPLE, SOFT_PURPLE);
    }

    private void drawFooters(Pdf pdf, MonthlyAnalyticsResponseV2 data) throws Exception {
        int totalPages = pdf.pageCount();

        for (int i = 0; i < totalPages; i++) {
            pdf.footer(
                    i,
                    "SmartWorks - Reporte mensual " + monthName(data.month) + " " + data.year,
                    i + 1,
                    totalPages
            );
        }
    }

    private String buildExecutiveSummary(MonthlyAnalyticsResponseV2 data) {
        StringBuilder sb = new StringBuilder();

        sb.append("Durante ").append(monthName(data.month)).append(" de ").append(data.year)
                .append(", SmartWorks registro ").append(data.ordersThisMonth).append(" pedidos, ")
                .append(data.unitsSoldThisMonth).append(" palets vendidos y una facturacion total de ")
                .append(money(data.revenueThisMonth)).append(". ");

        if (data.ordersChangePct > 0) {
            sb.append("La actividad de pedidos crecio un ").append(percent(data.ordersChangePct))
                    .append(" respecto al mes anterior. ");
        } else if (data.ordersChangePct < 0) {
            sb.append("La actividad de pedidos disminuyo un ").append(percent(Math.abs(data.ordersChangePct)))
                    .append(" respecto al mes anterior. ");
        } else {
            sb.append("La actividad de pedidos se mantuvo estable respecto al mes anterior. ");
        }

        sb.append("El ticket medio fue de ").append(money(data.avgOrderValueThisMonth))
                .append(" y el tiempo medio de entrega fue de ")
                .append(format1(data.avgDeliveryHoursThisMonth)).append(" horas.");

        return sb.toString();
    }

    private String recommendationOrders(MonthlyAnalyticsResponseV2 data) {
        if (data.pendingThisMonth > data.completedThisMonth) {
            return "Hay mas pedidos pendientes que completados. Conviene revisar operaciones abiertas y priorizar su cierre.";
        }

        if (data.ordersThisMonth == 0) {
            return "No se han registrado pedidos en este periodo. Puede ser util revisar la actividad comercial o crear datos de prueba para la demo.";
        }

        return "La gestion de pedidos presenta un estado equilibrado. Mantener seguimiento de pendientes y completados.";
    }

    private String recommendationRevenue(MonthlyAnalyticsResponseV2 data) {
        if (data.revenueChangePct > 0) {
            return "La facturacion ha aumentado respecto al mes anterior. Es buen indicador de crecimiento del periodo.";
        }

        if (data.revenueChangePct < 0) {
            return "La facturacion ha bajado respecto al mes anterior. Conviene revisar ventas, precios y productos con menor salida.";
        }

        return "La facturacion se mantiene estable. Se recomienda seguir comparando la evolucion mensual.";
    }

    private String recommendationProducts(MonthlyAnalyticsResponseV2 data) {
        if (data.topProductsByUnits == null || data.topProductsByUnits.isEmpty()) {
            return "Todavia no hay suficientes datos de productos vendidos para generar recomendaciones de inventario.";
        }

        MonthlyAnalyticsResponseV2.TopProduct top = data.topProductsByUnits.get(0);

        return "El producto con mayor salida es \"" + safe(top.name) + "\". Conviene vigilar su stock y asegurar disponibilidad.";
    }

    private String recommendationDelivery(MonthlyAnalyticsResponseV2 data) {
        if (data.avgDeliveryHoursThisMonth == 0) {
            return "No hay suficientes pedidos completados para calcular un tiempo medio de entrega fiable.";
        }

        if (data.avgDeliveryChangePct > 0) {
            return "El tiempo medio de entrega ha aumentado. Conviene revisar cuellos de botella en pedidos completados.";
        }

        if (data.avgDeliveryChangePct < 0) {
            return "El tiempo medio de entrega ha mejorado respecto al mes anterior.";
        }

        return "El tiempo medio de entrega se mantiene estable.";
    }

    private String money(double v) {
        return String.format(Locale.US, "%.2f EUR", v);
    }

    private String percent(double v) {
        String sign = v > 0 ? "+" : "";
        return sign + String.format(Locale.US, "%.1f%%", v);
    }

    private String pct(long value, long total) {
        if (total == 0) return "0%";
        return Math.round((value * 100.0) / total) + "%";
    }

    private String format1(double v) {
        return String.format(Locale.US, "%.1f", v);
    }

    private String safe(String text) {
        if (text == null || text.isBlank()) return "Sin nombre";
        return text;
    }

    private String monthName(int month) {
        return switch (month) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> "Mes";
        };
    }

    private static class Pdf {

        private final PDDocument doc;
        private PDPage page;
        private PDPageContentStream cs;

        private final float pageWidth = PDRectangle.A4.getWidth();
        private final float pageHeight = PDRectangle.A4.getHeight();

        Pdf(PDDocument doc) {
            this.doc = doc;
        }

        void newPage() throws Exception {
            close();

            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);

            fillRect(0, 0, pageWidth, pageHeight, PAGE_BG);
        }

        void newPageWithHeader(String title, String subtitle) throws Exception {
            newPage();

            roundRect(36, 36, 523, 770, 28, CARD);
            roundRect(36, 696, 523, 110, 28, NAVY);

            text(title, 60, 756, PDType1Font.HELVETICA_BOLD, 23, Color.WHITE);
            text(subtitle, 60, 728, PDType1Font.HELVETICA, 12, new Color(183, 200, 216));

            roundRect(492, 734, 38, 28, 9, BLUE);
            text("SW", 503, 744, PDType1Font.HELVETICA_BOLD, 10, Color.WHITE);
        }

        int pageCount() {
            return doc.getNumberOfPages();
        }

        void close() throws Exception {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }

        void footer(int pageIndex, String title, int pageNumber, int totalPages) throws Exception {
            PDPage target = doc.getPage(pageIndex);

            try (PDPageContentStream footer = new PDPageContentStream(
                    doc,
                    target,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            )) {
                footer.setStrokingColor(BORDER);
                footer.moveTo(60, 64);
                footer.lineTo(535, 64);
                footer.stroke();

                writeText(footer, title, 60, 45, PDType1Font.HELVETICA, 9, MUTED);
                writeText(footer, "Pagina " + pageNumber + " de " + totalPages, 468, 45,
                        PDType1Font.HELVETICA, 9, MUTED);
            }
        }

        void coverMetric(
                float x,
                float y,
                float w,
                float h,
                String title,
                String value,
                Color color,
                Color bg
        ) throws Exception {
            roundRect(x, y, w, h, 18, bg);
            text(title, x + 18, y + h - 24, PDType1Font.HELVETICA_BOLD, 11, NAVY);
            text(value, x + 18, y + 18, PDType1Font.HELVETICA_BOLD, 18, color);
        }

        void bigKpi(
                float x,
                float y,
                float w,
                float h,
                String title,
                String value,
                String subtitle,
                Color color,
                Color bg
        ) throws Exception {
            roundRect(x, y - h, w, h, 24, CARD);
            strokeRoundRect(x, y - h, w, h, 24, BORDER);

            roundRect(x + 18, y - 46, 44, 44, 16, bg);
            roundRect(x + 18, y - 46, 44, 8, 4, color);

            text(title, x + 78, y - 28, PDType1Font.HELVETICA_BOLD, 12, NAVY);
            text(value, x + 78, y - 58, PDType1Font.HELVETICA_BOLD, 21, color);
            text(subtitle, x + 78, y - 82, PDType1Font.HELVETICA, 9.5f, MUTED);
        }

        void statusCard(
                float x,
                float y,
                float w,
                float h,
                String title,
                long value,
                Color color,
                Color bg
        ) throws Exception {
            roundRect(x, y - h, w, h, 22, bg);
            text(title, x + 18, y - 28, PDType1Font.HELVETICA_BOLD, 11, NAVY);
            text(String.valueOf(value), x + 18, y - 58, PDType1Font.HELVETICA_BOLD, 24, color);
        }

        void productRow(
                float x,
                float y,
                float w,
                int index,
                String name,
                Long productId,
                long quantity,
                float percentage
        ) throws Exception {
            float h = 52;

            roundRect(x, y - h, w, h, 18, CARD);
            strokeRoundRect(x, y - h, w, h, 18, BORDER);

            roundRect(x + 14, y - 39, 30, 30, 12, SOFT_BLUE);
            text(String.valueOf(index), x + 24, y - 30, PDType1Font.HELVETICA_BOLD, 10, BLUE);

            text(safeText(name), x + 58, y - 19, PDType1Font.HELVETICA_BOLD, 11.5f, NAVY);
            text("ID " + productId + " - " + quantity + " palets", x + 58, y - 37,
                    PDType1Font.HELVETICA, 9.5f, MUTED);

            float barX = x + 290;
            float barY = y - 31;
            float barW = 170;
            float barH = 10;

            roundRect(barX, barY, barW, barH, 5, new Color(229, 234, 240));
            roundRect(barX, barY, Math.max(4, barW * percentage), barH, 5, BLUE);

            text("x" + quantity, x + 470, y - 28, PDType1Font.HELVETICA_BOLD, 10, BLUE);
        }

        void recommendation(
                float x,
                float y,
                float w,
                String title,
                String text,
                Color color,
                Color bg
        ) throws Exception {
            float h = 72;

            roundRect(x, y - h, w, h, 20, bg);
            text(title, x + 20, y - 25, PDType1Font.HELVETICA_BOLD, 12.5f, NAVY);
            paragraph(text, x + 20, y - 45, w - 40, 10.5f, PDType1Font.HELVETICA, MUTED);

            roundRect(x + w - 18, y - h + 14, 6, h - 28, 3, color);
        }

        void legend(float x, float y, String title, Color color, String pct) throws Exception {
            roundRect(x, y, 10, 10, 5, color);
            text(title, x + 16, y + 1, PDType1Font.HELVETICA_BOLD, 9.5f, NAVY);
            text(pct, x + 16, y - 13, PDType1Font.HELVETICA, 9, MUTED);
        }

        void paragraph(String text, float x, float y, float width, float fontSize, PDType1Font font, Color color)
                throws Exception {
            List<String> lines = wrapText(text, width, fontSize, font);

            float currentY = y;

            for (String line : lines) {
                text(line, x, currentY, font, fontSize, color);
                currentY -= fontSize + 4;
            }
        }

        private List<String> wrapText(String text, float width, float fontSize, PDType1Font font) throws Exception {
            java.util.ArrayList<String> lines = new java.util.ArrayList<>();

            String safe = safeText(text);
            String[] words = safe.split(" ");

            StringBuilder current = new StringBuilder();

            for (String word : words) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                float candidateWidth = font.getStringWidth(candidate) / 1000 * fontSize;

                if (candidateWidth <= width) {
                    current = new StringBuilder(candidate);
                } else {
                    if (current.length() > 0) {
                        lines.add(current.toString());
                    }
                    current = new StringBuilder(word);
                }
            }

            if (current.length() > 0) {
                lines.add(current.toString());
            }

            return lines;
        }

        void fillRect(float x, float y, float w, float h, Color color) throws Exception {
            cs.setNonStrokingColor(color);
            cs.addRect(x, y, w, h);
            cs.fill();
        }

        void roundRect(float x, float y, float w, float h, float r, Color color) throws Exception {
            cs.setNonStrokingColor(color);
            roundRectPath(x, y, w, h, r);
            cs.fill();
        }

        void strokeRoundRect(float x, float y, float w, float h, float r, Color color) throws Exception {
            cs.setStrokingColor(color);
            roundRectPath(x, y, w, h, r);
            cs.stroke();
        }

        private void roundRectPath(float x, float y, float w, float h, float r) throws Exception {
            float c = 0.55228475f * r;

            cs.moveTo(x + r, y);
            cs.lineTo(x + w - r, y);
            cs.curveTo(x + w - r + c, y, x + w, y + r - c, x + w, y + r);

            cs.lineTo(x + w, y + h - r);
            cs.curveTo(x + w, y + h - r + c, x + w - r + c, y + h, x + w - r, y + h);

            cs.lineTo(x + r, y + h);
            cs.curveTo(x + r - c, y + h, x, y + h - r + c, x, y + h - r);

            cs.lineTo(x, y + r);
            cs.curveTo(x, y + r - c, x + r - c, y, x + r, y);

            cs.closePath();
        }

        void text(String text, float x, float y, PDType1Font font, float size, Color color) throws Exception {
            writeText(cs, text, x, y, font, size, color);
        }

        private void writeText(
                PDPageContentStream stream,
                String text,
                float x,
                float y,
                PDType1Font font,
                float size,
                Color color
        ) throws Exception {
            stream.beginText();
            stream.setNonStrokingColor(color);
            stream.setFont(font, size);
            stream.newLineAtOffset(x, y);
            stream.showText(safeText(text));
            stream.endText();
        }

        private String safeText(String text) {
            if (text == null) return "";

            return text
                    .replace("€", "EUR")
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace("“", "\"")
                    .replace("”", "\"")
                    .replace("’", "'")
                    .replace("·", "-")
                    .replace("\n", " ")
                    .replace("\r", " ");
        }
    }
}