package com.ptrinity.bank.domain.model;

import com.ptrinity.bank.domain.exception.BusinessRuleException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Agregado Cliente. Concentra las reglas de negocio propias del cliente
 * (mayoría de edad, formato de correo, longitud de nombres) para que
 * ningún adaptador pueda persistir un cliente inválido.
 */
public class Customer {

    public static final int ADULT_AGE = 18;
    public static final int MIN_NAME_LENGTH = 2;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final Long id;
    private IdentificationType identificationType;
    private String identificationNumber;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer(Long id, IdentificationType identificationType, String identificationNumber,
                    String firstName, String lastName, String email, LocalDate birthDate,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.identificationType = identificationType;
        this.identificationNumber = identificationNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.birthDate = birthDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method: crea un cliente nuevo validando todas las reglas
     * y calculando automáticamente la fecha de creación.
     */
    public static Customer create(CustomerData data, LocalDateTime now) {
        validate(data, now.toLocalDate());
        return new Customer(null, data.identificationType(), data.identificationNumber().trim(),
                data.firstName().trim(), data.lastName().trim(), data.email().trim().toLowerCase(),
                data.birthDate(), now, null);
    }

    /**
     * Actualiza los datos del cliente y recalcula la fecha de modificación.
     */
    public void update(CustomerData data, LocalDateTime now) {
        validate(data, now.toLocalDate());
        this.identificationType = data.identificationType();
        this.identificationNumber = data.identificationNumber().trim();
        this.firstName = data.firstName().trim();
        this.lastName = data.lastName().trim();
        this.email = data.email().trim().toLowerCase();
        this.birthDate = data.birthDate();
        this.updatedAt = now;
    }

    private static void validate(CustomerData data, LocalDate today) {
        if (data.identificationType() == null) {
            throw new BusinessRuleException("El tipo de identificación es obligatorio");
        }
        if (isBlank(data.identificationNumber())) {
            throw new BusinessRuleException("El número de identificación es obligatorio");
        }
        if (isBlank(data.firstName()) || data.firstName().trim().length() < MIN_NAME_LENGTH) {
            throw new BusinessRuleException("El nombre debe tener al menos " + MIN_NAME_LENGTH + " caracteres");
        }
        if (isBlank(data.lastName()) || data.lastName().trim().length() < MIN_NAME_LENGTH) {
            throw new BusinessRuleException("El apellido debe tener al menos " + MIN_NAME_LENGTH + " caracteres");
        }
        if (isBlank(data.email()) || !EMAIL_PATTERN.matcher(data.email().trim()).matches()) {
            throw new BusinessRuleException("El correo electrónico no tiene un formato válido");
        }
        if (data.birthDate() == null) {
            throw new BusinessRuleException("La fecha de nacimiento es obligatoria");
        }
        if (data.birthDate().isAfter(today)) {
            throw new BusinessRuleException("La fecha de nacimiento no puede ser futura");
        }
        if (Period.between(data.birthDate(), today).getYears() < ADULT_AGE) {
            throw new BusinessRuleException("El cliente debe ser mayor de edad");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public Long getId() {
        return id;
    }

    public IdentificationType getIdentificationType() {
        return identificationType;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
