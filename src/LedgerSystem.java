import java.io.FileWriter; //Help write data to a file, will learn in week 7  
import java.io.IOException; //Used for handling input/output errors(e.g. file-writing issues)
import java.sql.*; //All the SQL stuff
import java.util.InputMismatchException;
import java.util.Scanner; //Allow to do input
import java.util.regex.*; //Used to validate the emails and passwords, by matching them to patterns. 

public class LedgerSystem {
   private static String userName = "";
   private static int userId;
   private static final String URL = "jdbc:mysql://localhost:3306/Ledger_System";
   private static final String USER = "root";
   private static final String PASSWORD = "";


   private static boolean validateEmail(String email) {
      String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"; 
      //^ and $ is the start and end of the string
      //A-Za-z0-9, meaning all letters and numbers;
      //+_.- meaning that allow additional characters like .,_, - 
      //@ meaning that email must have @
      //+@ here meaning that after the first set, like chuayujien then the follow up must have alias(@), resonates the gmail
      
      /*** Here maybe can do some more validation like only can @gmail, @yahoo. */
      return Pattern.compile(regex).matcher(email).matches();
      //matcher, which one needed to be matches
      //compile, compiles the provided regular expression, check for conditions
      //matches, if is true or false. 

   }

   private static boolean validatePassword(String password) {
      return password.length() >= 6 && password.matches(".*[!@#$%^&*(),.?\":{}|<>-].*");
      //The symbols there meaning at least 6 characters + at least one special character.
      //.* any number of characters(including none) before and after. 
      //[!@#$%^&*(),.?\":{}|<>-], these characters.
   }

   private static boolean validateName(String name) {
      return name.matches("^[a-zA-Z0-9 ]+$");
      //^ $, from start to end
      //accept only letters and numbers. 
   }      


   //This one is main(1)

   public static void main(String[] args) {
      Scanner scanner = new Scanner(System.in); //Input class.
      int option; //declare an integer option.

      do {
         System.out.println("1. Login");
         System.out.println("2. Register");
         System.out.print("Choose an option: ");
         option = scanner.nextInt();
         scanner.nextLine();

         switch (option) {
            case 1 -> loginUser(); //run the login user if is 1
            case 2 -> registerUser(); // run the register user if is 2.
            default -> System.out.println("Invalid option.");
         }
      } while (true);
   }


