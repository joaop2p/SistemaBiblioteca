package utils;

import models.Book;
import models.Loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class LibraryFunctions {
    public static final BigDecimal DAILY_FINE = new BigDecimal("2.00");

    private LibraryFunctions() {
    }

    public static List<Book> filterBooksByCategory(List<Book> books, Category category) {
        Objects.requireNonNull(books, "Books are required.");
        Objects.requireNonNull(category, "Category is required.");
        return books.stream()
                .filter(book -> book.getCategory() == category)
                .toList();
    }

    public static List<Book> filterBooksByCategoryImperative(List<Book> books, Category category) {
        Objects.requireNonNull(books, "Books are required.");
        Objects.requireNonNull(category, "Category is required.");
        List<Book> matchingBooks = new ArrayList<>();
        for (Book book : books) {
            if (book.getCategory() == category) {
                matchingBooks.add(book);
            }
        }
        return List.copyOf(matchingBooks);
    }

    public static BigDecimal calculateFine(long overdueDays, BigDecimal finePerDay) {
        Objects.requireNonNull(finePerDay, "Daily fine is required.");
        if (overdueDays < 0) {
            throw new IllegalArgumentException("Overdue days cannot be negative.");
        }
        if (finePerDay.signum() < 0) {
            throw new IllegalArgumentException("Daily fine cannot be negative.");
        }
        return finePerDay.multiply(BigDecimal.valueOf(overdueDays));
    }

    public static long overdueDays(Loan loan, LocalDate referenceDate) {
        Objects.requireNonNull(loan, "Loan is required.");
        Objects.requireNonNull(referenceDate, "Reference date is required.");
        if (!isOverdue(loan, referenceDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(loan.getDueDate(), referenceDate);
    }

    public static boolean isOverdue(Loan loan, LocalDate referenceDate) {
        Objects.requireNonNull(loan, "Loan is required.");
        Objects.requireNonNull(referenceDate, "Reference date is required.");
        return loan.isActive() && loan.getDueDate().isBefore(referenceDate);
    }

    public static List<Loan> findOverdueLoans(List<Loan> loans, LocalDate referenceDate) {
        Objects.requireNonNull(loans, "Loans are required.");
        Objects.requireNonNull(referenceDate, "Reference date is required.");
        return loans.stream()
                .filter(loan -> isOverdue(loan, referenceDate))
                .toList();
    }

    public static FineReport createFineReport(List<Loan> loans, LocalDate referenceDate) {
        List<FineItem> items = findOverdueLoans(loans, referenceDate).stream()
                .map(loan -> {
                    long days = overdueDays(loan, referenceDate);
                    return new FineItem(
                            loan.getId(),
                            loan.getUser().getName(),
                            loan.getBook().getTitle(),
                            days,
                            calculateFine(days, DAILY_FINE));
                })
                .toList();
        BigDecimal total = items.stream()
                .map(FineItem::fine)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FineReport(items, total);
    }

    public static LibraryStatistics calculateStatistics(
            List<Book> books,
            List<Loan> loans,
            LocalDate referenceDate
    ) {
        Objects.requireNonNull(books, "Books are required.");
        Objects.requireNonNull(loans, "Loans are required.");
        Objects.requireNonNull(referenceDate, "Reference date is required.");

        Map<Category, Integer> booksByCategory = books.stream()
                .collect(Collectors.groupingBy(
                        Book::getCategory,
                        () -> new EnumMap<>(Category.class),
                        Collectors.summingInt(book -> 1)));
        Map<UUID, Integer> loansByUser = loans.stream()
                .collect(Collectors.groupingBy(
                        loan -> loan.getUser().getId(),
                        Collectors.summingInt(loan -> 1)));
        int activeLoans = countUsingReduce(loans.stream()
                .filter(Loan::isActive)
                .map(loan -> 1));
        int overdueActiveLoans = countUsingReduce(findOverdueLoans(loans, referenceDate).stream()
                .map(loan -> 1));
        BigDecimal totalFines = createFineReport(loans, referenceDate).total();

        return new LibraryStatistics(
                booksByCategory,
                loansByUser,
                activeLoans,
                overdueActiveLoans,
                totalFines);
    }

    private static int countUsingReduce(java.util.stream.Stream<Integer> values) {
        return values.reduce(0, Integer::sum);
    }

    public record FineItem(
            UUID loanId,
            String userName,
            String bookTitle,
            long overdueDays,
            BigDecimal fine
    ) {
        public FineItem {
            Objects.requireNonNull(loanId, "Loan ID is required.");
            Objects.requireNonNull(userName, "User name is required.");
            Objects.requireNonNull(bookTitle, "Book title is required.");
            Objects.requireNonNull(fine, "Fine is required.");
        }
    }

    public record FineReport(List<FineItem> items, BigDecimal total) {
        public FineReport {
            items = List.copyOf(items);
            Objects.requireNonNull(total, "Report total is required.");
        }
    }

    public record LibraryStatistics(
            Map<Category, Integer> booksByCategory,
            Map<UUID, Integer> loansByUser,
            int activeLoans,
            int overdueActiveLoans,
            BigDecimal totalFines
    ) {
        public LibraryStatistics {
            booksByCategory = Map.copyOf(booksByCategory);
            loansByUser = Map.copyOf(loansByUser);
            Objects.requireNonNull(totalFines, "Total fines are required.");
        }
    }
}
