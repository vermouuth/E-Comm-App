package com.ecomm.sb_ecomm.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity(name = "Categories")
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@Table(name = "categories")
@EqualsAndHashCode(callSuper = true)
public class Category extends BaseEntity {

    @NotBlank
    @Size(min = 5 , max = 100 , message = "Category must contain at least 5 letters")
    private String categoryName;

    @OneToMany(mappedBy = "category" ,cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Product> products;



}
