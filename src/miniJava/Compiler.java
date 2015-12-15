package miniJava;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGenerator.Encoder;
import miniJava.ContextualAnalyzer.IdTable;
import miniJava.ContextualAnalyzer.Analyzer;
import miniJava.ContextualAnalyzer.Reporter;

public class Compiler {

    public static final int rc = 4;

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("No file specified");
            System.exit(rc);
        }

        try (FileReader input = new FileReader(args[0])) {

            // Setup
            Scanner scanner = new Scanner(new BufferedReader(input));
            Parser parser = new Parser(scanner);
            Package p = parser.parse();

            // Display
            // ASTDisplay display = new ASTDisplay();
            // display.showTree(p);

            // Contextual Analyzer
            IdTable table = new IdTable();
            Analyzer analyzer = new Analyzer();
            analyzer.visitPackage(p, table);

            // Compilation
            if (Reporter.error) {
                System.exit(rc);
            } else {

                // Build mJAM assembly
                Encoder encoder = new Encoder();
                encoder.visitPackage(p, null);

                // Create object file
                int pos = args[0].lastIndexOf(".java");
                String objectFileName = args[0].substring(0, pos) + ".mJAM";
                ObjectFile objF = new ObjectFile(objectFileName);
                if (objF.write()) {
                    Reporter.emit("Object File Failed.");
                }
            }

            System.exit(0);

        } catch (FileNotFoundException e) {
            Reporter.emit(e.getMessage());
        } catch (IOException e) {
            Reporter.emit(e.getMessage());
        }

        System.exit(rc);
    }

}
