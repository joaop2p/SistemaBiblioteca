package models;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Loan {
    private final UUID id = UUID.randomUUID();
    private final User user;
    private final Book book;
    private final LocalDate loanDate;
    private final LocalDate dueDate;
    private LocalDate actualReturnDate;

    Loan(User user, Book book, LocalDate loanDate, LocalDate dueDate) {
        this.user = Objects.requireNonNull(user, "User is required.");
        this.book = Objects.requireNonNull(book, "Book is required.");
        this.loanDate = Objects.requireNonNull(loanDate, "Loan date is required.");
        this.dueDate = Objects.requireNonNull(dueDate, "Due date is required.");
        if (dueDate.isBefore(loanDate)) {
            throw new IllegalArgumentException("Due date cannot be before loan date.");
        }
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Book getBook() {
        return book;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getActualReturnDate() {
        return actualReturnDate;
    }

    public boolean isActive() {
        return actualReturnDate == null;
    }

    void registerReturn(LocalDate actualReturnDate) {
        Objects.requireNonNull(actualReturnDate, "Return date is required.");
        if (!isActive()) {
            throw new IllegalStateException("Loan has already been returned.");
        }
        if (actualReturnDate.isBefore(loanDate)) {
            throw new IllegalArgumentException("Return date cannot be before loan date.");
        }
        this.actualReturnDate = actualReturnDate;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", user=" + user.getName() +
                ", book=" + book.getTitle() +
                ", loanDate=" + loanDate +
                ", dueDate=" + dueDate +
                ", actualReturnDate=" + actualReturnDate +
                '}';
    }
}
