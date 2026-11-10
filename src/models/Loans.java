package models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Loans {
    private final List<Loan> loans = new ArrayList<>();

    Loan addLoan(Book book, User user, LocalDate loanDate, LocalDate dueDate) {
        Loan loan = new Loan(user, book, loanDate, dueDate);
        loans.add(loan);
        return loan;
    }

    public List<Loan> getLoans() {
        return List.copyOf(loans);
    }

    public Optional<Loan> findById(UUID id) {
        return loans.stream()
                .filter(loan -> loan.getId().equals(id))
                .findFirst();
    }

    public List<Loan> findByUser(User user) {
        return loans.stream()
                .filter(loan -> loan.getUser().getId().equals(user.getId()))
                .toList();
    }

    public List<Loan> findOpen() {
        return loans.stream()
                .filter(Loan::isActive)
                .toList();
    }
}