package com.campussync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "department")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "department_code", nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    @Column(name = "department_name", nullable = false, unique = true, length = 150)
    private String name;

    @Email
    @Size(max = 255)
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @OneToMany(mappedBy = "department")
    private Set<Student> students = new LinkedHashSet<>();

    @OneToMany(mappedBy = "department")
    private Set<Faculty> facultyMembers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "department")
    private Set<Subject> subjects = new LinkedHashSet<>();

    @OneToMany(mappedBy = "department")
    private Set<Committee> committees = new LinkedHashSet<>();
}
