package org.marrok.amriirad.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.marrok.amriirad.model.*;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RevenueOrderService.
 * Tests business logic and validation rules (RO-01 through RO-12).
 */
@DisplayName("RevenueOrderService Tests")
class RevenueOrderServiceTest {

    private RevenueOrderService service;
    private RevenueOrderRepository mockOrderRepo;
    private AuditService mockAuditService;
    private FiscalYearRepository mockFyRepo;
    private TafqeetService mockTafqeetService;

    private RevenueOrder testOrder;
    private FiscalYear activeFiscalYear;
    private Debtor testDebtor;
    private BudgetChapter testChapter;

    @BeforeEach
    void setUp() {
        // Create mocks
        mockOrderRepo = mock(RevenueOrderRepository.class);
        mockAuditService = mock(AuditService.class);
        mockFyRepo = mock(FiscalYearRepository.class);
        mockTafqeetService = mock(TafqeetService.class);

        // Initialize service
        service = new RevenueOrderService(mockOrderRepo, mockAuditService, mockFyRepo, mockTafqeetService);

        // Create test data
        activeFiscalYear = new FiscalYear(1, "2025", true, LocalDateTime.now());
        testDebtor = new Debtor();
        testDebtor.setId(1);
        testDebtor.setFullName("Ahmed Mohamed");
        testDebtor.setDebtorType(DebtorType.INDIVIDUAL);

        testChapter = new BudgetChapter(1, "01", "الرواتب والأجور", "Salaires", null, 1, 1);

        testOrder = new RevenueOrder();
        testOrder.setFiscalYear(activeFiscalYear);
        testOrder.setDebtor(testDebtor);
        testOrder.setBudgetChapter(testChapter);
        testOrder.setAmount(new BigDecimal("10000.00"));
        testOrder.setObjectAr("تصفية رواتب الموظفين");
        testOrder.setIssueDate(LocalDate.now());
        testOrder.setCreatedBy("admin");
    }

    // =========================================================================
    // CREATE ORDER TESTS
    // =========================================================================

