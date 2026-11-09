package com.dormrental.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyUtil {
    private MoneyUtil() {
    }

    /** Formats an amount as "PHP 1,500.00". (Plain text avoids console encoding problems with the peso sign.) */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return "PHP " + format.format(amount);
    }
}
