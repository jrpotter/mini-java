package miniJava;

import java.io.*;

import mJAM.*;
import miniJava.SyntacticAnalyzer.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.Analyzer;
import miniJava.CodeGenerator.Encoder;
import miniJava.Exceptions.*;

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

			// Identification/Type Checking
			Analyzer analyzer = new Analyzer();
			analyzer.visitPackage(p, null);
			int analyzed = analyzer.validate();
			
			// Begin Compilation to mJAM
			if(analyzed == 0) {
				
				Encoder encoder = new Encoder();
				encoder.visitPackage(p, null);
				
				// Create object file
				int pos = args[0].lastIndexOf(".java");
				String objectFileName = args[0].substring(0, pos) + ".mJAM";
				ObjectFile objF = new ObjectFile(objectFileName);
				if(objF.write()) {
					System.out.println("***Object File Failed.");
				}
			}
			
			System.exit(analyzed);

		} catch (FileNotFoundException e) {
			System.out.println("***" + e.getMessage());
		} catch (IOException e) {
			System.out.println("***" + e.getMessage());
		} catch (ScanningException e) {
			System.out.println("***" + e.getMessage());
		} catch (ParsingException e) {
			System.out.println("***" + e.getMessage());
		}

		System.exit(rc);
	}

}
