package com.ddtt.repository.conditions;

import static com.ddtt.jooq.generated.tables.Book.BOOK;
import org.jooq.Condition;


public final class BookConditions {
    // status: DRAFT, ONGOING, HIATUS, COMPLETED, UNLISTED
    public static Condition discoverableStatus() {
        return BOOK.DELETED_AT.isNull()
                .and(BOOK.STATUS.in("ONGOING", "HIATUS", "COMPLETED"));
    }
}
