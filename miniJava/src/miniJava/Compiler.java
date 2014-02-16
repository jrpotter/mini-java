package miniJava;

import java.io.*;
import miniJava.SyntacticAnalyzer.*;

public class Compiler {
	
	private static final int rc = 4;
	
	public static void main(String[] args) {
		
		if(args.length == 0) {
			System.out.println("No file specified");
			System.exit(rc);
		}
		
		try(FileReader input = new FileReader(args[0])) {
			
			Scanner scanner = new Scanner(new BufferedReader(input));
			Parser parser = new Parser(scanner);
			parser.parse();
			
			System.out.println("Works");
			System.exit(0);
			
		} catch(FileNotFoundException e) {
			System.out.println("Not Found");
			System.exit(rc);
		} catch(IOException e) {
			System.out.println("Not Works");
			System.exit(rc);
		}
	}

}
