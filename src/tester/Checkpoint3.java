package tester;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/* Automated regression tester for Checkpoint 3 tests
 * Created by Max Beckman-Harned
 * Put your tests in "tests/pa3_tests" folder in your Eclipse workspace directory
 * If you preface your error messages / exceptions with ERROR or *** then they will be displayed if they appear during processing
 */

public class Checkpoint3 {

    public static void main(String[] args) throws IOException, InterruptedException {
        File testDir = new File(System.getProperty("java.class.path") + "/../tests/pa3_tests");
        int failures = 0;
        for (File x : testDir.listFiles()) {
            if (x.getName().endsWith("out") || x.getName().startsWith(".") || x.getName().endsWith("mJAM")
                    || x.getName().endsWith("asm"))
                continue;
            int returnCode = runTest(x);
            if (x.getName().indexOf("pass") != -1) {
                if (returnCode == 0) {
                    System.out.println(x.getName() + " processed successfully!");
                } else {
                    failures++;
                    System.err.println(x.getName() + " failed to be processed!");
                }
            } else {
                if (returnCode == 4)
                    System.out.println(x.getName() + " failed successfully!");
                else {
                    System.err.println(x.getName() + " did not fail properly!");
                    failures++;
                }
            }
        }
        System.out.println(failures + " failures in all.");
    }

    private static int runTest(File x) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java", "miniJava.Compiler", x.getPath())
                .directory(new File(System.getProperty("java.class.path")));
        pb.redirectErrorStream(true);
        Process p = pb.start();

        processStream(p.getInputStream());
        p.waitFor();
        int exitValue = p.exitValue();
        return exitValue;
    }

    public static void processStream(InputStream stream) {
        Scanner scan = new Scanner(stream);
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.startsWith("*** "))
                System.out.println(line);
            if (line.startsWith("ERROR")) {
                System.out.println(line);
                // while(scan.hasNext())
                // System.out.println(scan.next());
            }
        }
        scan.close();
    }
}
