// from W3Schools: Java Files
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CreateWriteFile {
    public static void main(String[] args) throws IOException {
        try {
            File myObj = new File("nice.txt");
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
