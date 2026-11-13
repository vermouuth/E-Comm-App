package com.ecomm.sb_ecomm.auth.controllers;

import com.ecomm.sb_ecomm.auth.model.AppRole;
import com.ecomm.sb_ecomm.auth.model.Role;
import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.auth.repository.RoleRepository;
import com.ecomm.sb_ecomm.auth.repository.UserRepository;
import com.ecomm.sb_ecomm.auth.jwt.JwtUtils;
import com.ecomm.sb_ecomm.auth.payload.request.LoginRequest;
import com.ecomm.sb_ecomm.auth.payload.request.SignupUser;
import com.ecomm.sb_ecomm.auth.payload.response.MessageResponse;
import com.ecomm.sb_ecomm.auth.payload.response.UserInfoResponse;
import com.ecomm.sb_ecomm.auth.services.UserDetailsImpl;
import com.ecomm.sb_ecomm.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Auth", description = "Sign up, sign in and manage the current session.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "Sign in", description = "On success, sets the JWT auth cookie (HttpOnly). The token is not returned in the body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed in",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bad credentials")
    })
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try{
            authentication = this.authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername().trim(),loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Bad credentials");
            response.put("status", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        List<String>roles = userDetails.getAuthorities()
                .stream().map(item->item.getAuthority())
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,jwtCookie.toString())
                .body(response);

    }

    @Operation(summary = "Sign up", description = "Registers a new user with the USER role. Requested roles are ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Username or email already taken")
    })
    @PostMapping("signup")
    public ResponseEntity<?> signupUser(@Valid @RequestBody SignupUser signedupUser) {

        signedupUser.setUsername(signedupUser.getUsername().trim());
        signedupUser.setEmail(signedupUser.getEmail().trim().toLowerCase());

        if(this.userRepository.existsByUsername(signedupUser.getUsername())){
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if(this.userRepository.existsByEmail(signedupUser.getEmail())){
            return ResponseEntity.badRequest().body("Email already exists");
        }



        Users user = new Users(signedupUser.getUsername(),this.passwordEncoder.encode(signedupUser.getPassword()),signedupUser.getEmail());
        // Public signup always creates a plain USER. Elevated roles are granted by an admin, never
        // requested by the caller.
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error : Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        user.setRoles(roles);
        this.userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }

    @Operation(summary = "Get the current username",
            description = "Path is publicly reachable, but returns an empty string unless a valid auth cookie is present.")
    @ApiResponse(responseCode = "200", description = "Username, or an empty string if not authenticated",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping("/username")
    public String getUsername(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        }
        else  {
            return "";
        }
    }

    @Operation(summary = "Get the current user's details",
            description = "Requires a valid auth cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user's id, username and roles",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities()
                .stream().map(item->item.getAuthority()).collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles);
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Sign out", description = "Clears the JWT auth cookie.")
    @ApiResponse(responseCode = "200", description = "Signed out",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser(){
        ResponseCookie cookie = jwtUtils.getCleanCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie.toString())
                .body(new MessageResponse("You've been logged out successfully"));
    }
}
