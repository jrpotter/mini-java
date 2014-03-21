package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import java.util.ArrayList;

import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Type;
import miniJava.AbstractSyntaxTrees.TypeKind;

public class IdentificationTable {
	
	private IdentificationTable parent;
	private HashMap<String, IdentificationTable> scope;
	private ArrayList<HashMap<String, Declaration>> table;
	
	/**
	 * 
	 */
	public IdentificationTable() {
		this(null);
	}
	
	/**
	 * 
	 * @param parent
	 */
	public IdentificationTable(IdentificationTable parent) {
		this.parent = parent;
		this.scope = new HashMap<String, IdentificationTable>();
		this.table = new ArrayList<HashMap<String, Declaration>>();
		this.table.add(new HashMap<String, Declaration>());
	}
	
	/**
	 * Adds another level for variables to be stored at (they will be 
	 * removed when popping said level).
	 */
	public void pushLevel() {
		table.add(new HashMap<String, Declaration>());
	}
	
	/**
	 * Removes all variables declared at the current level (these are
	 * no longer accessible).
	 */
	public void popLevel() {
		table.remove(table.size() - 1);
	}
	
	/**
	 * This method will only ever be called with class/method declarations
	 * @param decl
	 * @return
	 */
	public IdentificationTable openScope(Declaration decl) {
		if(scope.containsKey(decl.name) || getDeclarationAtScope(decl.name) != null) {
			Reporter.report(ErrorType.REDEFINITION, decl);
			return null;
		} else {
			table.get(table.size() - 1).put(decl.name, decl);
			scope.put(decl.name, new IdentificationTable(this));
			return scope.get(decl.name);
		}
	}
	
	/**
	 * Return nested scope corresponding to declaration (or null if non-existant).
	 * @param decl
	 * @return
	 */
	public IdentificationTable getScope(Declaration decl) {
		if(scope.containsKey(decl.name)) {
			return scope.get(decl.name);
		}
		
		return null;
	}
		
	/**
	 * Iterates through all parents and tries to find the specified
	 * declaration by name.
	 * @param name
	 * @return
	 */
	public Declaration getDeclaration(String name) {
		IdentificationTable current = this;
		while(current != null) {
			Declaration decl = current.getDeclarationAtScope(name);
			if(decl == null) {
				current = current.parent;
			} else {
				return decl;
			}
		}
		
		return null;
	}
	
	/**
	 * Iterates through levels (from higher to lower) for declaration,
	 * returning none if it does not exist.
	 * @param name
	 * @return
	 */
	public Declaration getDeclarationAtScope(String name) {
		for(int i = table.size() - 1; i >= 0; i--) {
			HashMap<String, Declaration> level = table.get(i);
			if(level.containsKey(name)) {
				return level.get(name);
			}
		}
		
		return null;
	}
	
	/**
	 * Add declaration to current table's table member.
	 * @param name
	 */
	public void setDeclarationAtScope(Declaration decl) {
		for(int i = 0; i < table.size(); i++) {
			HashMap<String, Declaration> level = table.get(i);
			if(level.containsKey(decl.name)) {
				Reporter.report(ErrorType.REDEFINITION, decl);
				return;
			}
		}
		
		table.get(table.size() - 1).put(decl.name, decl);
	}
	
	/**
	 * Checks that the passed class type does exist.
	 * @param ct
	 */
	public void validateClassId(Type t) {
		if(t.typeKind == TypeKind.CLASS) {
			ClassType ct = (ClassType) t;
			if(getDeclaration(ct.className.spelling) == null) {
				Reporter.report(ErrorType.MISSING_DECL, ct);
			}
		}
	}
}
