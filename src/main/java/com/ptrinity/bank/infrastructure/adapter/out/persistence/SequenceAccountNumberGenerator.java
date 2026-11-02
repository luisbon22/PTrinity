package com.ptrinity.bank.infrastructure.adapter.out.persistence;

import com.ptrinity.bank.domain.model.AccountType;
import com.ptrinity.bank.domain.port.out.AccountNumberGeneratorPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/**
 * Genera números de cuenta únicos usando secuencias de base de datos, lo que
 * evita colisiones incluso con varias instancias de la aplicación.
 * Formato: prefijo del tipo de cuenta (53 ahorros / 33 corriente) + 8 dígitos.
 */
@Component
public class SequenceAccountNumberGenerator implements AccountNumberGeneratorPort {

    private final EntityManager entityManager;

    public SequenceAccountNumberGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public String nextAccountNumber(AccountType type) {
        String sequence = switch (type) {
            case SAVINGS -> "savings_account_number_seq";
            case CHECKING -> "checking_account_number_seq";
        };
        Number next = (Number) entityManager
                .createNativeQuery("SELECT nextval('" + sequence + "')")
                .getSingleResult();
        return type.formatAccountNumber(next.longValue());
    }
}
