package com.ecomm.sb_ecomm.controllers;

import com.ecomm.sb_ecomm.models.AppRole;
import com.ecomm.sb_ecomm.models.Role;
import com.ecomm.sb_ecomm.models.User;
import com.ecomm.sb_ecomm.repositories.RoleRepository;
import com.ecomm.sb_ecomm.repositories.UserRepository;
import com.ecomm.sb_ecomm.security.jwt.JwtUtils;
import com.ecomm.sb_ecomm.security.request.LoginRequest;
import com.ecomm.sb_ecomm.security.request.SignupUser;
import com.ecomm.sb_ecomm.security.response.MessageResponse;
import com.ecomm.sb_ecomm.security.response.UserInfoResponse;
import com.ecomm.sb_ecomm.security.services.UserDetailsImpl;
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

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try{
            authentication = this.authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),loginRequest.getPassword()));
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

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), jwtCookie.toString(), roles);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,jwtCookie.toString())
                .body(response);

    }

    @PostMapping("signup")
    public ResponseEntity<?> signupUser(@RequestBody SignupUser  signedupUser) {

        if(this.userRepository.existsByUsername(signedupUser.getUsername())){
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if(this.userRepository.existsByEmail(signedupUser.getEmail())){
            return ResponseEntity.badRequest().body("Email already exists");
        }



        User user = new User(signedupUser.getUsername(),signedupUser.getEmail(),this.passwordEncoder.encode(signedupUser.getPassword()));
        Set<String> strRoles = signedupUser.getRoles();
        Set<Role> roles = new HashSet<>();
        if(strRoles==null){
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(()-> new RuntimeException("Error : Role not found"));
            roles.add(userRole);
        }
        else {
            strRoles.forEach(role->{
                switch (role){
                    case "admin":
                        Role adminRole = this.roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow(()-> new RuntimeException("Error : Role not found"));
                        roles.add(adminRole);
                        break;

                case "user":
                    Role useRole = this.roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow(()-> new RuntimeException("Error : Role not found"));
                    roles.add(useRole);
                    break;

                    case "seller":
                        Role sellerRole = this.roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow(()-> new RuntimeException("Error : Role not found"));
                        roles.add(sellerRole);
                        break;

                        default:
                            Role userRole = this.roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow(()-> new RuntimeException("Error : Role not found"));
                            roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        this.userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }

    @GetMapping("/username")
    public String getUsername(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        }
        else  {
            return "";
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities()
                .stream().map(item->item.getAuthority()).collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), userDetails.getPassword(), roles);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser(){
        ResponseCookie cookie = jwtUtils.getCleanCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie.toString())
                .body(new MessageResponse("You've been logged out successfully"));
    }
}
