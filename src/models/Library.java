package models;

import utils.Category;
import utils.LibraryFunctions;
import utils.LibraryFunctions.FineReport;
import utils.LibraryFunctions.LibraryStatistics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Library {
    private static final int LOAN_PERIOD_DAYS = 15;
    private final Loans loans = new Loans();
    private final List<Book> books = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    public synchronized void addBook(Book book) {
        Objects.requireNonNull(book, "Book is required.");
        if (books.stream().anyMatch(existingBook -> existingBook.getId().equals(book.getId()))) {
            throw new IllegalArgumentException("Book with the same ID already exists in the library.");
        }
        books.add(book);
    }

    public synchronized void addUser(User user) {
        Objects.requireNonNull(user, "User is required.");
        if (users.stream().anyMatch(existingUser ->
                existingUser.getRegistrationNumber() == user.getRegistrationNumber())) {
            throw new IllegalArgumentException("A user with this registration number already exists.");
        }
        users.add(user);
    }

    public synchronized Optional<User> findUserByRegistration(long registrationNumber) {
        return users.stream()
                .filter(user -> user.getRegistrationNumber() == registrationNumber)
                .findFirst();
    }

    public synchronized Optional<Book> findBookById(UUID id) {
        return books.stream()
                .filter(book -> book.getId().equals(id))
                .findFirst();
    }

    public synchronized Loan lendBook(long registrationNumber, Book book) {
        return lendBook(registrationNumber, book, LocalDate.now());
    }

    public synchronized Loan lendBook(long registrationNumber, Book book, LocalDate loanDate) {
        Objects.requireNonNull(book, "Book is required.");
        Objects.requireNonNull(loanDate, "Loan date is required.");
        User user = findUserByRegistration(registrationNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (findBookById(book.getId()).isEmpty()) {
            throw new IllegalArgumentException("Book is not registered in the library.");
        }
        if (!book.isAvailable()) {
            throw new IllegalArgumentException("Book is not available for loan.");
        }

        LocalDate dueDate = loanDate.plusDays(LOAN_PERIOD_DAYS);
        Loan loan = loans.addLoan(book, user, loanDate, dueDate);
        book.setAvailable(false);
        return loan;
    }

    public synchronized Loan returnBook(UUID loanId) {
        return returnBook(loanId, LocalDate.now());
    }

    public synchronized Loan returnBook(UUID loanId, LocalDate actualReturnDate) {
        Objects.requireNonNull(loanId, "Loan ID is required.");
        Objects.requireNonNull(actualReturnDate, "Return date is required.");
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found."));
        loan.registerReturn(actualReturnDate);
        loan.getBook().setAvailable(true);
        return loan;
    }

    public synchronized List<Loan> getLoans() {
        return loans.getLoans();
    }

    public synchronized List<Loan> findLoansByUser(User user) {
        Objects.requireNonNull(user, "User is required.");
        return loans.findByUser(user);
    }

    public synchronized List<Loan> findActiveLoans() {
        return loans.findOpen();
    }

    public synchronized Optional<Loan> findLoanById(UUID loanId) {
        return loans.findById(loanId);
    }

    public synchronized List<Book> getBooks() {
        return List.copyOf(books);
    }

    public synchronized List<User> getUsers() {
        return List.copyOf(users);
    }

    public synchronized List<Book> findBooksByCategory(Category category) {
        return LibraryFunctions.filterBooksByCategory(getBooks(), category);
    }

    public synchronized List<Loan> findOverdueLoans(LocalDate referenceDate) {
        return LibraryFunctions.findOverdueLoans(getLoans(), referenceDate);
    }

    public synchronized FineReport getFineReport(LocalDate referenceDate) {
        return LibraryFunctions.createFineReport(getLoans(), referenceDate);
    }

    public synchronized LibraryStatistics getStatistics(LocalDate referenceDate) {
        return LibraryFunctions.calculateStatistics(getBooks(), getLoans(), referenceDate);
    }
}
