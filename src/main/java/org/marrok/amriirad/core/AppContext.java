package org.marrok.amriirad.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.repository.*;
import org.marrok.amriirad.service.*;
import org.marrok.amriirad.controller.shared.*;
import org.marrok.amriirad.util.DatabaseConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight Dependency Injection container and application context.
 * Manages singleton instances of services and repositories.
 * Follows the GstockDz pattern with explicit fields for better type safety.
 */
public class AppContext implements Disposable {
    private static final Logger logger = LogManager.getLogger(AppContext.class);
    private static volatile AppContext instance;

    private final List<Disposable> disposables = new ArrayList<>();

    // Repositories
    private BudgetChapterRepository budgetChapterRepository;
    private DebtorRepository debtorRepository;
    private FiscalYearRepository fiscalYearRepository;
    private RevenueOrderRepository revenueOrderRepository;
    private CancellationOrderRepository cancellationOrderRepository;
    private DispatchSlipRepository dispatchSlipRepository;
    private InstitutionRepository institutionRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private AuditLogRepository auditLogRepository;
    private ZeroValueRepository zeroValueRepository;

    // Services
    private ConcurrencyManager concurrencyManager;
    private AuditService auditService;
    private TafqeetService tafqeetService;
    private ReportService reportService;
    private RevenueOrderService revenueOrderService;
    private CancellationOrderService cancellationOrderService;
    private DispatchSlipService dispatchSlipService;
    private AuthService authService;
    private InstitutionService institutionService;
    private AuditLogService auditLogService;
    private ExportService exportService;
    private ZeroValueService zeroValueService;
    private final Map<Class<?>, Object> instances = new HashMap<>();

    private AppContext() {
        logger.info("Initializing AppContext...");
        initializeCore();
        initializeRepositories();
        initializeServices();
        logger.info("AppContext initialized successfully");
    }

