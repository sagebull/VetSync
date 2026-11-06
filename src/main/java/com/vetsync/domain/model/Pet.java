package com.vetsync.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pets")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String species;

    @Column(nullable = false, length = 120)
    private String breed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Sex sex;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, precision = 7, scale = 3)
    private BigDecimal weight;

    @Column(length = 2000)
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pets_owner"))
    private Owner owner;

    protected Pet() {
    }

    public Pet(String name, String species, String breed, Sex sex, LocalDate birthDate,
               BigDecimal weight, String observations, Owner owner) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.sex = sex;
        this.birthDate = birthDate;
        this.weight = weight;
        this.observations = observations;
        this.owner = owner;
    }

    public void update(String name, String species, String breed, Sex sex, LocalDate birthDate,
                       BigDecimal weight, String observations, Owner owner) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.sex = sex;
        this.birthDate = birthDate;
        this.weight = weight;
        this.observations = observations;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public Sex getSex() {
        return sex;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public String getObservations() {
        return observations;
    }

    public Owner getOwner() {
        return owner;
    }
}
