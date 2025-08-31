package com.ddtt.exceptions;

public class DuplicateException extends RuntimeException {

    public DuplicateException(int accountId, int bookId) {
        super("Sách " + bookId + " đã tồn tại trong account " + accountId);
    }
}
