package com.ddtt;

import io.micronaut.http.annotation.*;

@Controller("/heThongSach")
public class HeThongSachController {

    @Get(uri="/", produces="text/plain")
    public String index() {
        return "Example Response";
    }
}