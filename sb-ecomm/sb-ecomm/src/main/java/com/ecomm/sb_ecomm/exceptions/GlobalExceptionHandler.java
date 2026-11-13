package com.ecomm.sb_ecomm.exceptions;

import com.ecomm.sb_ecomm.exceptions.ErrorResponse.InvalidField;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_BODY = "Request body is invalid, please fix and resend";

    private static ResponseEntity<ErrorResponse> reply(HttpStatus status, ErrorResponse body) {
        return ResponseEntity.status(status).body(body);
    }

    private static ResponseEntity<ErrorResponse> reply(HttpStatus status, String message, HttpServletRequest req) {
        return reply(status, ErrorResponse.of(status.value(), message, req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> missing = new ArrayList<>();
        List<InvalidField> invalid = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            if (e instanceof FieldError fe) {
                if (fe.getRejectedValue() == null) missing.add(fe.getField());
                else invalid.add(new InvalidField(fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()));
            } else {
                invalid.add(new InvalidField(e.getObjectName(), null, e.getDefaultMessage()));
            }
        });
        return reply(HttpStatus.BAD_REQUEST,
                ErrorResponse.fields(400, INVALID_BODY, missing, invalid, req.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        Throwable cause = ex.getCause();
        if (cause instanceof UnrecognizedPropertyException upe) {
            Collection<Object> known = upe.getKnownPropertyIds();
            List<String> allowed = known == null ? List.of() : known.stream().map(String::valueOf).sorted().toList();
            return reply(HttpStatus.BAD_REQUEST,
                    ErrorResponse.of(400, "Unknown field '" + upe.getPropertyName() + "'", req.getRequestURI())
                            .withAllowed(allowed));
        }
        if (cause instanceof InvalidFormatException ife) {
            return reply(HttpStatus.BAD_REQUEST, wrongType(path(ife), ife.getValue(), ife.getTargetType(), req));
        }
        if (cause instanceof MismatchedInputException mie) {
            if (mie.getPath().isEmpty()) {
                return reply(HttpStatus.BAD_REQUEST, "Request body is missing or has the wrong shape", req);
            }
            return reply(HttpStatus.BAD_REQUEST, wrongType(path(mie), null, mie.getTargetType(), req));
        }
        return reply(HttpStatus.BAD_REQUEST, "Malformed JSON, check commas, quotes and brackets", req);
    }

    private static ErrorResponse wrongType(String field, Object received, Class<?> expected, HttpServletRequest req) {
        String type = expected == null ? "a different type" : expected.getSimpleName();
        return ErrorResponse.fields(400, "Wrong data type", List.of(),
                List.of(new InvalidField(field, received, "expected " + type)), req.getRequestURI());
    }

    /** a.b[2].c style path: list elements have an index and no name. */
    private static String path(JsonMappingException ex) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference r : ex.getPath()) {
            if (r.getFieldName() != null) {
                if (!sb.isEmpty()) sb.append('.');
                sb.append(r.getFieldName());
            } else if (r.getIndex() >= 0) {
                sb.append('[').append(r.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String expected = ex.getRequiredType() == null ? "a different type" : ex.getRequiredType().getSimpleName();
        return reply(HttpStatus.BAD_REQUEST, ErrorResponse.fields(400,
                "Wrong data type for parameter '" + ex.getName() + "'", List.of(),
                List.of(new InvalidField(ex.getName(), ex.getValue(), "expected " + expected)), req.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return reply(HttpStatus.BAD_REQUEST, violations(ex.getConstraintViolations(), req));
    }

    // Bean-validation failures raised at flush time are wrapped in TransactionSystemException.
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransaction(TransactionSystemException ex, HttpServletRequest req) {
        if (ex.getRootCause() instanceof ConstraintViolationException cve) {
            return reply(HttpStatus.BAD_REQUEST, violations(cve.getConstraintViolations(), req));
        }
        log.error("Transaction failure on {}", req.getRequestURI(), ex);
        return reply(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);
    }

    private static ErrorResponse violations(Set<ConstraintViolation<?>> set, HttpServletRequest req) {
        List<InvalidField> invalid = set.stream()
                .map(v -> new InvalidField(leaf(v.getPropertyPath()), v.getInvalidValue(), v.getMessage()))
                .toList();
        return ErrorResponse.fields(400, "Request parameters are invalid, please fix and resend",
                List.of(), invalid, req.getRequestURI());
    }

    /** "addToCart.quantity" becomes "quantity" (drops the method name prefix). */
    private static String leaf(Path p) {
        String last = null;
        for (Path.Node n : p) last = n.getName();
        return last;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return reply(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return reply(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) throws Exception {
        // Security exceptions belong to Spring Security's 401/403 handling, not a 500.
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) throw ex;
        // Spring MVC exceptions (405, 404, 415, missing param/header...) already carry their own status.
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            return reply(HttpStatus.valueOf(er.getStatusCode().value()), er.getBody().getDetail(), req);
        }
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return reply(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);
    }
}
