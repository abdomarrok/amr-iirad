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
    /**
     * Converts a BigDecimal amount to Arabic words.
     */
    public String toArabicWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return "";
        try {
            long dinarPart = amount.longValue();
            int centPart = amount.subtract(new BigDecimal(dinarPart))
                    .multiply(new BigDecimal("100")).intValue();

            if (centPart > 0) {
                return tafqeet.doTafqeet(dinarPart) + " دينار و "
                        + tafqeet.doTafqeet(centPart) + " سنتيم";
            } else {
                return tafqeet.doTafqeet(dinarPart) + " دينار";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Converts a BigDecimal amount to French words.
     */
    public String toFrenchWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return "";
        long dinarPart = amount.longValue();
        int centPart = amount.subtract(new BigDecimal(dinarPart))
                .multiply(new BigDecimal("100")).intValue();

        String result = convertToFrench(dinarPart) + " Dinars";
        if (centPart > 0) {
            result += " et " + convertToFrench(centPart) + " Centimes";
        }
        return result;
    }

    private String convertToFrench(long n) {
        if (n == 0) return "zéro";
        if (n < 0) return "moins " + convertToFrench(-n);

        String[] units = {"", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"};
        String[] tens = {"", "dix", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante-dix", "quatre-vingt", "quatre-vingt-dix"};

        if (n < 20) return units[(int) n];
        if (n < 100) {
            int t = (int) (n / 10);
            int u = (int) (n % 10);
            if (t == 7 || t == 9) return tens[t-1] + "-" + convertToFrench(10 + u);
            if (u == 0) return tens[t];
            if (u == 1) return tens[t] + " et un";
            return tens[t] + "-" + units[u];
        }
        if (n < 1000) {
            long h = n / 100;
            long r = n % 100;
            String s = (h == 1 ? "" : convertToFrench(h) + " ") + "cent" + (h > 1 && r == 0 ? "s" : "");
            return r == 0 ? s : s + " " + convertToFrench(r);
        }
        if (n < 1000000) {
            long t = n / 1000;
            long r = n % 1000;
            String s = (t == 1 ? "" : convertToFrench(t) + " ") + "mille";
            return r == 0 ? s : s + " " + convertToFrench(r);
        }
        // Simplified for millions
        return String.valueOf(n); 
    }
}
