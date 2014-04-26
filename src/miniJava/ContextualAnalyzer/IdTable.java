package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import java.util.ArrayList;
import miniJava.Compiler;

import miniJava.AbstractSyntaxTrees.*;

/**
 *
 */
public class IdTable {
	
	private IdTable parent;
	private ArrayList<HashMap<String, Declaration>> scope;
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// CONSTRUCTORS
	//
	// /////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
	public IdTable() {
		this(null);
	}
	
	/**
	 * 
	 * @param parent
	 */
	public IdTable(IdTable parent) {
		this.parent = parent;
		this.scope = new ArrayList<>();
		push();
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// ACTIVE SCOPE
	//
	// /////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
	public void pop() {
		int last = scope.size() - 1;
		scope.remove(last);
	}
	
	/**
	 * 
	 */
	public void push() {
		HashMap<String, Declaration> nested = new HashMap<>();
		scope.add(nested);
	}
	
	/**
	 * 
	 */
	public void add(Declaration decl) {
		for(int i = 0; i < scope.size(); i++) {
			HashMap<String, Declaration> nest = scope.get(i);
			if(nest.containsKey(decl.name)) {
				
				Declaration prev = nest.get(decl.name);
				
				if(decl instanceof ClassDecl) {
					Reporter.report(decl, prev, "Class");
				} else if(decl instanceof FieldDecl) {
					Reporter.report(decl, prev, "Field");
				} else if(decl instanceof MethodDecl) {
					Reporter.report(decl, prev, "Method");
				} else if(decl instanceof ParameterDecl) {
					Reporter.report(decl, prev, "Parameter");
				} else if(decl instanceof VarDecl) {
					Reporter.report(decl, prev, "Variable");
				}
				
				System.exit(Compiler.rc);
			}
		}
		
		scope.get(scope.size()-1).put(decl.name, decl);
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// GETTERS
	//
	// /////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * @param name
	 */
	public Declaration getDeclaration(String name) {
		IdTable current = this;
		while (current != null) {
			Declaration decl = current.getDeclarationAtScope(name);
			if (decl == null) current = current.parent;
			else return decl;
		}

		return null;
	}
	
	/**
	 * 
	 * @param name
	 */
	public Declaration getDeclarationAtScope(String name) {
		for (int i = scope.size() - 1; i >= 0; i--) {
			HashMap<String, Declaration> nest = scope.get(i);
			if (nest.containsKey(name)) return nest.get(name);
		}
		
		return null;
	}
}