    @Test
    @DisplayName("Should successfully create a new revenue order (RO-01: Auto-generate order number)")
    void testCreateOrderSuccess() throws SQLException {
        // Given
        when(mockTafqeetService.toArabicWords(testOrder.getAmount())).thenReturn("عشرة آلاف دينار");
        when(mockOrderRepo.findAll(1)).thenReturn(new ArrayList<>());
        testOrder.setId(1);
        testOrder.setCreatedAt(LocalDateTime.now());

        // When
        RevenueOrder result = service.createOrder(testOrder);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrderNumber());
        assertTrue(result.getOrderNumber().startsWith("2025-"));
        assertEquals(OrderStatus.DRAFT, result.getStatus());
        assertEquals("عشرة آلاف دينار", result.getAmountInWordsAr());
        verify(mockOrderRepo).save(testOrder);
        verify(mockAuditService).log(eq("revenue_order"), anyInt(), 
                                     eq(AuditService.Action.INSERT), eq("admin"), contains("أمر إيراد جديد"));
    }

    @Test
    @DisplayName("Should reject order with zero amount (BR-02/RO-03)")
    void testCreateOrderWithZeroAmount() {
        // Given
        testOrder.setAmount(BigDecimal.ZERO);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createOrder(testOrder));
        assertTrue(exception.getMessage().contains("أكبر من صفر"));
    }

    @Test
    @DisplayName("Should reject order with negative amount (BR-02/RO-03)")
    void testCreateOrderWithNegativeAmount() {
        // Given
        testOrder.setAmount(new BigDecimal("-100.00"));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createOrder(testOrder));
        assertTrue(exception.getMessage().contains("أكبر من صفر"));
    }

    @Test
    @DisplayName("Should reject order without debtor (RO-02/INS-01)")
    void testCreateOrderWithoutDebtor() {
        // Given
        testOrder.setDebtor(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createOrder(testOrder));
        assertTrue(exception.getMessage().contains("المدين"));
    }

    @Test
    @DisplayName("Should reject order without fiscal year")
    void testCreateOrderWithoutFiscalYear() {
        // Given
        testOrder.setFiscalYear(null);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.createOrder(testOrder));
        assertTrue(exception.getMessage().contains("السنة المالية"));
    }

    @Test
    @DisplayName("Should reject order without budget chapter")
    void testCreateOrderWithoutBudgetChapter() {
        // Given
        testOrder.setBudgetChapter(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.createOrder(testOrder));
        assertTrue(exception.getMessage().contains("محور الميزانية"));
    }

    // =========================================================================
    // UPDATE ORDER TESTS
    // =========================================================================

    @Test
    @DisplayName("Should successfully update a DRAFT order")
    void testUpdateDraftOrderSuccess() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.DRAFT);
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));
        when(mockTafqeetService.toArabicWords(testOrder.getAmount())).thenReturn("عشرة آلاف دينار");

        // When
        service.updateOrder(testOrder);

        // Then
        verify(mockOrderRepo).update(testOrder);
        verify(mockAuditService).log(eq("revenue_order"), eq(1), 
                                     eq(AuditService.Action.UPDATE), eq("admin"), contains("تعديل أمر"));
    }

    @Test
    @DisplayName("Should reject updating an ISSUED order (RO-06)")
    void testUpdateIssuedOrder() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.ISSUED);
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.updateOrder(testOrder));
        assertTrue(exception.getMessage().contains("لا يمكن تعديل"));
    }

    // =========================================================================
    // ISSUE ORDER TESTS
    // =========================================================================

    @Test
    @DisplayName("Should successfully issue a DRAFT order")
    void testIssueOrderSuccess() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.DRAFT);
        testOrder.setObjectAr("موضوع الإيراد");
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When
        service.issueOrder(1, "admin");

        // Then
        verify(mockOrderRepo).updateStatus(1, OrderStatus.ISSUED);
        verify(mockAuditService).log(eq("revenue_order"), eq(1), 
                                     eq(AuditService.Action.UPDATE), eq("admin"), contains("إصدار"));
    }

    @Test
    @DisplayName("Should reject issuing without object/reason (RO-04)")
    void testIssueOrderWithoutObject() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.DRAFT);
        testOrder.setObjectAr(null);
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.issueOrder(1, "admin"));
        assertTrue(exception.getMessage().contains("موضوع"));
    }

    @Test
    @DisplayName("Should reject issuing with future issue date (RO-09)")
    void testIssueOrderWithFutureDate() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.DRAFT);
        testOrder.setObjectAr("موضوع");
        testOrder.setIssueDate(LocalDate.now().plusDays(1)); // Future date
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.issueOrder(1, "admin"));
        assertTrue(exception.getMessage().contains("المستقبل"));
    }

    // =========================================================================
    // DISPATCH ORDER TESTS
    // =========================================================================

    @Test
    @DisplayName("Should successfully mark order as DISPATCHED")
    void testMarkDispatchedSuccess() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.ISSUED);
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When
        service.markDispatched(1, "admin");

        // Then
        verify(mockOrderRepo).updateStatus(1, OrderStatus.DISPATCHED);
        verify(mockAuditService).log(eq("revenue_order"), eq(1), 
                                     eq(AuditService.Action.UPDATE), eq("admin"), contains("إرسال"));
    }

    @Test
    @DisplayName("Should reject dispatching non-ISSUED order (BR-06)")
    void testDispatchNonIssuedOrder() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.DRAFT);
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.markDispatched(1, "admin"));
        assertTrue(exception.getMessage().contains("لا يمكن إرسال"));
    }

    // =========================================================================
    // DELETE ORDER TESTS
    // =========================================================================

    @Test
    @DisplayName("Should successfully delete a DRAFT order")
    void testDeleteDraftOrderSuccess() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.DRAFT);
        testOrder.setOrderNumber("2025-001");
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When
        service.deleteOrder(1, "admin");

        // Then
        verify(mockOrderRepo).delete(1, "admin");
        verify(mockAuditService).log(eq("revenue_order"), eq(1), 
                                     eq(AuditService.Action.DELETE), eq("admin"), contains("حذف"));
    }

    @Test
    @DisplayName("Should reject deleting DISPATCHED order (RO-08)")
    void testDeleteDispatchedOrder() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.DISPATCHED);
        testOrder.setOrderNumber("2025-001");
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.deleteOrder(1, "admin"));
        assertTrue(exception.getMessage().contains("لا يمكن حذف"));
    }

    @Test
    @DisplayName("Should reject deleting CANCELLED order (RO-08)")
    void testDeleteCancelledOrder() throws SQLException {
        // Given
        testOrder.setId(1);
        testOrder.setStatus(OrderStatus.CANCELLED);
        testOrder.setOrderNumber("2025-001");
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.deleteOrder(1, "admin"));
        assertTrue(exception.getMessage().contains("لا يمكن حذف"));
    }

    // =========================================================================
    // QUERY TESTS
    // =========================================================================

    @Test
    @DisplayName("Should find all orders for fiscal year")
    void testFindAllByFiscalYear() throws SQLException {
        // Given
        List<RevenueOrder> orders = List.of(testOrder);
        when(mockOrderRepo.findAll(1)).thenReturn(orders);

        // When
        List<RevenueOrder> result = service.findAllByFiscalYear(1);

        // Then
        assertEquals(1, result.size());
        verify(mockOrderRepo).findAll(1);
    }

    @Test
    @DisplayName("Should find orders by status")
    void testFindByStatus() throws SQLException {
        // Given
        testOrder.setStatus(OrderStatus.DRAFT);
        List<RevenueOrder> orders = List.of(testOrder);
        when(mockOrderRepo.findByStatus(1, OrderStatus.DRAFT)).thenReturn(orders);

        // When
        List<RevenueOrder> result = service.findByStatus(1, OrderStatus.DRAFT);

        // Then
        assertEquals(1, result.size());
        assertEquals(OrderStatus.DRAFT, result.get(0).getStatus());
        verify(mockOrderRepo).findByStatus(1, OrderStatus.DRAFT);
    }

    @Test
    @DisplayName("Should find order by ID")
    void testFindById() throws SQLException {
        // Given
        testOrder.setId(1);
        when(mockOrderRepo.findById(1)).thenReturn(Optional.of(testOrder));

        // When
        Optional<RevenueOrder> result = service.findById(1);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testOrder, result.get());
        verify(mockOrderRepo).findById(1);
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent order")
    void testFindByIdNotFound() throws SQLException {
        // Given
        when(mockOrderRepo.findById(999)).thenReturn(Optional.empty());

        // When
        Optional<RevenueOrder> result = service.findById(999);

        // Then
        assertTrue(result.isEmpty());
        verify(mockOrderRepo).findById(999);
    }
}
