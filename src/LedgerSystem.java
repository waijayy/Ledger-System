package led;

import java.io.FileWriter; //Help write data to a file, will learn in week 7  
import java.io.IOException; //Used for handling input/output errors(e.g. file-writing issues)
import java.sql.Connection; //All the SQL stuff
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate; //Allow to do input
import java.util.ArrayList; //Used to validate the emails and passwords, by matching them to patterns.
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;


public class LedgerSystem_Copy {
   private static String userName = "";
   private static int userId;
   private static final String URL = "jdbc:mysql://localhost:3306/Ledger_System";
   private static final String USER = "root";
   private static final String PASSWORD = "";


   private static boolean validateEmail(String email) {
      String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"; 
      return Pattern.compile(regex).matcher(email).matches();


   }

   private static boolean validatePassword(String password) {
      return password.length() >= 6 && password.matches(".*[!@#$%^&*(),.?\":{}|<>-].*");
   }

   private static boolean validateName(String name) {
      return name.matches("^[a-zA-Z0-9 ]+$");

   }      




   public static void main(String[] args) {
      Scanner scanner = new Scanner(System.in); 
      int option; 

      do {
         System.out.println("1. Login");
         System.out.println("2. Register");
         System.out.print("Choose an option: ");
         option = scanner.nextInt();
         scanner.nextLine();

         switch (option) {
            case 1 -> loginUser(); 
            case 2 -> registerUser(); 
            default -> System.out.println("Invalid option.");
         }
      } while (true);
   }


   
   private static void loginUser() {
    Scanner scanner = new Scanner(System.in);
    System.out.println("== Please enter your email and password ==");
    System.out.print("Email: ");
    String email = scanner.nextLine();
    System.out.print("Password: ");
    String password = scanner.nextLine();

    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
        // Retrieve the hashed password from the database
        String query = "SELECT user_id, name, password FROM Users WHERE email = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, email);

        ResultSet resultSet = stmt.executeQuery();

        if (resultSet.next()) {
            String hashedPassword = resultSet.getString("password");
            userId = resultSet.getInt("user_id");
            userName = resultSet.getString("name");

            // Verify the password using bcrypt
            if (BCrypt.checkpw(password, hashedPassword)) {
                System.out.println("Login Successful!!!");
                System.out.println("== Welcome, " + userName + " ==");
                showTransactionMenu();
            } else {
                System.out.println("Invalid email or password.");
            }
        } else {
            System.out.println("Invalid email or password.");
        }
      } catch (SQLException e) {
        System.out.println("Error logging in.");
        e.printStackTrace();
      }
   }


   private static void registerUser() {
      Scanner scanner = new Scanner(System.in);
      System.out.println("== Please fill in the form ==");
      System.out.print("Name: ");
      String name = scanner.nextLine();
      System.out.print("Email: ");
      String email = scanner.nextLine();
      System.out.print("Password: ");
      String password = scanner.nextLine();
      System.out.print("Confirm Password: ");
      String confirmPassword = scanner.nextLine();
  
      if (!validateName(name)) {
          System.out.println("Name must be alphanumeric (alphabets or/and numbers) and cannot contain special characters.");
      } else if (!validateEmail(email)) {
          System.out.println("Invalid email format.");
      } else if (!validatePassword(password)) {
          System.out.println("Password must be at least 6 characters long and contain at least one special character.");
      } else if (!password.equals(confirmPassword)) {
          System.out.println("Passwords do not match.");
      } else {
          try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
              // Hash the password using bcrypt
              String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
  
              // Insert user details into the database
              String userQuery = "INSERT INTO Users (name, email, password) VALUES (?, ?, ?)";
              PreparedStatement userStmt = conn.prepareStatement(userQuery, Statement.RETURN_GENERATED_KEYS);
              userStmt.setString(1, name);
              userStmt.setString(2, email);
              userStmt.setString(3, hashedPassword);
              userStmt.executeUpdate();
  
              ResultSet generatedKeys = userStmt.getGeneratedKeys();
              if (generatedKeys.next()) {
                  int userId = generatedKeys.getInt(1);
  
                  // Initialize user's account balance
                  String balanceQuery = "INSERT INTO Account_Balance (user_id, balance) VALUES (?, 0.00)";
                  PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery);
                  balanceStmt.setInt(1, userId);
                  balanceStmt.executeUpdate();
                  balanceStmt.close();
              }
  
              userStmt.close();
              System.out.println("Register Successful!!!");
  
          } catch (SQLException e) {
              System.out.println("Error registering user. Email might already be taken.");
              e.printStackTrace();
          }
      }
  }

   

   private static void displayBalance() {
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
         String query = "SELECT ab.balance, IFNULL(s.savings_balance, 0) AS savings " + "FROM Account_Balance ab " + "LEFT JOIN Savings s ON ab.user_id = s.user_id " + "WHERE ab.user_id = ?";
         PreparedStatement stmt = conn.prepareStatement(query);
         stmt.setInt(1, userId); 


         ResultSet resultSet = stmt.executeQuery();
         if (resultSet.next()) {
            double balance = resultSet.getDouble("balance");
            double savings = resultSet.getDouble("savings");
            System.out.printf("Balance: %.2f%n", balance);
            System.out.printf("Savings: %.2f%n", savings);
         }
      } catch (SQLException e) {
         System.out.println("Error retrieving balance.");
         e.printStackTrace();
      }
   }


   private static void showTransactionMenu() {
      Scanner scanner = new Scanner(System.in);
      int option;
      do {
         displayBalance();
         System.out.println("1. Debit");
         System.out.println("2. Credit");
         System.out.println("3. History");
         System.out.println("4. Savings");
         System.out.println("5. Credit Loan");
         System.out.println("6. Deposit Interest Predictor");
         System.out.println("7. Logout");
         System.out.print("Choose an option: ");
         option = scanner.nextInt();
         scanner.nextLine();

         switch (option) {
            case 1 -> {
               System.out.print("Enter amount to debit: ");
               double amount = scanner.nextDouble();
               scanner.nextLine();
               System.out.print("Enter description: ");
               String description = scanner.nextLine();
               debitTransaction(amount, description); 
            }
            case 2 -> {
               System.out.print("Enter amount to credit: ");
               double amount = scanner.nextDouble();
               scanner.nextLine();
               System.out.print("Enter description: ");
               String description = scanner.nextLine();
               creditTransaction(amount, description);
            }
            case 3 -> viewTransactionHistory(); 
            case 4 -> manageSavings();   
            case 6 -> depositInterestPredictor();
            case 7 -> {
               System.out.println("Thank you for using our service."); 
               System.exit(0);
            }
            default -> System.out.println("Invalid option.Please select from 1 to 7");
         }
      } while (option != 7);
   }   

   private static void debitTransaction(double amount, String description) {
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
         String savingsQuery = "SELECT percentage FROM Savings WHERE user_id = ? AND status = 'active'";
         PreparedStatement savingsStmt = conn.prepareStatement(savingsQuery);
         savingsStmt.setInt(1, userId);
         ResultSet savingsRs = savingsStmt.executeQuery();

         double savingsAmount = 0.0;
         if (savingsRs.next()) {
            int percentage = savingsRs.getInt("percentage");
            savingsAmount = (amount * percentage) / 100.0;
         }

         // Update balance
         double finalAmount = amount - savingsAmount;
         String balanceQuery = "UPDATE Account_Balance SET balance = balance + ? WHERE user_id = ?";
         //Update in username, 
         PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery);
         balanceStmt.setDouble(1, finalAmount); 
         balanceStmt.setInt(2, userId); 
         balanceStmt.executeUpdate();

         // Update savings
         if (savingsAmount > 0) {
            String savingsUpdateQuery = "UPDATE Savings SET savings_balance = savings_balance + ? WHERE user_id = ?";
            PreparedStatement savingsUpdateStmt = conn.prepareStatement(savingsUpdateQuery);
            savingsUpdateStmt.setDouble(1, savingsAmount);
            savingsUpdateStmt.setInt(2, userId);
            savingsUpdateStmt.executeUpdate();
        }

         String transactionQuery = "INSERT INTO Transactions (user_id, transaction_type, amount, description) VALUES (?, 'debit', ?, ?)";
         PreparedStatement transactionStmt = conn.prepareStatement(transactionQuery);
         transactionStmt.setInt(1, userId);
         transactionStmt.setDouble(2, finalAmount);
         transactionStmt.setString(3, description);
         transactionStmt.executeUpdate();

         System.out.println("Debit Successfully Recorded!");
      } catch (SQLException e) {
         System.out.println("Error processing debit transaction.");
         e.printStackTrace();
      }
   }

   private static void creditTransaction(double amount, String description) {
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
         String balanceQuery = "UPDATE Account_Balance SET balance = balance - ? WHERE user_id = ?";
         PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery);
         balanceStmt.setDouble(1, amount);
         balanceStmt.setInt(2, userId);
         balanceStmt.executeUpdate();

         String transactionQuery = "INSERT INTO Transactions (user_id, transaction_type, amount, description) VALUES (?, 'credit', ?, ?)";
         PreparedStatement transactionStmt = conn.prepareStatement(transactionQuery);
         transactionStmt.setInt(1, userId);
         transactionStmt.setDouble(2, amount);
         transactionStmt.setString(3, description);
         transactionStmt.executeUpdate();

         System.out.println("Credit Successfully Recorded!");
      } catch (SQLException e) {
         System.out.println("Error processing credit transaction.");
         e.printStackTrace();
      }
   }


   private static void viewTransactionHistory() {
      String query = "SELECT date, description, transaction_type, amount FROM Transactions WHERE user_id = ? ORDER BY date DESC";
      double runningBalance = 0.0; // Initialize balance
  
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
           PreparedStatement stmt = conn.prepareStatement(query)) {
          stmt.setInt(1, userId);
          ResultSet rs = stmt.executeQuery();
  
          System.out.println("== History ==");
          System.out.println("Date       | Description               | Debit     | Credit    | Balance");
  
          // Iterate through transactions and compute balance
          while (rs.next()) {
              String date = rs.getDate("date").toString();
              String description = rs.getString("description");
              String type = rs.getString("transaction_type");
              double amount = rs.getDouble("amount");
  
              if (type.equalsIgnoreCase("debit")) {
                  runningBalance += amount; // Add for debit
                  System.out.printf("%-10s | %-25s | %-9.2f | %-9s | %-9.2f\n", date, description, amount, "", runningBalance);
              } else if (type.equalsIgnoreCase("credit")) {
                  runningBalance -= amount; // Subtract for credit
                  System.out.printf("%-10s | %-25s | %-9s | %-9.2f | %-9.2f\n", date, description, "", amount, runningBalance);
              }
          }
  
      } catch (SQLException e) {
          System.out.println("Error retrieving transaction history.");
          e.printStackTrace();
      }
  }
  