   //(2a) if select 1, when logging in or registering
   private static void loginUser() {
      Scanner scanner = new Scanner(System.in);
      System.out.println("== Please enter your email and password ==");
      System.out.print("Email: ");
      String email = scanner.nextLine();
      System.out.print("Password: ");
      String password = scanner.nextLine();

      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
         //Here is to get the connection done, calling its method macam the scanner.
         //DriverManager is part of the JDBC(Jaca Database Connectivity) API. acts as a central service for managing database drivers. 
   
         String query = "SELECT * FROM Users WHERE email = ? AND password = ?";
         PreparedStatement stmt = conn.prepareStatement(query);
         //Prepared Statement is JDBC API a special type of command you give to database, telling it what data you want to manipulate. Its like prepare beforehand
         //? telling what the actual values I am going to put it in
         stmt.setString(1, email);
         stmt.setString(2, password);
         //So the first ? is email, ensures that the data you insert into SQL query is treated as String type.
         //second ? is password. 

         ResultSet resultSet = stmt.executeQuery();
         //This executeQuery() method runs the query and returns the results in a ResultSet Object
         if (resultSet.next()) {
            //this moves the cursor to the next row in the ResultSet. the cursor is positioned before the first row, so calling next() for the first time moves it to the first row.
            //So this one, if there is no such email and password, there is no row, so the resultSet.next() will return false because empty, but if there is, macam the SQL workbench, there is this such record, then resultSet.next() = true.
            userId = resultSet.getInt("user_id"); //This one, temporarily not useful here first, but soon after yep.
            userName = resultSet.getString("name");
            System.out.println("Login Successful!!!");
            System.out.println("== Welcome, " + userName + " ==");

            displayBalance(); //lead to 3a
            showTransactionMenu(); //lead to 3b

         } else {
            System.out.println("Invalid email or password.");
         }
      } catch (SQLException e) {
         System.out.println("Error logging in.");
         e.printStackTrace();
         //This line is to print the error message when being lead to that error, nothing much.
         //It prints out useful information for debugging, showing the exact line number and method in your code where the error happened.
      }
   }


   //(2b)... when registering the User
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
         System.out.println("Name must be alphanumeric(alphabets or/and numbers) and cannot contain special characters.");
      } else if (!validateEmail(email)) {
         System.out.println("Invalid email format.");
      } else if (!validatePassword(password)) {
         System.out.println("Password must be at least 6 characters long and contain at least one special character.");
      } else if (!password.equals(confirmPassword)) {
         System.out.println("Passwords do not match.");
      } else {
         try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String userQuery = "INSERT INTO Users (name, email, password) VALUES (?, ?, ?)";
            PreparedStatement userStmt = conn.prepareStatement(userQuery, Statement.RETURN_GENERATED_KEYS);
            //For the userQuery, meaning the ? will be replaced with actual values later, then the Statement.
            //RETURN_GENERATED_KEYS indicates that you want to get the auto-generated keys (like user ID). so the user ID will be auto-incremented.
            userStmt.setString(1, name);
            userStmt.setString(2, email);
            userStmt.setString(3, password);
            userStmt.executeUpdate();
            //Update the registration in the SQL workbench

            ResultSet generatedKeys = userStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
               //yes, got new input, usually this will be right
               int userId = generatedKeys.getInt(1);
               //getInt(1), meaning that gets the generated user ID< which is the unique identifier for the new user.

               String balanceQuery = "INSERT INTO Account_Balance (user_id, balance) VALUES (?, 0.00)";
               PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery);
               balanceStmt.setInt(1, userId);
               balanceStmt.executeUpdate();
               //Runs the query to add the balance information
               balanceStmt.close();
               //Closes the statement to free up resources.
            }

            userStmt.close();
            //close the user statement to free up resources.
            System.out.println("Register Successful!!!");

         } catch (SQLException e) {
            System.out.println("Error registering user. Email might already be taken.");
            e.printStackTrace();
         }
      }
   }

   
   //From the loginUser platform. 

   private static void displayBalance() {
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
         String query = "SELECT balance FROM Account_Balance WHERE user_id = ?";
         PreparedStatement stmt = conn.prepareStatement(query);
         stmt.setInt(1, userId); 
         //sets the value of the first placeholder (?) to user_id
         //meaning that the first one that popped up, yep, immediately link to user_id.

         ResultSet resultSet = stmt.executeQuery();
         if (resultSet.next()) {
            //yep, there is such record.
            double balance = resultSet.getDouble("balance");
            //retrives the balance of the current row. 
            //of course, its from the balance attributes. 
            System.out.printf("Balance: %.2f%n", balance);
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
               debitTransaction(amount, description); //This one is 4
            }
            case 2 -> {
               System.out.print("Enter amount to credit: ");
               double amount = scanner.nextDouble();
               scanner.nextLine();
               System.out.print("Enter description: ");
               String description = scanner.nextLine();
               creditTransaction(amount, description); //This one is 5
            }
            case 3 -> viewTransactionHistory(); //This one is 6
            case 4 -> manageSavings();
           // case 5 -> creditandloan();
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
         String balanceQuery = "UPDATE Account_Balance SET balance = balance + ? WHERE user_id = ?";
         //Update in username, 
         PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery);
         balanceStmt.setDouble(1, amount); //1 meaning the first (?)
         balanceStmt.setInt(2, userId); // 2 meaning the second (?)
         balanceStmt.executeUpdate();

         String transactionQuery = "INSERT INTO Transactions (user_id, transaction_type, amount, description) VALUES (?, 'debit', ?, ?)";
         //Even though you already add here, but to update in SQL workbench, thats why you need to INSERT INTO.amount
         PreparedStatement transactionStmt = conn.prepareStatement(transactionQuery);
         transactionStmt.setInt(1, userId);
         transactionStmt.setDouble(2, amount);
         transactionStmt.setString(3, description);
         transactionStmt.executeUpdate();

         System.out.println("Debit Successfully Recorded!");
         displayBalance();
         //back to the displayBalance() above. 
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
         displayBalance();
      } catch (SQLException e) {
         System.out.println("Error processing credit transaction.");
         e.printStackTrace();
      }
   }


   private static void viewTransactionHistory() {
      String query = "SELECT date, description, transaction_type, amount FROM Transactions WHERE user_id = ? ORDER BY date DESC";
      double runningBalance = 0.0;

      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD); //This one idk why, but because now no error, thats why nothing happens. 
         PreparedStatement stmt = conn.prepareStatement(query)) {
         stmt.setInt(1, userId);
         ResultSet rs = stmt.executeQuery();
         //Run the query.
         
         System.out.println("== History ==");
         System.out.println("Date       | Description               | Debit     | Credit    | Balance");

         while (rs.next()) {
            //Yep, there are records of the person's credit and debit, one by one being printed out.
            String date = rs.getDate("date").toString();
            //The date make it into string
            String description = rs.getString("description");
            String type = rs.getString("transaction_type");
            double amount = rs.getDouble("amount");
            
            if (type.equals("debit")) {
               runningBalance += amount;
               System.out.printf("%-10s | %-25s | %-9.2f | %-9s | %-9.2f\n", date, description, amount, "", runningBalance);
               //blank after the amount because thats credit, so just ignore. 
            } else {
               runningBalance -= amount;
               System.out.printf("%-10s | %-25s | %-9s | %-9.2f | %-9.2f\n", date, description, "", amount, runningBalance);
            }

            //the runningBalance shows the remaining Balance that the person has in their balances, maybe can do comparison in future so that there is no error in the system. 
            //So based on my code here, it does not have any uses. 
         }
         exportToCSV(); //Another one as well.

      } catch (SQLException e) {
         System.out.println("Error retrieving transaction history.");
         e.printStackTrace();
      }      
   }

   private static void exportToCSV() {
      Scanner scanner = new Scanner(System.in);
      System.out.print("Export to CSV? (y/n): ");
      if (!scanner.nextLine().equalsIgnoreCase("y")) return;

      String query = "SELECT date, description, transaction_type, amount FROM Transactions WHERE user_id = ? ORDER BY date DESC";
      double runningBalance = 0.0;

      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
           PreparedStatement stmt = conn.prepareStatement(query);
           FileWriter writer = new FileWriter("TransactionHistory.csv")) {
           //Yes, week 7 syllabus. 
           
         stmt.setInt(1, userId);
         ResultSet rs = stmt.executeQuery();
         //Run the query again. 

         writer.append("Date,Description,Debit,Credit,Balance\n");
         //append these in the CSV
         while (rs.next()) {
            //means if there is, then move to the next row. 
            String date = rs.getDate("date").toString();
            String description = rs.getString("description");
            String type = rs.getString("transaction_type");
            double amount = rs.getDouble("amount");

            if (type.equals("debit")) {
               runningBalance += amount;
               writer.append(String.format("%s,%s,%.2f,,%.2f\n", date, description, amount, runningBalance));
               //Now I see the use of running Balance.
               //,, because blank space since it is in the credit column.
               //using comma to go to next cell box. 
            } else {
               runningBalance -= amount;
               writer.append(String.format("%s,%s,,%.2f,%.2f\n", date, description, amount, runningBalance));
            }
         }
         System.out.println("File exported.");

      } catch (SQLException | IOException e) {
         // | = OR
         System.out.println("Error exporting transaction history to CSV.");
         e.printStackTrace();
      }     
   }

   private static void manageSavings() {
      Scanner scanner = new Scanner(System.in);
      System.out.println("== Savings ==");
      System.out.print("Are you sure you want to activate it? (Y/N): ");
      String response = scanner.nextLine();
  
      if (!response.equalsIgnoreCase("Y")) {
         System.out.println("Savings not activated.");
         try {
             Connection conn2 = DriverManager.getConnection(URL, USER, PASSWORD);
             String query2 = "DELETE FROM Savings WHERE user_id = ?";
             PreparedStatement checkStmt = conn2.prepareStatement(query2);
             checkStmt.setInt(1, userId); // Ensure userId is correctly passed here
             int rowsAffected = checkStmt.executeUpdate(); // Use executeUpdate instead of executeQuery
     
             if (rowsAffected > 0) {
                 System.out.println("Saving record deleted.");
             } else {
                 System.out.println("No saving record found for the user.");
             }
         } catch (SQLException e) {
             System.out.println("Error managing savings.");
             e.printStackTrace();
         }
         return;
     }
  
      System.out.print("Please enter the percentage you wish to deduct from the next debit (1-100): ");
      int percentage = scanner.nextInt();
      scanner.nextLine();
  
      if (percentage < 1 || percentage > 100) {
          System.out.println("Invalid percentage. Please try again.");
          return;
      }
  
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
          // Check if savings already exist
          String checkQuery = "SELECT * FROM Savings WHERE user_id = ?";
          PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
          checkStmt.setInt(1, userId);
          ResultSet rs = checkStmt.executeQuery();
  
          if (rs.next()) {
              System.out.println("Savings is already active. Updating the percentage...");
              String updateQuery = "UPDATE Savings SET percentage = ?, status = 'active' WHERE user_id = ?";
              PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
              updateStmt.setInt(1, percentage);
              updateStmt.setInt(2, userId);
              updateStmt.executeUpdate();
              System.out.println("Savings percentage updated successfully!");
          } else {
              String insertQuery = "INSERT INTO Savings (user_id, status, percentage) VALUES (?, 'active', ?)";
              PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
              insertStmt.setInt(1, userId);
              insertStmt.setInt(2, percentage);
              insertStmt.executeUpdate();
              System.out.println("Savings activated successfully!");
          }
      } catch (SQLException e) {
          System.out.println("Error managing savings.");
          e.printStackTrace();
      }
  }

   private static void transferSavingsToBalance() {
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
          String transferQuery = "SELECT * FROM Savings WHERE user_id = ?";
          PreparedStatement transferStmt = conn.prepareStatement(transferQuery);
          transferStmt.setInt(1, userId);
          ResultSet rs = transferStmt.executeQuery();
  
          if (rs.next() && "active".equalsIgnoreCase(rs.getString("status"))) {
              int percentage = rs.getInt("percentage");
              double transferAmount = (percentage * 0.01);
              String updateBalance = "UPDATE Account_Balance SET balance = balance + ? WHERE user_id = ?";
              PreparedStatement updateStmt = conn.prepareStatement(updateBalance);
              updateStmt.setDouble(1, transferAmount);
              updateStmt.setInt(2, userId);
              updateStmt.executeUpdate();
  
              System.out.printf("End of month savings (%.2f) transferred to balance.%n", transferAmount);
          } else {
              System.out.println("No active savings to transfer.");
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
        // Display banks
        while (rs.next()) {
            int bankID = rs.getInt("bank_id");
            String bankName = rs.getString("bank_name");
            double interestRate = rs.getDouble("interest_rate");
            System.out.printf("%s. %s: %.2f%%%n", bankID, bankName, interestRate);
        }

        // Enter deposit amount
        System.out.print("Enter deposit amount (-1 to quit): ");
        double depositAmount = scanner.nextDouble();
        scanner.nextLine(); // Consume newline character
        if (depositAmount == -1) {
            System.out.println("Returning to Transaction Menu...");
            showTransactionMenu(); // Call your transaction menu method
            return; // Exit the current method
        }

        // Enter bank choice
        System.out.print("Enter your bank choice (1 to 6) or -1 to quit: ");
        String selectedBank = scanner.nextLine();
        if (selectedBank.equals("-1")) {
            System.out.println("Returning to Transaction Menu...");
            showTransactionMenu();
            return;
        }

        // Retrieve interest rate
        double interestRate = 0.0;
        query = "SELECT interest_rate FROM Banks WHERE bank_id = ?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, selectedBank);
        rs = stmt.executeQuery();
        if (rs.next()) {
            interestRate = rs.getDouble("interest_rate");
        } else {
            System.out.println("Invalid bank selection. Please try again.");
            continue; // Restart loop
        }

        // Interest Calculation Choice
        System.out.println("Select interest type to calculate:");
        System.out.println("1. Daily Interest");
        System.out.println("2. Monthly Interest");
        System.out.println("3. Annual Interest");
        System.out.print("Enter your choice (1-3): ");
        int interestChoice = scanner.nextInt();
        scanner.nextLine(); // Consume newline character

        double calculatedInterest = 0.0;
        switch (interestChoice) {
            case 1: // Daily Interest
                calculatedInterest = (depositAmount * interestRate) / 365;
                System.out.printf("Daily Interest: $%.2f%n", calculatedInterest);
                break;
            case 2: // Monthly Interest
                calculatedInterest = (depositAmount * interestRate) / 12;
                System.out.printf("Monthly Interest: $%.2f%n", calculatedInterest);
                break;
            case 3: // Annual Interest
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
        scanner.nextLine(); // Clear buffer
    }
}
      } catch (SQLException e) {
         System.out.println("Error retrieving bank interest rates.");
         e.printStackTrace();
       }
     }
     
     
}
