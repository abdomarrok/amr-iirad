package org.marrok.amriirad.model;

/**
 * Represents audit details for a specific Revenue Order within a Zero Value Decision.
 * Maps to `zero_value_order_details` table.
 */
public class ZeroValueOrderDetail {

    private int id;
    private int decisionId;
    private RevenueOrder revenueOrder;
    private String nonCollectionReasons; // أسباب عدم التحصيل
    private String enforcementActions;    // طبيعة وتاريخ المتابعات
    private String deliberativeOpinion;   // رأي الهيئات التداولية
    private String accountantObservations; // ملاحظات المحاسب

    public ZeroValueOrderDetail() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDecisionId() { return decisionId; }
    public void setDecisionId(int decisionId) { this.decisionId = decisionId; }

    public RevenueOrder getRevenueOrder() { return revenueOrder; }
    public void setRevenueOrder(RevenueOrder revenueOrder) { this.revenueOrder = revenueOrder; }

    public String getNonCollectionReasons() { return nonCollectionReasons; }
    public void setNonCollectionReasons(String s) { this.nonCollectionReasons = s; }

    public String getEnforcementActions() { return enforcementActions; }
    public void setEnforcementActions(String s) { this.enforcementActions = s; }

    public String getDeliberativeOpinion() { return deliberativeOpinion; }
    public void setDeliberativeOpinion(String s) { this.deliberativeOpinion = s; }

    public String getAccountantObservations() { return accountantObservations; }
    public void setAccountantObservations(String s) { this.accountantObservations = s; }
}
