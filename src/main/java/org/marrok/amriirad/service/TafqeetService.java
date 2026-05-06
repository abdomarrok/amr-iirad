package org.marrok.amriirad.service;

import io.github.osamabmaq.tafqeetj.converters.Tafqeet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts numeric amounts to Arabic words (التفقيط).
 * Reuses the same Tafqeet pattern from GstockDz (MoneyCalculationUtil + DetailViewController).
 *
 * Business Rule: Amount-in-words is ALWAYS computed — never accepted from user input (INS-02, RO-10).
 */
public class TafqeetService {

    private static final Logger logger = LogManager.getLogger(TafqeetService.class);

    private final Tafqeet tafqeet = Tafqeet.getInstance();

    /**
     * Converts a BigDecimal amount to Arabic words with dinar/centime separation.
     * Pattern borrowed from GstockDz's MoneyCalculationUtil.calculateTafqeetParts().
     *
     * Example: 15000.50 → "خمسة عشر ألف دينار و خمسون سنتيم"
     *
     * @param amount the numeric amount (must be > 0)
     * @return Arabic text representation of the amount
     * @throws IllegalArgumentException if amount is null or ≤ 0
     */
    public String toArabicWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر");
        }

        try {
            // Split into dinar and centime parts (same as GstockDz)
            long dinarPart = amount.longValue();
            BigDecimal centPartDecimal = amount.subtract(new BigDecimal(dinarPart))
                    .multiply(new BigDecimal("100"));
            int centPart = centPartDecimal.setScale(0, RoundingMode.DOWN).intValue();

            if (centPart > 0) {
                return tafqeet.doTafqeet(dinarPart) + " دينار و "
                        + tafqeet.doTafqeet(centPart) + " سنتيم";
            } else {
                return tafqeet.doTafqeet(dinarPart) + " دينار";
            }
        } catch (Exception e) {
            logger.warn("Tafqeet conversion failed for amount: {}", amount, e);
            return "";
        }
    }
}
