package com.smartworks.smartworks_api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartworks.smartworks_api.dto.MonthlyAnalyticsResponseV2;
import com.smartworks.smartworks_api.entity.Order;
import com.smartworks.smartworks_api.repository.OrderLineRepository;
import com.smartworks.smartworks_api.repository.OrderRepository;

@Service
public class AnalyticsServiceV2 {

        private final OrderRepository orderRepo;
        private final OrderLineRepository orderLineRepo;
        private final AnalyticsService analyticsService; // reutilizamos tu media de entrega existente

        public AnalyticsServiceV2(OrderRepository orderRepo,
                        OrderLineRepository orderLineRepo,
                        AnalyticsService analyticsService) {
                this.orderRepo = orderRepo;
                this.orderLineRepo = orderLineRepo;
                this.analyticsService = analyticsService;
        }

        public MonthlyAnalyticsResponseV2 monthly(Integer year, Integer month) {
                LocalDate now = LocalDate.now();
                int y = (year == null) ? now.getYear() : year;
                int m = (month == null) ? now.getMonthValue() : month;

                LocalDateTime start = LocalDate.of(y, m, 1).atStartOfDay();
                LocalDateTime end = LocalDate.of(y, m, 1).plusMonths(1).atStartOfDay();

                LocalDateTime prevStart = start.minusMonths(1);
                LocalDateTime prevEnd = start;

                long thisCount = orderRepo.countByCreatedAtBetween(start, end);
                long prevCount = orderRepo.countByCreatedAtBetween(prevStart, prevEnd);

                long pending = orderRepo.countByStatusAndCreatedAtBetween(Order.Status.PENDING, start, end);
                long completed = orderRepo.countByStatusAndCreatedAtBetween(Order.Status.COMPLETED, start, end);
                long cancelled = orderRepo.countByStatusAndCreatedAtBetween(Order.Status.CANCELLED, start, end);

                double ordersChangePct = (prevCount == 0)
                                ? (thisCount == 0 ? 0 : 100)
                                : ((thisCount - prevCount) * 100.0 / prevCount);

                // ✅ Reutilizamos tu servicio actual (no rompemos nada)
                double avgThis = analyticsService.monthly(y, m).avgDeliveryHoursThisMonth;

                int prevY = (m == 1) ? (y - 1) : y;
                int prevM = (m == 1) ? 12 : (m - 1);
                double avgPrev = analyticsService.monthly(prevY, prevM).avgDeliveryHoursThisMonth;

                double avgChangePct = (avgPrev == 0)
                                ? (avgThis == 0 ? 0 : 100)
                                : ((avgThis - avgPrev) * 100.0 / avgPrev);

                // Top productos por unidades
                List<Object[]> rows = orderLineRepo.topProducts(start, end);
                List<MonthlyAnalyticsResponseV2.TopProduct> top = rows.stream()
                                .limit(5)
                                .map(r -> new MonthlyAnalyticsResponseV2.TopProduct(
                                                (Long) r[0],
                                                (String) r[1],
                                                ((Number) r[2]).longValue()))
                                .toList();

                // ✅ NUEVO: unidades y facturación
                long units = orderLineRepo.totalUnits(start, end);
                double revenueThis = safe(orderLineRepo.revenue(start, end));
                double revenuePrev = safe(orderLineRepo.revenue(prevStart, prevEnd));

                double revenueChangePct = (revenuePrev == 0)
                                ? (revenueThis == 0 ? 0 : 100)
                                : ((revenueThis - revenuePrev) * 100.0 / revenuePrev);

                double avgOrderValue = (thisCount == 0) ? 0 : (revenueThis / thisCount);

                MonthlyAnalyticsResponseV2 res = new MonthlyAnalyticsResponseV2();
                res.year = y;
                res.month = m;

                res.ordersThisMonth = thisCount;
                res.ordersLastMonth = prevCount;
                res.ordersChangePct = ordersChangePct;

                res.pendingThisMonth = pending;
                res.completedThisMonth = completed;
                res.cancelledThisMonth = cancelled;

                res.avgDeliveryHoursThisMonth = avgThis;
                res.avgDeliveryChangePct = avgChangePct;

                res.topProductsByUnits = top;

                res.unitsSoldThisMonth = units;
                res.revenueThisMonth = revenueThis;
                res.revenueLastMonth = revenuePrev;
                res.revenueChangePct = revenueChangePct;
                res.avgOrderValueThisMonth = avgOrderValue;

                return res;
        }

        private double safe(Double v) {
                return (v == null) ? 0 : v;
        }
}
