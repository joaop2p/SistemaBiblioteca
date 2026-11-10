package tests;

import models.Book;
import models.Library;
import models.Loan;
import models.User;
import utils.Category;
import utils.LibraryFunctions;
import utils.LibraryFunctions.FineReport;
import utils.LibraryFunctions.LibraryStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LibraryBehaviorTest {
    public static void main(String[] args) throws InterruptedException {
        testLendingReturnAndFineReport();
        testFunctionalFilteringAndStatistics();
        testConcurrentLending();
        testRegistrationAndLendingValidation();
        testReturnValidationAndOverdueExclusion();
        testReadOnlySnapshots();
        System.out.println("All 6 library behavior test groups passed.");
    }

    private static void testLendingReturnAndFineReport() {
        Library library = new Library();
        User user = new User("Ana", 1001);
        Book overdueBook = new Book("Author A", Category.FICTION, "Book A");
        Book returnedBook = new Book("Author B", Category.HISTORY, "Book B");
        LocalDate loanDate = LocalDate.of(2026, 1, 1);
        LocalDate referenceDate = LocalDate.of(2026, 1, 20);

        library.addUser(user);
        library.addBook(overdueBook);
        library.addBook(returnedBook);
        Loan overdueLoan = library.lendBook(user.getRegistrationNumber(), overdueBook, loanDate);
        Loan returnedLoan = library.lendBook(user.getRegistrationNumber(), returnedBook, loanDate);
        require(overdueLoan.getDueDate().equals(LocalDate.of(2026, 1, 16)), "Loan term must be 15 days.");
        require(!overdueBook.isAvailable(), "Lending must make a book unavailable.");
        expectFailure(() -> library.lendBook(user.getRegistrationNumber(), overdueBook, loanDate));

        library.returnBook(returnedLoan.getId(), LocalDate.of(2026, 1, 10));
        require(returnedBook.isAvailable(), "Returning must make a book available.");
        require(returnedLoan.getActualReturnDate().equals(LocalDate.of(2026, 1, 10)),
                "Return date must be recorded.");
        expectFailure(() -> library.returnBook(returnedLoan.getId(), LocalDate.of(2026, 1, 11)));

        FineReport report = library.getFineReport(referenceDate);
        require(report.items().size() == 1, "Only active overdue loans belong in the fine report.");
        require(report.items().getFirst().loanId().equals(overdueLoan.getId()),
                "Fine report must identify the overdue loan.");
        require(report.items().getFirst().overdueDays() == 4, "Overdue days must use calendar days.");
        require(report.total().compareTo(new BigDecimal("8.00")) == 0,
                "Four overdue days must produce an R$ 8.00 fine.");
        require(!LibraryFunctions.isOverdue(overdueLoan, overdueLoan.getDueDate()),
                "A loan is not overdue on its due date.");
        require(LibraryFunctions.overdueDays(overdueLoan, overdueLoan.getDueDate().plusDays(1)) == 1,
                "The first day after the due date is one day overdue.");
        require(report != library.getFineReport(referenceDate),
                "Each report call must produce a separate report value.");
        require(report.total().compareTo(library.getFineReport(referenceDate).total()) == 0,
                "Identical inputs must produce the same total fine.");
        expectFailure(() -> report.items().clear());
        expectFailure(() -> LibraryFunctions.calculateFine(-1, LibraryFunctions.DAILY_FINE));
    }

    private static void testFunctionalFilteringAndStatistics() {
        Library library = new Library();
        User user = new User("Bruno", 1002);
        Book fantasyBook = new Book("Author A", Category.FANTASY, "Fantasy");
        Book fictionBook = new Book("Author B", Category.FICTION, "Fiction");
        LocalDate loanDate = LocalDate.of(2026, 2, 1);

        library.addUser(user);
        library.addBook(fantasyBook);
        library.addBook(fictionBook);
        Loan loan = library.lendBook(user.getRegistrationNumber(), fantasyBook, loanDate);
        List<Book> originalBooks = library.getBooks();
        List<Book> functional = LibraryFunctions.filterBooksByCategory(originalBooks, Category.FANTASY);
        List<Book> imperative = LibraryFunctions.filterBooksByCategoryImperative(originalBooks, Category.FANTASY);

        require(functional.equals(imperative), "Imperative and functional filtering must agree.");
        require(originalBooks.size() == 2, "Filtering must not modify the source collection.");
        require(functional.size() == 1 && functional.getFirst() == fantasyBook,
                "Filtering must return only matching books.");
        expectFailure(() -> functional.clear());

        LibraryStatistics statistics = library.getStatistics(LocalDate.of(2026, 2, 20));
        require(statistics.booksByCategory().get(Category.FANTASY) == 1,
                "Statistics must count books by category.");
        require(statistics.loansByUser().get(user.getId()) == 1,
                "Statistics must count loans by user.");
        require(statistics.activeLoans() == 1, "Statistics must count active loans.");
        require(statistics.overdueActiveLoans() == 1, "Statistics must count overdue active loans.");
        require(statistics.totalFines().compareTo(new BigDecimal("8.00")) == 0,
                "Statistics must use the fine report total.");
        require(library.findLoanById(loan.getId()).isPresent(), "Loans must be queryable by ID.");
        require(library.findLoansByUser(user).size() == 1, "Loans must be queryable by user.");
        require(library.findActiveLoans().size() == 1, "Active loans must be queryable.");
        require(library.findBookById(fantasyBook.getId()).isPresent(), "Books must be queryable by ID.");
    }

    private static void testConcurrentLending() throws InterruptedException {
        Library library = new Library();
        User firstUser = new User("First", 1003);
        User secondUser = new User("Second", 1004);
        Book book = new Book("Author", Category.MYSTERY, "Shared book");
        library.addUser(firstUser);
        library.addUser(secondUser);
        library.addBook(book);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successfulLoans = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread first = loanAttempt(library, firstUser, book, ready, start, successfulLoans, failure);
        Thread second = loanAttempt(library, secondUser, book, ready, start, successfulLoans, failure);
        first.start();
        second.start();
        ready.await();
        start.countDown();
        first.join();
        second.join();

        require(failure.get() == null, "Concurrent lending must fail only for the losing request.");
        require(successfulLoans.get() == 1, "Only one concurrent request can loan the same book.");
        require(library.getLoans().size() == 1, "Concurrent lending must create only one loan.");
    }

    private static void testRegistrationAndLendingValidation() {
        Library library = new Library();
        User user = new User("Carla", 1005);
        Book registeredBook = new Book("Author", Category.BIOGRAPHY, "Biography");

        library.addUser(user);
        library.addBook(registeredBook);
        expectFailure(() -> library.addUser(new User("Duplicate", user.getRegistrationNumber())));
        expectFailure(() -> library.addUser(null));
        expectFailure(() -> library.addBook(registeredBook));
        expectFailure(() -> library.addBook(null));
        expectFailure(() -> library.lendBook(9999, registeredBook, LocalDate.of(2026, 4, 1)));
        expectFailure(() -> library.lendBook(
                user.getRegistrationNumber(),
                new Book("Author", Category.BIOGRAPHY, "Unregistered"),
                LocalDate.of(2026, 4, 1)));
        expectFailure(() -> library.lendBook(user.getRegistrationNumber(), null, LocalDate.of(2026, 4, 1)));
        expectFailure(() -> library.lendBook(user.getRegistrationNumber(), registeredBook, null));

        require(registeredBook.isAvailable(), "Rejected lending attempts must not change book availability.");
        require(library.getLoans().isEmpty(), "Rejected lending attempts must not create loans.");
    }

    private static void testReturnValidationAndOverdueExclusion() {
        Library library = new Library();
        User user = new User("Diana", 1006);
        Book book = new Book("Author", Category.ROMANCE, "Returned overdue book");
        LocalDate loanDate = LocalDate.of(2026, 5, 1);
        library.addUser(user);
        library.addBook(book);
        Loan loan = library.lendBook(user.getRegistrationNumber(), book, loanDate);

        expectFailure(() -> library.returnBook(loan.getId(), loanDate.minusDays(1)));
        require(loan.isActive(), "A return before the loan date must not close the loan.");
        require(!book.isAvailable(), "An invalid return must not make the book available.");
        expectFailure(() -> library.returnBook(java.util.UUID.randomUUID(), loanDate.plusDays(1)));

        library.returnBook(loan.getId(), loan.getDueDate().plusDays(5));
        LocalDate referenceDate = loan.getDueDate().plusDays(10);
        require(LibraryFunctions.findOverdueLoans(library.getLoans(), referenceDate).isEmpty(),
                "A returned loan must not be reported as overdue.");
        FineReport report = library.getFineReport(referenceDate);
        require(report.items().isEmpty(), "Returned loans must not appear in the fine report.");
        require(report.total().compareTo(BigDecimal.ZERO) == 0, "An empty fine report must total zero.");
        require(library.getStatistics(referenceDate).activeLoans() == 0,
                "Returned loans must not count as active.");
        require(library.getStatistics(referenceDate).overdueActiveLoans() == 0,
                "Returned loans must not count as overdue.");
    }

    private static void testReadOnlySnapshots() {
        Library library = new Library();
        User user = new User("Eduardo", 1007);
        Book book = new Book("Author", Category.POETRY, "Poems");
        library.addUser(user);
        library.addBook(book);

        List<Book> books = library.getBooks();
        List<User> users = library.getUsers();
        List<Loan> loans = library.getLoans();
        expectFailure(() -> books.clear());
        expectFailure(() -> users.clear());
        expectFailure(() -> loans.clear());
        require(library.getBooks().size() == 1, "Changing a returned book list must not affect the library.");
        require(library.getUsers().size() == 1, "Changing a returned user list must not affect the library.");
        require(library.getLoans().isEmpty(), "Changing a returned loan list must not affect the library.");
    }

    private static Thread loanAttempt(
            Library library,
            User user,
            Book book,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successfulLoans,
            AtomicReference<Throwable> failure
    ) {
        return new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                library.lendBook(user.getRegistrationNumber(), book, LocalDate.of(2026, 3, 1));
                successfulLoans.incrementAndGet();
            } catch (IllegalArgumentException exception) {
                if (!"Book is not available for loan.".equals(exception.getMessage())) {
                    failure.compareAndSet(null, exception);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                failure.compareAndSet(null, exception);
            }
        });
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException
                 | UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected operation to fail.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
