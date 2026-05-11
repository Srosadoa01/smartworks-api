package com.smartworks.smartworks_api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartworks.smartworks_api.dto.MonthlyAnalyticsResponse;
import com.smartworks.smartworks_api.entity.Order;
import com.smartworks.smartworks_api.repository.OrderLineRepository;
import com.smartworks.smartworks_api.repository.OrderRepository;

@Service
public class AnalyticsService {

        private final OrderRepository orderRepo;
        private final OrderLineRepository orderLineRepo;

        public AnalyticsService(OrderRepository orderRepo, OrderLineRepository orderLineRepo) {
                this.orderRepo = orderRepo;
                this.orderLineRepo = orderLineRepo;
        }

        public MonthlyAnalyticsResponse monthly(Integer year, Integer month) {
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

                double avgThis = avgCompletionHours(start, end);
                double avgPrev = avgCompletionHours(prevStart, prevEnd);

                double ordersChangePct = (prevCount == 0)
                                ? (thisCount == 0 ? 0 : 100)
                                : ((thisCount - prevCount) * 100.0 / prevCount);

                double avgChangePct = (avgPrev == 0)
                                ? (avgThis == 0 ? 0 : 100)
                                : ((avgThis - avgPrev) * 100.0 / avgPrev);

                List<Object[]> rows = orderLineRepo.topProducts(start, end);
                List<MonthlyAnalyticsResponse.TopProduct> top = rows.stream()
                                .limit(5)
                                .map(r -> new MonthlyAnalyticsResponse.TopProduct(
                                                (Long) r[0],
                                                (String) r[1],
                                                ((Number) r[2]).longValue()))
                                .toList();

                MonthlyAnalyticsResponse res = new MonthlyAnalyticsResponse();
                res.year = y;
                res.month = m;

                res.ordersThisMonth = thisCount;
                res.ordersLastMonth = prevCount;
                res.ordersChangePct = ordersChangePct;

                res.pendingThisMonth = pending;
                res.completedThisMonth = completed;

                res.avgDeliveryHoursThisMonth = avgThis;
                res.avgDeliveryChangePct = avgChangePct;

                res.topProducts = top;

                return res;
        }

        private double avgCompletionHours(LocalDateTime from, LocalDateTime to) {
                List<Order> completedOrders = orderRepo.findByStatusAndCompletedAtIsNotNullAndCreatedAtBetween(
                                Order.Status.COMPLETED, from, to);

                if (completedOrders.isEmpty())
                        return 0;

                double avgMinutes = completedOrders.stream()
                                .mapToLong(o -> ChronoUnit.MINUTES.between(o.getCreatedAt(), o.getCompletedAt()))
                                .average()
                                .orElse(0);

                return avgMinutes / 60.0;
        }
}
