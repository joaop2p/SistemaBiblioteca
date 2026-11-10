package models;

import utils.Category;

import java.util.Objects;
import java.util.UUID;

public class Book {
    private final UUID id = UUID.randomUUID();
    private final String author;
    private final Category category;
    private final String title;
    private boolean available = true;

    public Book(String author, Category category, String title) {
        this.author = requireText(author, "Author");
        this.category = Objects.requireNonNull(category, "Category is required.");
        this.title = requireText(title, "Title");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAvailable() {
        return available;
    }

    void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "Book{" + '\n' +
                "\t" +"id=" + id + '\n' +
                "\t" +"author='" + author + '\n' +
                "\t" +"category=" + category + '\n' +
                "\t" +"title='" + title + '\n' +
                "\t" +"available=" + available + '\n' +
                '}';
    }
}
