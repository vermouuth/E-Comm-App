package com.ecomm.sb_ecomm.exceptions.newexceptions;

import java.io.Serial;

public class ApiException extends RuntimeException {

    @Serial
    private static  final  long serialVersionUID = 1L;

    public ApiException(String message) {
        super(message);
    }


}
