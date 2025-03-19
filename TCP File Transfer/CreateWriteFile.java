// from W3Schools: Java Files
// + self
import java.util.Scanner;
import java.io.*;

public class CreateWriteFile {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String currentpath = System.getProperty("user.dir") + File.separator;
        String filename;
        String filepath;
        System.out.print("Enter the name of the file you want to create: ");
        filename = scanner.nextLine();
        filepath = currentpath + filename;
        System.out.printf("Attempting to create file in:\n%s\t\n", filepath);
        try {
            File myObj = new File(filepath);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
                FileWriter myWriter = new FileWriter(myObj);
                myWriter.write("Files in Java might be tricky, but it is fun enough!");
                myWriter.close();
                System.out.println("Successfully wrote to the file.");
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
