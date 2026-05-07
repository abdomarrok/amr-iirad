package org.marrok.amriirad;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {

        logger.info("org.marrok.amriirad.App lunched");
        AmrIiradApp.main(args);
    }
}
