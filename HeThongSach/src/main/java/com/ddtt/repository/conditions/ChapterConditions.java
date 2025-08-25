package com.ddtt.repository.conditions;

import com.ddtt.jooq.generated.tables.Chapter;
import org.jooq.Condition;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;

public final class ChapterConditions {

    private ChapterConditions() {}

    public static Condition isPublished() {
        // status: DRAFT, PUBLISHED, UNLISTED
        return CHAPTER.STATUS.eq("PUBLISHED")
                .and(CHAPTER.DELETED_AT.isNull());
    }
    
    public static Condition isPublished(Chapter alias) {
        return alias.STATUS.eq("PUBLISHED")
                .and(alias.DELETED_AT.isNull());
    }
}
