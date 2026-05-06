package org.marrok.amriirad.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.*;
import org.marrok.amriirad.repository.CancellationOrderRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Business logic for Cancellation and Reduction orders (Annexe 3 & 4).
 * Enforces rules BR-03, BR-04, INS-04, and INS-05.
 */
public class CancellationOrderService {

    private static final Logger logger = LogManager.getLogger(CancellationOrderService.class);

    private final CancellationOrderRepository cancelRepo = new CancellationOrderRepository();
    private final RevenueOrderRepository      orderRepo  = new RevenueOrderRepository();
    private final AuditService                audit      = new AuditService();

    /**
     * Creates a full cancellation (إلغاء كلي) for an existing revenue order.
     *
     * BR-03: Cannot cancel a DRAFT order — must be ISSUED.
     * INS-04: Must reference an existing revenue order.
     * INS-05: Reason (motif) is mandatory.
     */
    public RevenueOrderCancellation cancelOrder(RevenueOrderCancellation cancellation) throws SQLException {
        RevenueOrder original = resolveOriginalOrder(cancellation);

        // BR-03: Cannot cancel a DRAFT order
        if (original.getStatus() == OrderStatus.DRAFT) {
            throw new IllegalStateException("لا يمكن إلغاء أمر في حالة مسودة (DRAFT)");
        }

        // Only ISSUED orders can be cancelled
        if (original.getStatus() != OrderStatus.ISSUED) {
            throw new IllegalStateException("لا يمكن إلغاء أمر في حالة: " + original.getStatus());
        }

        // INS-05: Reason is mandatory
        validateReason(cancellation.getReasonAr());

        // Force type
        cancellation.setCancellationType(CancellationType.FULL_CANCEL);

        // Persist cancellation record
        cancelRepo.save(cancellation);

        // Update original order status to CANCELLED
        orderRepo.updateStatus(original.getId(), OrderStatus.CANCELLED);

        audit.log("revenue_order_cancellation", cancellation.getId(),
                  AuditService.Action.INSERT, cancellation.getCreatedBy(),
                  "إلغاء كلي للأمر رقم: " + original.getOrderNumber());

        logger.info("Full cancellation created for order {}", original.getOrderNumber());
        return cancellation;
    }

    /**
     * Creates a reduction (تخفيض جزئي) for an existing revenue order.
     *
     * BR-04: Reduction cannot exceed original amount.
     * INS-05: Reason is mandatory.
     */
    public RevenueOrderCancellation reduceOrder(RevenueOrderCancellation cancellation) throws SQLException {
        RevenueOrder original = resolveOriginalOrder(cancellation);

        // Only ISSUED orders can be reduced
        if (original.getStatus() != OrderStatus.ISSUED) {
            throw new IllegalStateException("لا يمكن تخفيض أمر غير مصدر. الحالة الحالية: " + original.getStatus());
        }

        // INS-05: Reason is mandatory
        validateReason(cancellation.getReasonAr());

        // BR-04: Reduction amount must be > 0 and ≤ original amount
        BigDecimal reducedAmount = cancellation.getReducedAmount();
        if (reducedAmount == null || reducedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("مبلغ التخفيض يجب أن يكون أكبر من صفر");
        }
        if (reducedAmount.compareTo(original.getAmount()) > 0) {
            throw new IllegalArgumentException(
                    "مبلغ التخفيض (" + reducedAmount + ") لا يمكن أن يتجاوز المبلغ الأصلي (" + original.getAmount() + ")");
        }

        // Force type
        cancellation.setCancellationType(CancellationType.REDUCTION);

        // Persist
        cancelRepo.save(cancellation);

        // Update original order status to REDUCED
        orderRepo.updateStatus(original.getId(), OrderStatus.REDUCED);

        audit.log("revenue_order_cancellation", cancellation.getId(),
                  AuditService.Action.INSERT, cancellation.getCreatedBy(),
                  "تخفيض الأمر رقم: " + original.getOrderNumber() + " بمبلغ " + reducedAmount);

        logger.info("Reduction of {} created for order {}", reducedAmount, original.getOrderNumber());
        return cancellation;
    }

    /**
     * Finds a cancellation record by the original order ID.
     */
    public Optional<RevenueOrderCancellation> findByOrderId(int orderId) throws SQLException {
        return cancelRepo.findByOrderId(orderId);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private RevenueOrder resolveOriginalOrder(RevenueOrderCancellation cancellation) throws SQLException {
        // INS-04: Must reference an existing order
        if (cancellation.getOriginalOrder() == null) {
            throw new IllegalArgumentException("يجب تحديد الأمر الأصلي");
        }
        return orderRepo.findById(cancellation.getOriginalOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "الأمر الأصلي غير موجود: " + cancellation.getOriginalOrder().getId()));
    }

    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("يجب تحديد سبب الإلغاء أو التخفيض (INS-05)");
        }
    }
}