    public static AppContext getInstance() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) {
                    instance = new AppContext();
                }
            }
        }
        return instance;
    }

    private void initializeCore() {
        this.concurrencyManager = new ConcurrencyManager();
        registerDisposable(this.concurrencyManager);
    }

    private void initializeRepositories() {
        this.budgetChapterRepository = new BudgetChapterRepository();
        this.debtorRepository = new DebtorRepository();
        this.fiscalYearRepository = new FiscalYearRepository();
        this.revenueOrderRepository = new RevenueOrderRepository();
        this.cancellationOrderRepository = new CancellationOrderRepository();
        this.dispatchSlipRepository = new DispatchSlipRepository();
        this.institutionRepository = new InstitutionRepository();
        this.userRepository = new UserRepository();
        this.roleRepository = new RoleRepository();
        this.permissionRepository = new PermissionRepository();
        this.auditLogRepository = new AuditLogRepository();
        this.zeroValueRepository = new ZeroValueRepository();
    }

    private void initializeServices() {
        this.auditService = new AuditService();
        this.tafqeetService = new TafqeetService();
        this.reportService = new ReportService();

        this.revenueOrderService = new RevenueOrderService(
                revenueOrderRepository,
                auditService,
                fiscalYearRepository,
                tafqeetService
        );
        this.cancellationOrderService = new CancellationOrderService(
                cancellationOrderRepository,
                revenueOrderRepository,
                auditService
        );
        this.dispatchSlipService = new DispatchSlipService(
                dispatchSlipRepository,
                revenueOrderRepository,
                revenueOrderService,
                auditService
        );
        this.authService = new AuthService(userRepository);
        this.institutionService = new InstitutionService(institutionRepository, auditService);
        this.auditLogService = new AuditLogService(auditLogRepository);
        this.exportService = new ExportService();
        this.zeroValueService = new ZeroValueService(zeroValueRepository, revenueOrderRepository, auditService);
    }

    /**
     * Creates a new instance of the given class, injecting dependencies into its constructor if found.
     * Used as a ControllerFactory for JavaFX.
     */
    public Object createInstance(Class<?> clazz) {
        // 1. Check if it's a known singleton
        if (clazz == ConcurrencyManager.class) return concurrencyManager;
        if (clazz == AuditService.class) return auditService;
        if (clazz == TafqeetService.class) return tafqeetService;
        if (clazz == ReportService.class) return reportService;
        if (clazz == RevenueOrderService.class) return revenueOrderService;
        if (clazz == CancellationOrderService.class) return cancellationOrderService;
        if (clazz == DispatchSlipService.class) return dispatchSlipService;
        if (clazz == AuthService.class) return authService;
        if (clazz == InstitutionService.class) return institutionService;
        if (clazz == AuditLogService.class) return auditLogService;
        if (clazz == ExportService.class) return exportService;
        if (clazz == ZeroValueService.class) return zeroValueService;
        
        if (clazz == BudgetChapterRepository.class) return budgetChapterRepository;
        if (clazz == DebtorRepository.class) return debtorRepository;
        if (clazz == FiscalYearRepository.class) return fiscalYearRepository;
        if (clazz == RevenueOrderRepository.class) return revenueOrderRepository;
        if (clazz == CancellationOrderRepository.class) return cancellationOrderRepository;
        if (clazz == DispatchSlipRepository.class) return dispatchSlipRepository;
        // Controllers (Explicit registrations for complex ones or sub-packages)
        if (clazz == org.marrok.amriirad.controller.dashboard.DashboardController.class) 
            return new org.marrok.amriirad.controller.dashboard.DashboardController(fiscalYearRepository, revenueOrderRepository, authService);
        
        if (clazz == org.marrok.amriirad.controller.users.UserManagementController.class) 
            return new org.marrok.amriirad.controller.users.UserManagementController(userRepository);
        
        if (clazz == org.marrok.amriirad.controller.users.PermissionManagementController.class) 
            return new org.marrok.amriirad.controller.users.PermissionManagementController(roleRepository, permissionRepository);
        
        if (clazz == org.marrok.amriirad.controller.users.UserFormController.class) 
            return new org.marrok.amriirad.controller.users.UserFormController(userRepository, roleRepository);
        
        if (clazz == org.marrok.amriirad.controller.orders.RevenueOrderListController.class) 
            return new org.marrok.amriirad.controller.orders.RevenueOrderListController(revenueOrderService, revenueOrderRepository, fiscalYearRepository, authService, exportService, concurrencyManager);
        
        if (clazz == org.marrok.amriirad.controller.orders.RevenueOrderFormController.class) 
            return new org.marrok.amriirad.controller.orders.RevenueOrderFormController(fiscalYearRepository, debtorRepository, budgetChapterRepository, revenueOrderService, reportService, tafqeetService, institutionService, authService, concurrencyManager);
        
        if (clazz == org.marrok.amriirad.controller.orders.OrderDetailsController.class) 
            return new org.marrok.amriirad.controller.orders.OrderDetailsController(reportService, tafqeetService, institutionService, cancellationOrderService, authService, concurrencyManager);
        
        if (clazz == org.marrok.amriirad.controller.orders.BudgetChapterFormController.class) 
            return new org.marrok.amriirad.controller.orders.BudgetChapterFormController(budgetChapterRepository, concurrencyManager);
        
        if (clazz == org.marrok.amriirad.controller.budget.BudgetChapterListController.class) 
            return new org.marrok.amriirad.controller.budget.BudgetChapterListController(budgetChapterRepository, authService, concurrencyManager);
        
        if (clazz == org.marrok.amriirad.controller.orders.CancellationFormController.class) 
            return new org.marrok.amriirad.controller.orders.CancellationFormController(cancellationOrderService, reportService, tafqeetService, concurrencyManager);
        
        if (clazz == org.marrok.amriirad.controller.debtors.DebtorListController.class) 
            return new org.marrok.amriirad.controller.debtors.DebtorListController(debtorRepository, authService, concurrencyManager);
        
        if (clazz == org.marrok.amriirad.controller.debtors.DebtorFormController.class) 
            return new org.marrok.amriirad.controller.debtors.DebtorFormController(debtorRepository, concurrencyManager);
            
        if (clazz == org.marrok.amriirad.controller.dispatch.DispatchSlipController.class) 
            return new org.marrok.amriirad.controller.dispatch.DispatchSlipController(dispatchSlipRepository, fiscalYearRepository, reportService, tafqeetService, institutionService, authService, concurrencyManager);
            
        if (clazz == org.marrok.amriirad.controller.dispatch.DispatchSlipFormController.class) 
            return new org.marrok.amriirad.controller.dispatch.DispatchSlipFormController(fiscalYearRepository, revenueOrderRepository, dispatchSlipService, reportService, tafqeetService, authService, concurrencyManager);
            
        if (clazz == org.marrok.amriirad.controller.settings.EnterpriseInfoController.class) 
            return new org.marrok.amriirad.controller.settings.EnterpriseInfoController(institutionService);
            
        if (clazz == org.marrok.amriirad.controller.settings.ModeSelectionController.class) 
            return new org.marrok.amriirad.controller.settings.ModeSelectionController();
            
        if (clazz == org.marrok.amriirad.controller.settings.ServerConfigController.class) 
            return new org.marrok.amriirad.controller.settings.ServerConfigController();
            
        if (clazz == org.marrok.amriirad.controller.settings.AuditLogController.class)
            return new org.marrok.amriirad.controller.settings.AuditLogController(auditLogService, concurrencyManager);

        if (clazz == org.marrok.amriirad.controller.orders.ZeroValueListController.class)
            return new org.marrok.amriirad.controller.orders.ZeroValueListController(zeroValueService, fiscalYearRepository, concurrencyManager);

        if (clazz == org.marrok.amriirad.controller.orders.ZeroValueFormController.class)
            return new org.marrok.amriirad.controller.orders.ZeroValueFormController(zeroValueService, revenueOrderService, fiscalYearRepository, concurrencyManager);

        if (clazz == org.marrok.amriirad.controller.login.LoginController.class) 
            return new org.marrok.amriirad.controller.login.LoginController(authService, concurrencyManager);

        if (clazz == TopBarController.class) return new TopBarController(fiscalYearRepository, authService);
        if (clazz == FooterController.class) return new FooterController(authService);
        
        if (clazz == BudgetChapterRepository.class) return budgetChapterRepository;

        // 2. Try constructor injection
        for (java.lang.reflect.Constructor<?> ctor : clazz.getConstructors()) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            boolean allFound = true;

            for (int i = 0; i < paramTypes.length; i++) {
                Object dependency = getBean(paramTypes[i]);
                if (dependency == null) {
                    allFound = false;
                    break;
                }
                args[i] = dependency;
            }

            if (allFound) {
                try {
                    return ctor.newInstance(args);
                } catch (Exception e) {
                    logger.error("Failed to inject dependencies for " + clazz.getName(), e);
                    throw new RuntimeException("DI failed for " + clazz.getName(), e);
                }
            }
        }

        // 3. Fallback to default constructor
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Failed to instantiate " + clazz.getName(), e);
            throw new RuntimeException("Instantiation failed for " + clazz.getName(), e);
        }
    }

    public Object getBean(Class<?> clazz) {
        if (clazz == ConcurrencyManager.class) return concurrencyManager;
        if (clazz == AuditService.class) return auditService;
        if (clazz == TafqeetService.class) return tafqeetService;
        if (clazz == ReportService.class) return reportService;
        if (clazz == RevenueOrderService.class) return revenueOrderService;
        if (clazz == CancellationOrderService.class) return cancellationOrderService;
        if (clazz == DispatchSlipService.class) return dispatchSlipService;
        if (clazz == AuthService.class) return authService;
        if (clazz == InstitutionService.class) return institutionService;
        if (clazz == ExportService.class) return exportService;
        
        if (clazz == BudgetChapterRepository.class) return budgetChapterRepository;
        if (clazz == DebtorRepository.class) return debtorRepository;
        if (clazz == FiscalYearRepository.class) return fiscalYearRepository;
        if (clazz == RevenueOrderRepository.class) return revenueOrderRepository;
        if (clazz == CancellationOrderRepository.class) return cancellationOrderRepository;
        if (clazz == DispatchSlipRepository.class) return dispatchSlipRepository;
        if (clazz == InstitutionRepository.class) return institutionRepository;
        if (clazz == UserRepository.class) return userRepository;
        if (clazz == RoleRepository.class) return roleRepository;
        if (clazz == PermissionRepository.class) return permissionRepository;
        if (clazz == AuditLogRepository.class) return auditLogRepository;
        
        return null;
    }

    public String getCurrentUser() {
        return authService != null && authService.getCurrentUser() != null 
            ? authService.getCurrentUser().getUsername() 
            : "guest";
    }

    public ConcurrencyManager getConcurrencyManager() { return concurrencyManager; }
    public AuditService getAuditService() { return auditService; }
    public TafqeetService getTafqeetService() { return tafqeetService; }
    public ReportService getReportService() { return reportService; }
    public RevenueOrderService getRevenueOrderService() { return revenueOrderService; }
    public CancellationOrderService getCancellationOrderService() { return cancellationOrderService; }
    public DispatchSlipService getDispatchSlipService() { return dispatchSlipService; }
    public AuthService getAuthService() { return authService; }
    public InstitutionService getInstitutionService() { return institutionService; }
    public AuditLogService getAuditLogService() { return auditLogService; }

    public BudgetChapterRepository getBudgetChapterRepository() { return budgetChapterRepository; }
    public DebtorRepository getDebtorRepository() { return debtorRepository; }
    public FiscalYearRepository getFiscalYearRepository() { return fiscalYearRepository; }
    public RevenueOrderRepository getRevenueOrderRepository() { return revenueOrderRepository; }
    public CancellationOrderRepository getCancellationOrderRepository() { return cancellationOrderRepository; }
    public DispatchSlipRepository getDispatchSlipRepository() { return dispatchSlipRepository; }
    public InstitutionRepository getInstitutionRepository() { return institutionRepository; }
    public UserRepository getUserRepository() { return userRepository; }
    public RoleRepository getRoleRepository() { return roleRepository; }
    public PermissionRepository getPermissionRepository() { return permissionRepository; }

    public void setCurrentUser(String user) {
        // Redundant with AuthService now handling sessions
    }

    public void registerDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

    @Override
    public void dispose() {
        logger.info("AppContext shutting down...");
        for (int i = disposables.size() - 1; i >= 0; i--) {
            try {
                disposables.get(i).dispose();
            } catch (Exception e) {
                logger.error("Error disposing resource", e);
            }
        }
        DatabaseConnection.shutdown();
        logger.info("AppContext shutdown complete");
    }
}
