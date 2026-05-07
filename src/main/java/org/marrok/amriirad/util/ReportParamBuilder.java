package org.marrok.amriirad.util;

import org.marrok.amriirad.model.Debtor;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.model.RevenueOrderCancellation;
import org.marrok.amriirad.service.TafqeetService;

import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder to centralize JasperReport parameter mapping.
 * Eliminates redundant map construction in controllers.
 */
public class ReportParamBuilder {

    private final Map<String, Object> params = new HashMap<>();
    private final TafqeetService tafqeet;

    private ReportParamBuilder(TafqeetService tafqeet) {
        this.tafqeet = tafqeet;
    }

    public static ReportParamBuilder create(TafqeetService tafqeet) {
        return new ReportParamBuilder(tafqeet);
    }

    public ReportParamBuilder withOrder(RevenueOrder order) {
        params.put("ORDER_NUMBER", order.getOrderNumber() != null ? order.getOrderNumber() : "");
        params.put("FISCAL_YEAR", order.getFiscalYear() != null ? order.getFiscalYear().getYearLabel() : "");
        params.put("ISSUE_DATE", order.getIssueDate() != null ? order.getIssueDate().toString() : "");
        params.put("AMOUNT", order.getAmount() != null ? String.format("%,.2f", order.getAmount()) : "0.00");
        params.put("AMOUNT_RAW", order.getAmount() != null ? order.getAmount().toString() : "0.00");
        
        if (tafqeet != null && order.getAmount() != null) {
            params.put("AMOUNT_WORDS", tafqeet.toArabicWords(order.getAmount()));
        }

        params.put("REASON_AR", order.getObjectAr() != null ? order.getObjectAr() : "");
        params.put("LIQUIDATION_BASIS", order.getObjectAr() != null ? order.getObjectAr() : "");
        params.put("TREASURY_REF", "1980000034/55"); // TODO: Move to AppSettings

        if (order.getBudgetChapter() != null) {
            params.put("BUDGET_CHAPTER", order.getBudgetChapter().getCode());
        }

        if (order.getDebtor() != null) {
            withDebtor(order.getDebtor());
        }

        return this;
    }

    public ReportParamBuilder withDebtor(Debtor debtor) {
        params.put("DEBTOR_NAME", debtor.getFullName() != null ? debtor.getFullName() : "");
        params.put("DEBTOR_ADDRESS", debtor.getAddress() != null ? debtor.getAddress() : "");
        params.put("DEBTOR_ACCOUNT", debtor.getBankAccount() != null ? debtor.getBankAccount() : "");
        params.put("DEBTOR_CNAS", debtor.getCnasNumber() != null ? debtor.getCnasNumber() : "");
        params.put("DEBTOR_NIF", debtor.getNifNumber() != null ? debtor.getNifNumber() : "");
        return this;
    }

    public ReportParamBuilder withCancellation(RevenueOrderCancellation cancel) {
        params.put("CANCEL_NUMBER", cancel.getCancellationNumber() != null ? cancel.getCancellationNumber() : "");
        params.put("DATE", cancel.getCancellationDate() != null ? cancel.getCancellationDate().toString() : "");
        params.put("REASON", cancel.getReasonAr() != null ? cancel.getReasonAr() : "");
        
        if (cancel.getOriginalOrder() != null) {
            params.put("ORDER_NUMBER", cancel.getOriginalOrder().getOrderNumber());
            withDebtor(cancel.getOriginalOrder().getDebtor());
        }
        
        var amount = cancel.getReducedAmount() != null ? cancel.getReducedAmount() : 
                    (cancel.getOriginalOrder() != null ? cancel.getOriginalOrder().getAmount() : java.math.BigDecimal.ZERO);
        
        params.put("AMOUNT", amount.toString());
        if (tafqeet != null) {
            params.put("AMOUNT_WORDS", tafqeet.toArabicWords(amount));
        }
        
        return this;
    }

    public ReportParamBuilder put(String key, Object value) {
        params.put(key, value);
        return this;
    }

    public Map<String, Object> build() {
        return params;
    }
}
