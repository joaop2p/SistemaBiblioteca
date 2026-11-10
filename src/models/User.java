package models;


import java.util.UUID;

public class User {
    private final UUID id = UUID.randomUUID();
    private final String name;
    private final long registrationNumber;

    public User(String name, long registrationNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (registrationNumber <= 0) {
            throw new IllegalArgumentException("Registration number must be positive.");
        }
        this.name = name;
        this.registrationNumber = registrationNumber;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getRegistrationNumber() {
        return registrationNumber;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", registrationNumber=" + registrationNumber +
                '}';
    }
}
