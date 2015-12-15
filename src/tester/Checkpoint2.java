package tester;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Scanner;

/* Automated regression tester for Checkpoint 2 tests
 * Created by Max Beckman-Harned
 * Put your tests in "tests/pa2_tests" folder in your Eclipse workspace directory
 * If you preface your error messages / exceptions with ERROR or *** then they will be displayed if they appear during processing
 */

public class Checkpoint2 {

    private static class ReturnInfo {
        int returnCode;
        String ast;

        public ReturnInfo(int _returnCode, String _ast) {
            returnCode = _returnCode;
            ast = _ast;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        File testDir = new File(System.getProperty("java.class.path") + "/../tests/pa2_tests");
        int failures = 0;
        for (File x : testDir.listFiles()) {
            if (x.getName().endsWith("out") || x.getName().startsWith("."))
                continue;
            ReturnInfo info = runTest(x);
            int returnCode = info.returnCode;
            String ast = info.ast;
            if (x.getName().indexOf("pass") != -1) {
                if (returnCode == 0) {
                    String actualAST = getAST(new FileInputStream(x.getPath() + ".out"));
                    if (actualAST.equals(ast))
                        System.out.println(x.getName() + " parsed successfully and has a correct AST!");
                    else {
                        System.err.println(x.getName() + " parsed successfully but has an incorrect AST!");
                        failures++;
                    }
                } else {
                    failures++;
                    System.err.println(x.getName() + " failed to be parsed!");
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

    private static ReturnInfo runTest(File x) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java", "miniJava.Compiler", x.getPath())
                .directory(new File(System.getProperty("java.class.path")));
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String ast = getAST(p.getInputStream());
        p.waitFor();
        int exitValue = p.exitValue();
        return new ReturnInfo(exitValue, ast);
    }

    public static String getAST(InputStream stream) {
        Scanner scan = new Scanner(stream);
        String ast = null;
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.equals("======= AST Display =========================")) {
                line = scan.nextLine();
                while (scan.hasNext() && !line.equals("=============================================")) {
                    ast += line + "\n";
                    line = scan.nextLine();
                }
            }
            if (line.startsWith("*** "))
                System.out.println(line);
            if (line.startsWith("ERROR")) {
                System.out.println(line);
                while (scan.hasNext())
                    System.out.println(scan.next());
            }
        }
        scan.close();
        return ast;
    }
}
