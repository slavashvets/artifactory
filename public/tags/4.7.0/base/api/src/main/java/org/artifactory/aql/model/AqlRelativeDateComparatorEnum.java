package org.artifactory.aql.model;

import org.artifactory.aql.AqlParserException;
import org.joda.time.*;

/**
 * @author gidis
 */
public enum AqlRelativeDateComparatorEnum {
    lastYears("$lastYears", AqlComparatorEnum.greater),
    lastDays("$lastDays", AqlComparatorEnum.greater),
    lastMinutes("$lastMinutes", AqlComparatorEnum.greater),
    lastSeconds("$lastSeconds", AqlComparatorEnum.greater),
    beforeYears("$beforeYears", AqlComparatorEnum.less),
    beforeDays("$beforeDays", AqlComparatorEnum.less),
    beforeMinutes("$beforeMinutes", AqlComparatorEnum.less),
    beforeSeconds("$beforeSeconds", AqlComparatorEnum.less);

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
        DateTime now = DateTime.now();
        switch (this){
            case lastYears:return now.minus(Years.years(Integer.parseInt(value))).getMillis();
            case lastDays:return now.minus(Days.days(Integer.parseInt(value))).getMillis();
            case lastMinutes:return now.minus(Minutes.minutes(Integer.parseInt(value))).getMillis();
            case lastSeconds:return now.minus(Seconds.seconds(Integer.parseInt(value))).getMillis();
            case beforeYears:return now.minus(Years.years(Integer.parseInt(value))).getMillis();
            case beforeDays:return now.minus(Days.days(Integer.parseInt(value))).getMillis();
            case beforeMinutes:return now.minus(Minutes.minutes(Integer.parseInt(value))).getMillis();
            case beforeSeconds:return now.minus(Seconds.seconds(Integer.parseInt(value))).getMillis();
        }
        throw new AqlParserException("unexpected exception please handle the:"+this.name()+ " enum");
    }
}
