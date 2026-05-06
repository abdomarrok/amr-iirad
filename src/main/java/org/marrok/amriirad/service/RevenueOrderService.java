package org.marrok.amriirad.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.OrderStatus;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic and validation for Revenue Orders (أوامر الإيراد).
 * Enforces rules RO-01 through RO-12 and INS-01 through INS-02.
 *
 * State Machine:
 *   DRAFT → ISSUED → DISPATCHED
 *   ISSUED → CANCELLED  (via CancellationOrderService)
 *   ISSUED → REDUCED    (via CancellationOrderService)
 */
public class RevenueOrderService {

    private static final Logger logger = LogManager.getLogger(RevenueOrderService.class);

    private final RevenueOrderRepository orderRepo = new RevenueOrderRepository();
    private final FiscalYearRepository   fyRepo    = new FiscalYearRepository();
    private final TafqeetService         tafqeet   = new TafqeetService();
    private final AuditService           audit     = new AuditService();

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Creates a new Revenue Order in DRAFT status.
     * Validates: fiscal year active, amount > 0, debtor present.
     * Auto-generates order number (RO-01) and amount-in-words (RO-10, INS-02).
     */
    public RevenueOrder createOrder(RevenueOrder order) throws SQLException {
        // BR-02 / RO-03: Amount must be > 0
        validateAmount(order.getAmount());

        // RO-02 / INS-01: Must have a debtor
        if (order.getDebtor() == null) {
            throw new IllegalArgumentException("يجب تحديد المدين قبل إنشاء أمر الإيراد");
        }

        // Must have a fiscal year
        if (order.getFiscalYear() == null || !order.getFiscalYear().isActive()) {
            throw new IllegalStateException("السنة المالية غير مفعّلة أو غير محددة");
        }

        // Must have a budget chapter
        if (order.getBudgetChapter() == null) {
            throw new IllegalArgumentException("يجب تحديد محور الميزانية");
        }

        // RO-01: Auto-generate order number (YYYY-NNN format)
        String orderNumber = generateOrderNumber(order.getFiscalYear().getId(),
                                                  order.getFiscalYear().getYearLabel());
        order.setOrderNumber(orderNumber);

        // RO-10 / INS-02: Compute amount in Arabic words — never manual input
        order.setAmountInWordsAr(tafqeet.toArabicWords(order.getAmount()));

        // Default status
        order.setStatus(OrderStatus.DRAFT);

        // Persist
        orderRepo.save(order);
        audit.log("revenue_order", order.getId(), AuditService.Action.INSERT,
                  order.getCreatedBy(), "أمر إيراد جديد: " + order.getOrderNumber());

        logger.info("Created revenue order: {}", order.getOrderNumber());
        return order;
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /**
     * Updates an existing Revenue Order.
     * RO-06: Can only edit in DRAFT status.
     * RO-12: DISPATCHED/CANCELLED/REDUCED orders are permanently locked.
     */
    public void updateOrder(RevenueOrder order) throws SQLException {
        RevenueOrder existing = findByIdOrThrow(order.getId());

        // RO-06: Only editable in DRAFT
        if (existing.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("لا يمكن تعديل أمر إيراد في حالة: " + existing.getStatus());
        }

        validateAmount(order.getAmount());

        // RO-10: Recompute amount in words
        order.setAmountInWordsAr(tafqeet.toArabicWords(order.getAmount()));

        orderRepo.update(order);
        audit.log("revenue_order", order.getId(), AuditService.Action.UPDATE,
                  order.getCreatedBy(), "تعديل أمر: " + order.getOrderNumber());
    }

    // =========================================================================
    // STATUS TRANSITIONS
    // =========================================================================

    /**
     * DRAFT → ISSUED.
     * INS-01: All required fields must be filled before issuing.
     * RO-04: Reasons (الأسباب) is mandatory.
     * RO-09: Issue date must not be in the future.
     */
    public void issueOrder(int orderId, String issuedBy) throws SQLException {
        RevenueOrder order = findByIdOrThrow(orderId);

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("لا يمكن إصدار الأمر إلا من حالة DRAFT. الحالة الحالية: " + order.getStatus());
        }

        // RO-04 / INS-01: Reasons mandatory
        if (order.getObjectAr() == null || order.getObjectAr().isBlank()) {
            throw new IllegalArgumentException("يجب تحديد موضوع الإيراد (الأسباب) قبل الإصدار");
        }

        // RO-09: Issue date not in future
        if (order.getIssueDate() != null && order.getIssueDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("تاريخ الإصدار لا يمكن أن يكون في المستقبل");
        }

        orderRepo.updateStatus(orderId, OrderStatus.ISSUED);
        audit.log("revenue_order", orderId, AuditService.Action.UPDATE,
                  issuedBy, "إصدار الأمر: DRAFT → ISSUED");

        logger.info("Order {} issued.", orderId);
    }

    /**
     * ISSUED → DISPATCHED (called by DispatchSlipService when order is added to a slip).
     * BR-06: Only ISSUED orders can be dispatched.
     */
    public void markDispatched(int orderId, String performedBy) throws SQLException {
        RevenueOrder order = findByIdOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ISSUED) {
            throw new IllegalStateException("لا يمكن إرسال أمر غير مصدر. الحالة الحالية: " + order.getStatus());
        }

        orderRepo.updateStatus(orderId, OrderStatus.DISPATCHED);
        audit.log("revenue_order", orderId, AuditService.Action.UPDATE,
                  performedBy, "إرسال الأمر: ISSUED → DISPATCHED");
    }

    // =========================================================================
    // SOFT DELETE
    // =========================================================================

    /**
     * Soft-deletes a revenue order.
     * RO-08: Cannot delete DISPATCHED, CANCELLED, or REDUCED orders.
     */
    public void deleteOrder(int orderId, String deletedBy) throws SQLException {
        RevenueOrder order = findByIdOrThrow(orderId);

        if (order.getStatus() == OrderStatus.DISPATCHED
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.REDUCED) {
            throw new IllegalStateException("لا يمكن حذف أمر في حالة: " + order.getStatus());
        }

        orderRepo.delete(orderId, deletedBy);
        audit.log("revenue_order", orderId, AuditService.Action.DELETE,
                  deletedBy, "حذف أمر: " + order.getOrderNumber());
    }

    // =========================================================================
    // QUERIES
    // =========================================================================

    public List<RevenueOrder> findAllByFiscalYear(int fiscalYearId) throws SQLException {
        return orderRepo.findAll(fiscalYearId);
    }

    public List<RevenueOrder> findByStatus(int fiscalYearId, OrderStatus status) throws SQLException {
        return orderRepo.findByStatus(fiscalYearId, status);
    }

    public Optional<RevenueOrder> findById(int id) throws SQLException {
        return orderRepo.findById(id);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * BR-02 / RO-03: Amount must be strictly > 0.
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر");
        }
    }

    private RevenueOrder findByIdOrThrow(int id) throws SQLException {
        return orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("أمر الإيراد غير موجود: " + id));
    }

    /**
     * RO-01: Auto-generate order number.
     * Format: YYYY-NNN (e.g. 2025-001, 2025-002)
     */
    private String generateOrderNumber(int fiscalYearId, String yearLabel) throws SQLException {
        List<RevenueOrder> existing = orderRepo.findAll(fiscalYearId);
        int nextSeq = existing.size() + 1;
        return yearLabel + "-" + String.format("%03d", nextSeq);
    }
}
