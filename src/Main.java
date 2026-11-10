import models.Library;
import models.Book;
import models.Loan;
import models.User;
import utils.Category;
import utils.LibraryFunctions;
import utils.LibraryFunctions.FineReport;
import utils.LibraryFunctions.LibraryStatistics;

import java.time.LocalDate;
import java.util.List;

void main() {
    Library library = new Library();
    User user = new User("Ana Silva", 2026001);
    Book overdueBook = new Book("Ursula K. Le Guin", Category.FANTASY, "A mão esquerda da escuridão");
    Book returnedBook = new Book("Machado de Assis", Category.FICTION, "Dom Casmurro");
    Book anotherFantasyBook = new Book("J. R. R. Tolkien", Category.FANTASY, "O Hobbit");

    library.addUser(user);
    library.addBook(overdueBook);
    library.addBook(returnedBook);
    library.addBook(anotherFantasyBook);

    Loan overdueLoan = library.lendBook(user.getRegistrationNumber(), overdueBook);
    Loan returnedLoan = library.lendBook(user.getRegistrationNumber(), returnedBook);
    library.returnBook(returnedLoan.getId());

    LocalDate referenceDate = LocalDate.now().plusDays(20);
    List<Book> functionalResults = library.findBooksByCategory(Category.FANTASY);
    List<Book> imperativeResults = LibraryFunctions.filterBooksByCategoryImperative(
            library.getBooks(),
            Category.FANTASY);
    FineReport fineReport = library.getFineReport(referenceDate);
    LibraryStatistics statistics = library.getStatistics(referenceDate);

    System.out.println("Livros de fantasia: " + functionalResults);
    System.out.println("As versoes imperativa e funcional sao equivalentes: "
            + functionalResults.equals(imperativeResults));
    System.out.println("Livro devolvido disponivel: " + returnedBook.isAvailable());
    System.out.println("Emprestimo ativo atrasado: " + overdueLoan.getId());
    System.out.println("Usuarios cadastrados: " + library.getUsers());
    System.out.println("Emprestimos ativos: " + library.findActiveLoans());
    System.out.println("Multas individuais: " + fineReport.items());
    System.out.println("Total de multas em " + referenceDate + ": R$ " + fineReport.total().toPlainString());
    System.out.println("Estatisticas: " + statistics);
}
