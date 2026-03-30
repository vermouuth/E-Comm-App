package com.ecomm.sb_ecomm.security.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupUser {
    @Size(min = 4 , max = 20)
    @NotBlank
    private String username;

    @Size(min = 8, max = 50 , message = "password must be more that 8 characters")
    @NotBlank
    private String password;

    @Size(min = 8, max = 50)
    @Email
    private String email;

    Set<String> roles;

}
