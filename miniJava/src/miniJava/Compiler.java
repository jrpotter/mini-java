package miniJava;

import java.io.*;

import miniJava.SyntacticAnalyzer.*;
import miniJava.SyntacticAnalyzer.Errors.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Compiler {
	
	private static final int rc = 4;
	
	public static void main(String[] args) {
		
		if(args.length == 0) {
			System.out.println("No file specified");
			System.exit(rc);
		}
		
		try(FileReader input = new FileReader(args[0])) {
			
			// Setup
			Scanner scanner = new Scanner(new BufferedReader(input));
			Parser parser = new Parser(scanner);
			Package p = parser.parse();
			
			// Display
			ASTDisplay display = new ASTDisplay();
			display.showTree(p);
			System.exit(0);
			
		} catch(FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch(IOException e) {
			System.out.println(e.getMessage());
		}catch(ScanningException e) {
			System.out.println(e.getMessage());
		} catch(ParsingException e) {
			System.out.println(e.getMessage());
		}
		
		System.exit(rc);
	}

}
