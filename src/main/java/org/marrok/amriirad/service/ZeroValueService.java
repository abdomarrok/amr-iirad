package org.marrok.amriirad.service;

import org.marrok.amriirad.model.*;
import org.marrok.amriirad.repository.ZeroValueRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;

import java.sql.SQLException;
import java.util.List;

public class ZeroValueService {

    private final ZeroValueRepository zeroRepo;
    private final RevenueOrderRepository orderRepo;
    private final AuditService audit;

    public ZeroValueService(ZeroValueRepository zeroRepo, RevenueOrderRepository orderRepo, AuditService audit) {
        this.zeroRepo = zeroRepo;
        this.orderRepo = orderRepo;
        this.audit = audit;
    }

    public ZeroValueDecision createDecision(ZeroValueDecision decision) throws SQLException {
        if (decision.getDetails().isEmpty()) {
            throw new IllegalArgumentException("يجب اختيار أمر إيراد واحد على الأقل");
        }

        // Persist decision
        zeroRepo.save(decision);

        // Update each order status
        for (ZeroValueOrderDetail detail : decision.getDetails()) {
            orderRepo.updateStatus(detail.getRevenueOrder().getId(), OrderStatus.ZERO_VALUE);
            
            audit.log("zero_value_decision", decision.getId(),
                      AuditService.Action.INSERT, decision.getCreatedBy(),
                      "قبول الأمر رقم " + detail.getRevenueOrder().getOrderNumber() + " كقيمة منعدمة");
        }

        return decision;
    }

    public List<ZeroValueDecision> listByYear(int fiscalYearId) throws SQLException {
        return zeroRepo.findAll(fiscalYearId);
    }

    public void loadDetails(ZeroValueDecision decision) throws SQLException {
        zeroRepo.loadDetails(decision);
    }
}
