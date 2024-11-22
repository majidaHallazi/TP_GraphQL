package com.example.graphql.controllers;

import com.example.graphql.entities.Compte;
import com.example.graphql.entities.Transaction;
import com.example.graphql.repositories.CompteRepository;
import com.example.graphql.repositories.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
public class CompteControllerGraphQL {

    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;

    @QueryMapping
    public List<Compte> allComptes() {
        return compteRepository.findAll();
    }

    @QueryMapping
    public Compte compteById(@Argument Long id) {
        return compteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(String.format("Compte %s not found", id)));
    }

    @QueryMapping
    public Map<String, Object> totalSolde() {
        long count = compteRepository.count();
        double sum = compteRepository.sumSoldes();
        double average = count > 0 ? sum / count : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("sum", sum);
        result.put("average", average);

        return result;
    }

    @MutationMapping
    public Compte saveCompte(@Argument Compte compte) {
        return compteRepository.save(compte);
    }

    @QueryMapping
    public List<Transaction> transactionsByCompteId(@Argument Long compteId) {
        return transactionRepository.findByCompteId(compteId);
    }

    @MutationMapping
    public Transaction addTransaction(
            @Argument Long compteId,
            @Argument double montant,
            @Argument String type
    ) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte not found"));

        // Update solde based on transaction type
        if ("credit".equalsIgnoreCase(type)) {
            compte.setSolde(compte.getSolde() + montant);
        } else if ("debit".equalsIgnoreCase(type)) {
            if (compte.getSolde() < montant) {
                throw new RuntimeException("Insufficient balance");
            }
            compte.setSolde(compte.getSolde() - montant);
        } else {
            throw new RuntimeException("Invalid transaction type");
        }

        compteRepository.save(compte);

        Transaction transaction = new Transaction();
        transaction.setCompte(compte);
        transaction.setMontant(montant);
        transaction.setType(type);
        transaction.setDateTransaction(LocalDate.now());
        return transactionRepository.save(transaction);
    }
}
