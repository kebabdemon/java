import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class CsvGenerator {

    public static void main(String[] args) {
        String csvFilePath = "C:\\Users\\l_uchytil\\Documents\\test_users.csv";
        generateAndSaveTestCSV(csvFilePath,
                "pepa", "pepaheslo",
                "franta", "frantaheslo",
                "honza", "honzaheslo");
        loadAndDisplayTestCSV(csvFilePath);
    }

    private static void generateAndSaveTestCSV(String csvFilePath, String... users) {
        try (FileWriter writer = new FileWriter(csvFilePath)) {
            for (int i = 0; i < users.length; i += 2) {
                String userId = users[i];
                String password = users[i + 1];
                String salt = generateSalt();
                String hashedPassword = hashPassword(password, salt);

                String line = String.format("%s;%s;%s%n", userId, salt, hashedPassword);
                writer.write(line);
            }
        } catch (IOException e) {
            handleException("Error writing to CSV file", e);
        }
    }

    private static void loadAndDisplayTestCSV(String csvFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            br.lines().forEach(System.out::println);
        } catch (IOException e) {
            handleException("Error reading CSV file", e);
        }
    }

    private static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            handleException("Error hashing password", e);
            return null;
        }
    }

    private static void handleException(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
    }
}
