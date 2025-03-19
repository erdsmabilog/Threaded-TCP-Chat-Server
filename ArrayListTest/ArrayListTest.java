import java.util.ArrayList;
import java.util.Scanner;

public class ArrayListTest {
    public static void main(String[] args) {
        ArrayList<Integer> intArrList= new ArrayList<Integer>();
        Scanner scanner = new Scanner(System.in);
        intArrList.add(1);
        intArrList.add(2);
        intArrList.add(3);
        Thread ThreadTester = new Thread(new ThreadTester(intArrList));
        ThreadTester.start();
        int input = 0;
        while (input != -1) {
            input = scanner.nextInt();
            intArrList.add(input);
        }
    }
}

class ThreadTester implements Runnable {
    ArrayList<Integer> intArrList;

    ThreadTester(ArrayList<Integer> intArrList) {
        this.intArrList = intArrList;
    }

    @Override
    public void run() {
        while (true) {
            System.out.println(intArrList);
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {}
        }
    }
}