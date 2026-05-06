package org.marrok.amriirad.core;

/**
 * Interface for resources that need to be explicitly disposed/closed
 * during application shutdown.
 */
public interface Disposable {
    void dispose();
}
