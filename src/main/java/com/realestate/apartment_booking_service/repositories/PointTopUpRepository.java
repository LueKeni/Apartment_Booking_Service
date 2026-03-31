package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.PointTopUp;
import com.realestate.apartment_booking_service.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointTopUpRepository extends JpaRepository<PointTopUp, Long> {

    Optional<PointTopUp> findByOrderId(String orderId);

    List<PointTopUp> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<PointTopUp> findTop200ByUserRoleOrderByCreatedAtDesc(com.realestate.apartment_booking_service.enums.Role role);

    @Query("select coalesce(sum(p.amount), 0) from PointTopUp p where p.status = :status")
    long sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query(value = """
            select date_format(created_at, '%Y-%m') as month, coalesce(sum(amount), 0) as total
            from point_topups
            where status = 'SUCCESS'
            group by month
            order by month
            """, nativeQuery = true)
    List<MonthlyRevenueProjection> findMonthlyRevenue();

    interface MonthlyRevenueProjection {
        String getMonth();

        BigDecimal getTotal();
    }
}
