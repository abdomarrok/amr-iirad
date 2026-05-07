package org.marrok.amriirad.util;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages the embedded MariaDB4j instance for LOCAL mode.
 * Data is stored in ~/.amriirad/data so it survives application restarts.
 */
public class EmbeddedDatabase {

    private static final Logger logger = LogManager.getLogger(EmbeddedDatabase.class);
    private static final int DEFAULT_PORT = 3307;

    private DB db;
    private int port;
    private boolean running = false;

    public void start() throws Exception {
        if (running) {
            logger.info("Embedded MariaDB4j is already running.");
            return;
        }
        logger.info("Starting embedded MariaDB4j database...");
        DBConfigurationBuilder config = DBConfigurationBuilder.newBuilder();
        config.setPort(DEFAULT_PORT);
        config.setDataDir(System.getProperty("user.home") + "/.amriirad/data");

        db = DB.newEmbeddedDB(config.build());
        db.start();
        port = DEFAULT_PORT;
        running = true;
        logger.info("Embedded MariaDB4j started on port {}", port);
    }

    public void stop() {
        if (db != null && running) {
            try {
                db.stop();
                running = false;
                logger.info("Embedded MariaDB4j stopped.");
            } catch (Exception e) {
                logger.error("Error stopping embedded database", e);
            }
        }
    }

    /** Returns the port on which the embedded database is listening. */
    public int getPort() {
        return port;
    }
}