// Helper method to export to CSV
private static void exportToCSV(List<Transaction> transactions, double initialBalance) {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Export to CSV? (y/n): ");
    if (!scanner.nextLine().equalsIgnoreCase("y")) return;

    try (FileWriter writer = new FileWriter("TransactionHistory.csv")) {
        writer.append("Date,Description,Debit,Credit,Balance\n");
        double runningBalance = initialBalance;

        for (Transaction tx : transactions) {
            if (tx.type.equals("debit")) {
                runningBalance += tx.amount;
                writer.append(String.format("%s,%s,%.2f,,%.2f\n", tx.date, tx.description, tx.amount, runningBalance));
            } else {
                runningBalance -= tx.amount;
                writer.append(String.format("%s,%s,,%.2f,%.2f\n", tx.date, tx.description, tx.amount, runningBalance));
            }
        }
        System.out.println("File exported.");
    } catch (IOException e) {
        System.out.println("Error exporting transaction history to CSV.");
        e.printStackTrace();
    }
}

// Helper class to represent transactions
private static class Transaction {
    String date;
    String description;
    String type;
    double amount;

    Transaction(String date, String description, String type, double amount) {
        this.date = date;
        this.description = description;
        this.type = type;
        this.amount = amount;
    }
}


   private static void manageSavings() {
      Scanner scanner = new Scanner(System.in);
      System.out.println("== Savings ==");
      System.out.print("Are you sure you want to activate it? (Y/N): ");
      String response = scanner.nextLine();
  
      if (response.equalsIgnoreCase("N")) {
         try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
             // Set status to 'inactive' without deleting the record
             String deactivateQuery = "UPDATE Savings SET status = 'inactive' WHERE user_id = ?";
             PreparedStatement stmt = conn.prepareStatement(deactivateQuery);
             stmt.setInt(1, userId);
             int rowsUpdated = stmt.executeUpdate();
             if (rowsUpdated > 0) {
                 System.out.println("Savings deactivated. Your savings balance is preserved.");
             } else {
                 System.out.println("No active savings found for deactivation.");
             }
         } catch (SQLException e) {
             System.out.println("Error deactivating savings.");
             e.printStackTrace();
         }
         return;
     }

     if(response.equalsIgnoreCase("Y")){
         System.out.print("Please enter the percentage you wish to deduct from the next debit (1-100): ");
         int percentage = scanner.nextInt();

  
         if (percentage < 1 || percentage > 100) {
            System.out.println("Invalid percentage. Please try again.");
            return;
         }
  
         try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String upsertQuery = "INSERT INTO Savings (user_id, status, percentage, savings_balance) " +
                                 "VALUES (?, 'active', ?, 0) " +
                                 "ON DUPLICATE KEY UPDATE status = 'active', percentage = ?";
            PreparedStatement stmt = conn.prepareStatement(upsertQuery);
            stmt.setInt(1, userId);
            stmt.setInt(2, percentage);
            stmt.setInt(3, percentage);
            stmt.executeUpdate();
            System.out.println("Savings settings updated successfully!");
         }catch (SQLException e) {
            System.out.println("Error managing savings.");
            e.printStackTrace();
     }
   }
}

  private static void transferSavingsToBalance() {
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
        // Get the current date and check if it's the 1st of the month
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() != 1) {
            return; // Do nothing if it's not the 1st of the month
        }

        // Get the savings balance
        String getSavingsQuery = "SELECT savings_balance FROM Savings WHERE user_id = ? AND status = 'active'";
        PreparedStatement getSavingsStmt = conn.prepareStatement(getSavingsQuery);
        getSavingsStmt.setInt(1, userId);
        ResultSet rs = getSavingsStmt.executeQuery();

        double savingsAmount = 0;
        if (rs.next()) {
            savingsAmount = rs.getDouble("savings_balance");
        }

        if (savingsAmount > 0) {
            // Add savings to the balance
            String updateBalanceQuery = "UPDATE Account_Balance SET balance = balance + ? WHERE user_id = ?";
            PreparedStatement updateBalanceStmt = conn.prepareStatement(updateBalanceQuery);
            updateBalanceStmt.setDouble(1, savingsAmount);
            updateBalanceStmt.setInt(2, userId);
            updateBalanceStmt.executeUpdate();

            // Reset the savings balance to zero
            String resetSavingsQuery = "UPDATE Savings SET savings_balance = 0 WHERE user_id = ?";
            PreparedStatement resetSavingsStmt = conn.prepareStatement(resetSavingsQuery);
            resetSavingsStmt.setInt(1, userId);
            resetSavingsStmt.executeUpdate();

            System.out.printf("Savings of %.2f transferred to balance.%n", savingsAmount);
        }
    } catch (SQLException e) {
        System.out.println("Error transferring savings to balance.");
        e.printStackTrace();
    }
}



   private static void depositInterestPredictor() {
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
         Scanner scanner = new Scanner(System.in);
             
         System.out.println("== Deposit Interest Predictor ==");
            
         String query = "SELECT * FROM Banks";
         PreparedStatement stmt = conn.prepareStatement(query);
         ResultSet rs = stmt.executeQuery();
             
while (true) {
    try {
        while (rs.next()) {
            int bankID = rs.getInt("bank_id");
            String bankName = rs.getString("bank_name");
            double interestRate = rs.getDouble("interest_rate");
            System.out.printf("%s. %s: %.2f%%%n", bankID, bankName, interestRate);
        }

        System.out.print("Enter deposit amount (-1 to quit): ");
        double depositAmount = scanner.nextDouble();
        scanner.nextLine(); 
        if (depositAmount == -1) {
            System.out.println("Returning to Transaction Menu...");
            showTransactionMenu(); 
            return; 
        }

        System.out.print("Enter your bank choice (1 to 6) or -1 to quit: ");
        String selectedBank = scanner.nextLine();
        if (selectedBank.equals("-1")) {
            System.out.println("Returning to Transaction Menu...");
            showTransactionMenu();
            return;
        }
        double interestRate = 0.0;
        query = "SELECT interest_rate FROM Banks WHERE bank_id = ?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, selectedBank);
        rs = stmt.executeQuery();
        if (rs.next()) {
            interestRate = rs.getDouble("interest_rate");
        } else {
            System.out.println("Invalid bank selection. Please try again.");
            continue; 
        }

        System.out.println("Select interest type to calculate:");
        System.out.println("1. Daily Interest");
        System.out.println("2. Monthly Interest");
        System.out.println("3. Annual Interest");
        System.out.print("Enter your choice (1-3): ");
        int interestChoice = scanner.nextInt();
        scanner.nextLine(); 

        double calculatedInterest = 0.0;
        switch (interestChoice) {
            case 1: 
                calculatedInterest = (depositAmount * interestRate) / 365;
                System.out.printf("Daily Interest: $%.2f%n", calculatedInterest);
                break;
            case 2: 
                calculatedInterest = (depositAmount * interestRate) / 12;
                System.out.printf("Monthly Interest: $%.2f%n", calculatedInterest);
                break;
            case 3: 
                calculatedInterest = depositAmount * interestRate;
                System.out.printf("Annual Interest: $%.2f%n", calculatedInterest);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }

    } catch (SQLException e) {
        System.out.println("Error retrieving bank interest rates.");
        e.printStackTrace();
    } catch (InputMismatchException e) {
        System.out.println("Invalid input. Please enter valid numbers.");
        scanner.nextLine(); 
    }
}
      } catch (SQLException e) {
         System.out.println("Error retrieving bank interest rates.");
         e.printStackTrace();
       }
     }
     
     
}

