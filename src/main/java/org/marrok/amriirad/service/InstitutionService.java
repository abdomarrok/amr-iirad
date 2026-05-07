package org.marrok.amriirad.service;

import org.marrok.amriirad.model.InstitutionInfo;
import org.marrok.amriirad.repository.InstitutionRepository;

public class InstitutionService {
    private final InstitutionRepository repository;
    private final AuditService auditService;

    public InstitutionService(InstitutionRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public InstitutionInfo getInfo() {
        return repository.getInfo();
    }

    public void updateInfo(InstitutionInfo info) {
        repository.update(info);
        String currentUser = org.marrok.amriirad.core.AppContext.getInstance().getCurrentUser();
        auditService.log("institution_info", 1, AuditService.Action.UPDATE, currentUser != null ? currentUser : "admin", "Updated institution settings");
    }
}
