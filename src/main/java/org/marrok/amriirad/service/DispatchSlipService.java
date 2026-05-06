package org.marrok.amriirad.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.DispatchSlip;
import org.marrok.amriirad.model.OrderStatus;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.repository.DispatchSlipRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for Dispatch Slips / بوردرو الإرسال (Annexe 5).
 * Enforces rules BR-05, BR-06, and INS-06.
 */
public class DispatchSlipService {

    private static final Logger logger = LogManager.getLogger(DispatchSlipService.class);

    private final DispatchSlipRepository slipRepo;
    private final RevenueOrderRepository orderRepo;
    private final RevenueOrderService    orderSvc;
    private final AuditService           audit;

    public DispatchSlipService(DispatchSlipRepository slipRepo, RevenueOrderRepository orderRepo, RevenueOrderService orderSvc, AuditService audit) {
        this.slipRepo = slipRepo;
        this.orderRepo = orderRepo;
        this.orderSvc = orderSvc;
        this.audit = audit;
    }

    /**
     * Creates a new Dispatch Slip with the given orders.
     *
     * BR-05: All orders must belong to the same fiscal year.
     * BR-06: Only ISSUED orders can be dispatched.
     * INS-06: Total amount is auto-computed — never from user input.
     */
    public DispatchSlip createSlip(DispatchSlip slip) throws SQLException {
        if (slip.getOrders() == null || slip.getOrders().isEmpty()) {
            throw new IllegalArgumentException("يجب إضافة أمر إيراد واحد على الأقل إلى البوردرو");
        }

        if (slip.getFiscalYear() == null) {
            throw new IllegalArgumentException("يجب تحديد السنة المالية للبوردرو");
        }

        int expectedFyId = slip.getFiscalYear().getId();

        for (RevenueOrder order : slip.getOrders()) {
            // BR-06: Only ISSUED orders
            if (order.getStatus() != OrderStatus.ISSUED) {
                throw new IllegalStateException(
                        "الأمر رقم " + order.getOrderNumber() + " ليس في حالة ISSUED. الحالة: " + order.getStatus());
            }

            // BR-05: Same fiscal year
            if (order.getFiscalYear() == null || order.getFiscalYear().getId() != expectedFyId) {
                throw new IllegalStateException(
                        "الأمر رقم " + order.getOrderNumber() + " لا ينتمي لنفس السنة المالية");
            }
        }

        // INS-06: Auto-compute total amount
        slip.recalculateTotal();

        // Auto-generate slip number
        String slipNumber = generateSlipNumber(expectedFyId, slip.getFiscalYear().getYearLabel());
        slip.setSlipNumber(slipNumber);

        // Persist slip + link orders
        slipRepo.save(slip);

        // Mark all orders as DISPATCHED
        for (RevenueOrder order : slip.getOrders()) {
            orderSvc.markDispatched(order.getId(), slip.getCreatedBy());
        }

        audit.log("dispatch_slip", slip.getId(), AuditService.Action.INSERT,
                  slip.getCreatedBy(),
                  "بوردرو جديد: " + slipNumber + " (" + slip.getOrders().size() + " أوامر)");

        logger.info("Created dispatch slip {} with {} orders", slipNumber, slip.getOrders().size());
        return slip;
    }

    // =========================================================================
    // QUERIES
    // =========================================================================

    public List<DispatchSlip> findAllByFiscalYear(int fiscalYearId) throws SQLException {
        return slipRepo.findAll(fiscalYearId);
    }

    public Optional<DispatchSlip> findById(int id) throws SQLException {
        return slipRepo.findById(id);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Generates a slip number: BRD-YYYY-NNN
     */
    private String generateSlipNumber(int fiscalYearId, String yearLabel) throws SQLException {
        List<DispatchSlip> existing = slipRepo.findAll(fiscalYearId);
        int nextSeq = existing.size() + 1;
        return "BRD-" + yearLabel + "-" + String.format("%03d", nextSeq);
    }
}
