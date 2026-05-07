package org.marrok.amriirad.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.repository.*;
import org.marrok.amriirad.service.*;
import org.marrok.amriirad.util.DatabaseConnection;

import java.util.ArrayList;
import java.util.List;

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

    // Services
    private ConcurrencyManager concurrencyManager;
    private AuditService auditService;
    private TafqeetService tafqeetService;
    private ReportService reportService;
    private RevenueOrderService revenueOrderService;
    private CancellationOrderService cancellationOrderService;
    private DispatchSlipService dispatchSlipService;

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
        
        if (clazz == BudgetChapterRepository.class) return budgetChapterRepository;
        if (clazz == DebtorRepository.class) return debtorRepository;
        if (clazz == FiscalYearRepository.class) return fiscalYearRepository;
        if (clazz == RevenueOrderRepository.class) return revenueOrderRepository;
        if (clazz == CancellationOrderRepository.class) return cancellationOrderRepository;
        if (clazz == DispatchSlipRepository.class) return dispatchSlipRepository;

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
        
        if (clazz == BudgetChapterRepository.class) return budgetChapterRepository;
        if (clazz == DebtorRepository.class) return debtorRepository;
        if (clazz == FiscalYearRepository.class) return fiscalYearRepository;
        if (clazz == RevenueOrderRepository.class) return revenueOrderRepository;
        if (clazz == CancellationOrderRepository.class) return cancellationOrderRepository;
        if (clazz == DispatchSlipRepository.class) return dispatchSlipRepository;
        
        return null;
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
