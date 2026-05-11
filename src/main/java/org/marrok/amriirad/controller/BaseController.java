package org.marrok.amriirad.controller;

import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;

/**
 * Base class for non-modal controllers (like lists) to unify
 * common dependencies like ConcurrencyManager.
 */
public abstract class BaseController {
    protected final ConcurrencyManager concurrencyManager;

    protected BaseController(ConcurrencyManager concurrencyManager) {
        this.concurrencyManager = concurrencyManager;
    }

    protected abstract Logger getLogger();
}
