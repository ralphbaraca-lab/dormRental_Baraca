package com.dormrental.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyUtil {
    private static final String CURRENCY_PREFIX = "PHP ";
    private static final String MISSING = "-";

    /** DecimalFormat isn't thread-safe, so each thread gets its own instance instead of one per call. */
    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP); // the usual rule for money (default is HALF_EVEN)
        return format;
    });

    private MoneyUtil() {
    }

    /** Formats an amount as "PHP 1,500.00", or "-" if there is no amount. (Plain text avoids console encoding problems with the peso sign.) */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return MISSING;
        }
        return CURRENCY_PREFIX + FORMAT.get().format(amount);
    }
}