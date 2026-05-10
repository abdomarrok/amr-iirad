package org.marrok.amriirad.util;

import org.marrok.amriirad.model.Debtor;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.model.RevenueOrderCancellation;
import org.marrok.amriirad.model.InstitutionInfo;
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
    private org.marrok.amriirad.model.PrintLanguage language = org.marrok.amriirad.model.PrintLanguage.ARABIC;

    private ReportParamBuilder(TafqeetService tafqeet) {
        this.tafqeet = tafqeet;
    }

    public static ReportParamBuilder create(TafqeetService tafqeet) {
        return new ReportParamBuilder(tafqeet);
    }

    public ReportParamBuilder withLanguage(org.marrok.amriirad.model.PrintLanguage lang) {
        this.language = lang;
        params.put("REPORT_LOCALE", lang == org.marrok.amriirad.model.PrintLanguage.ARABIC ? new java.util.Locale("ar") : java.util.Locale.FRENCH);
        params.put("PRINT_LANGUAGE", lang.getCode());
        return this;
    }

    public ReportParamBuilder withOrder(RevenueOrder order) {
        params.put("ORDER_NUMBER", order.getOrderNumber() != null ? order.getOrderNumber() : "");
        params.put("FISCAL_YEAR", order.getFiscalYear() != null ? order.getFiscalYear().getYearLabel() : "");
        params.put("ISSUE_DATE", order.getIssueDate() != null ? order.getIssueDate().toString() : "");
        params.put("AMOUNT", order.getAmount() != null ? String.format("%,.2f", order.getAmount()) : "0.00");
        params.put("AMOUNT_RAW", order.getAmount() != null ? order.getAmount().toString() : "0.00");
        
        if (tafqeet != null && order.getAmount() != null) {
            if (language == org.marrok.amriirad.model.PrintLanguage.FRENCH) {
                params.put("AMOUNT_WORDS", tafqeet.toFrenchWords(order.getAmount()));
            } else {
                params.put("AMOUNT_WORDS", tafqeet.toArabicWords(order.getAmount()));
            }
        }

        if (language == org.marrok.amriirad.model.PrintLanguage.FRENCH) {
            params.put("OBJECT", order.getObjectFr() != null ? order.getObjectFr() : "");
            params.put("LIQUIDATION_BASIS", order.getObjectFr() != null ? order.getObjectFr() : "");
        } else {
            params.put("OBJECT", order.getObjectAr() != null ? order.getObjectAr() : "");
            params.put("LIQUIDATION_BASIS", order.getObjectAr() != null ? order.getObjectAr() : "");
        }
        // Legacy support if JRXMLs still use REASON_AR
        params.put("REASON_AR", language == org.marrok.amriirad.model.PrintLanguage.FRENCH ? 
            (order.getObjectFr() != null ? order.getObjectFr() : "") : 
            (order.getObjectAr() != null ? order.getObjectAr() : ""));

        params.put("TREASURY_REF", ""); 

        if (order.getBudgetChapter() != null) {
            params.put("BUDGET_CHAPTER", order.getBudgetChapter().getCode());
            params.put("BUDGET_CODE", order.getBudgetChapter().getCode());
            params.put("BUDGET_LABEL", language == org.marrok.amriirad.model.PrintLanguage.FRENCH ? 
                order.getBudgetChapter().getLabelFr() : order.getBudgetChapter().getLabelAr());

            // Try to extract LOLF components from code (assuming dots separator)
            String[] parts = order.getBudgetChapter().getCode().split("\\.");
            if (parts.length >= 1) params.put("PORTEFEUILLE", parts[0]);
            if (parts.length >= 2) params.put("PROGRAMME", parts[1]);
            if (parts.length >= 3) params.put("ACTION", parts[2]);
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

    public ReportParamBuilder withInstitution(InstitutionInfo info) {
        if (info == null) return this;
        
        // Language-aware unified parameters
        boolean isFr = (language == org.marrok.amriirad.model.PrintLanguage.FRENCH);
        params.put("MINISTRY_NAME", isFr ? info.getMinistryNameFr() : info.getMinistryNameAr());
        params.put("INSTITUTION_NAME", isFr ? info.getNameFr() : info.getNameAr());
        params.put("WILAYA", isFr ? info.getWilayaFr() : info.getWilayaAr());
        params.put("OFFICER_NAME", isFr ? info.getAuthorizingOfficerFr() : info.getAuthorizingOfficerAr());
        params.put("TREASURY_NAME", isFr ? info.getTreasuryNameFr() : info.getTreasuryAccountAr());
        params.put("INSTITUTION_ADDRESS", isFr ? info.getAddressFr() : info.getAddressAr());

        // Legacy/Direct parameters
        params.put("MINISTRY_NAME_AR", info.getMinistryNameAr());
        params.put("MINISTRY_NAME_FR", info.getMinistryNameFr());
        params.put("INSTITUTION_NAME_AR", info.getNameAr());
        params.put("INSTITUTION_NAME_FR", info.getNameFr());
        params.put("ORDONNATEUR_CODE", info.getOrdonnateurCode());
        params.put("OFFICER_NAME_AR", info.getAuthorizingOfficerAr());
        params.put("WILAYA_AR", info.getWilayaAr());
        params.put("TREASURY_REF", info.getTreasuryAccountAr());
        params.put("TREASURY_NAME_FR", info.getTreasuryNameFr());
        return this;
    }

    public ReportParamBuilder withCancellation(RevenueOrderCancellation cancel) {
        params.put("CANCEL_NUMBER", cancel.getCancellationNumber() != null ? cancel.getCancellationNumber() : "");
        params.put("DATE", cancel.getCancellationDate() != null ? cancel.getCancellationDate().toString() : "");
        if (language == org.marrok.amriirad.model.PrintLanguage.FRENCH) {
            params.put("REASON", cancel.getReasonFr() != null ? cancel.getReasonFr() : "");
        } else {
            params.put("REASON", cancel.getReasonAr() != null ? cancel.getReasonAr() : "");
        }
        
        if (cancel.getOriginalOrder() != null) {
            params.put("ORDER_NUMBER", cancel.getOriginalOrder().getOrderNumber());
            withDebtor(cancel.getOriginalOrder().getDebtor());
        }
        
        var amount = cancel.getReducedAmount() != null ? cancel.getReducedAmount() : 
                    (cancel.getOriginalOrder() != null ? cancel.getOriginalOrder().getAmount() : java.math.BigDecimal.ZERO);
        
        params.put("AMOUNT", amount.toString());
        if (tafqeet != null) {
            if (language == org.marrok.amriirad.model.PrintLanguage.FRENCH) {
                params.put("AMOUNT_WORDS", tafqeet.toFrenchWords(amount));
            } else {
                params.put("AMOUNT_WORDS", tafqeet.toArabicWords(amount));
            }
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
