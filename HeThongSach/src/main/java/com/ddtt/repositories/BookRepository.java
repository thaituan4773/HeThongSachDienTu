package com.ddtt.repositories;

import com.ddtt.dtos.BookDTO;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import java.util.List;
import org.jooq.DSLContext;

@Singleton
@Blocking
public class BookRepository {
    private final DSLContext dsl;

    public BookRepository(DSLContext dsl) {
        this.dsl = dsl;
    }
    
    public List<BookDTO> findAllBooks(){
        return dsl.selectFrom(BOOK).fetch(record -> new BookDTO(
                record.getBookId(),
                record.getTitle(),
                null,
                null
        ));
    }
}
