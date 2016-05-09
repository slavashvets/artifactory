package org.artifactory.aql.model;

import org.apache.commons.compress.archivers.dump.InvalidFormatException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Date;

/**
 * @author gidis
 */
public enum AqlRelativeDateComparatorEnum {
    last("$last", AqlComparatorEnum.greater),
    before("$before", AqlComparatorEnum.less);

    public String signature;
    public AqlComparatorEnum aqlComparatorEnum;

    AqlRelativeDateComparatorEnum(String signature, AqlComparatorEnum aqlComparatorEnum) {
        this.signature = signature;
        this.aqlComparatorEnum = aqlComparatorEnum;
    }

    public static AqlRelativeDateComparatorEnum value(String comparator) {
        for (AqlRelativeDateComparatorEnum comparatorEnum : values()) {
            if (comparatorEnum.signature.equals(comparator)) {
                return comparatorEnum;
            }
        }
        return null;
    }

    public long toDate(String value) {
        try {
            PeriodFormatter formatter = new PeriodFormatterBuilder()
                    .appendMillis().appendSuffix("millis")
                    .appendMillis().appendSuffix("ms")
                    .appendMinutes().appendSuffix("minutes")
                    .appendMinutes().appendSuffix("mi")
                    .appendDays().appendSuffix("days")
                    .appendDays().appendSuffix("d")
                    .appendMonths().appendSuffix("months")
                    .appendMonths().appendSuffix("mo")
                    .appendYears().appendSuffix("years")
                    .appendYears().appendSuffix("y")
                    .appendSeconds().appendSuffix("seconds")
                    .appendSeconds().appendSuffix("s")
                    .appendWeeks().appendSuffix("weeks")
                    .appendWeeks().appendSuffix("w")
                    .toFormatter();
            Period period = formatter.parsePeriod(value);
            DateTime now = DateTime.now();
            return now.minus(period).getMillis();
        }catch (IllegalArgumentException e){
            return -1;
        }
    }
}
