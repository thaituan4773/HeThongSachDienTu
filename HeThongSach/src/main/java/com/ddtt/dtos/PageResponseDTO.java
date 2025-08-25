package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Value;


@Value
@Serdeable
public class PageResponseDTO<T> {
    Long total;      // tổng items tất cả page // Chỉ tính ở trang đầu, các trang còn lại null
    int page;        // page hiện tại (bắt đầu từ 1)
    int size;
    Integer totalPages; // tổng page // Chỉ tính ở trang đầu, các trang còn lại null
    List<T> items;
}
