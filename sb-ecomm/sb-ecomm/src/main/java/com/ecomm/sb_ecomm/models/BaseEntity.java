package com.ecomm.sb_ecomm.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

@Data
@MappedSuperclass
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BaseEntity {



    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(allocationSize = 50, name = "")
    private Long id;

    @Column(updatable = false, nullable = false)
    private Date creationalDate;

    @Column(insertable = false)
    private Date modificationalDate;


    private String createdBy;

    private String modifiedBy;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "SYSTEM";
        }

        return auth.getName();
    }

    @PrePersist
    public void prePersist() {
        this.creationalDate = new Date();
        this.createdBy = getCurrentUsername();
    }

    @PreUpdate
    public void preUpdateOnModifiedDate() {
        this.modificationalDate = new Date();
        this.modifiedBy = getCurrentUsername();
    }


}
