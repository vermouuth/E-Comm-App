package com.ecomm.sb_ecomm.auth.payload.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Clients must never choose their own roles; a legacy "roles" field in the JSON is ignored.
@JsonIgnoreProperties(value = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupUser {
    @NotBlank(message = "username is required")
    @Size(min = 5, max = 20, message = "username must be 5-20 characters")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 50, message = "password must be 8-50 characters")
    private String password;

    @NotBlank(message = "email is required")
    @Size(min = 8, max = 50, message = "email must be 8-50 characters")
    @Email(message = "email must be a valid email like name@example.com")
    private String email;

}
