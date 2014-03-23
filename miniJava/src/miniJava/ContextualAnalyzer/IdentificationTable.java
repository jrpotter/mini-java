package miniJava.ContextualAnalyzer;

import java.util.*;

import miniJava.AbstractSyntaxTrees.*;

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
	 * Adds another level for variables to be stored at
	 */
	public void popLevel() {
		table.remove(table.size() - 1);
	}

	/**
	 * Removes all variables declared at the current level
	 */
	public void pushLevel() {
		table.add(new HashMap<String, Declaration>());
	}

	/**
	 * This method will only ever be called with class/method declarations.
	 * 
	 * @param decl
	 * @return
	 */
	public IdentificationTable openScope(Declaration decl) {
		Declaration current = getDeclarationAtScope(decl.name);
		if (scope.containsKey(decl.name) || current != null) {
			Reporter.report(ErrorType.REDEFINITION, decl, current);
			return null;
		} else {
			table.get(table.size() - 1).put(decl.name, decl);
			scope.put(decl.name, new IdentificationTable(this));
			return scope.get(decl.name);
		}
	}

	/**
	 * Return nested scope corresponding to declaration (or null if
	 * non-existant).
	 * 
	 * @param decl
	 * @return
	 */
	public IdentificationTable getScope(Declaration decl) {
		if (scope.containsKey(decl.name)) {
			return scope.get(decl.name);
		}

		return null;
	}

	/**
	 * Iterates through all parents and tries to find the specified declaration
	 * by name.
	 * 
	 * @param name
	 * @return
	 */
	public Declaration getDeclaration(String name) {
		IdentificationTable current = this;
		while (current != null) {
			Declaration decl = current.getDeclarationAtScope(name);
			if (decl == null) current = current.parent;
			else return decl;
		}

		return null;
	}

	/**
	 * Iterates through levels (from higher to lower) for declaration, returning
	 * none if it does not exist.
	 * 
	 * @param name
	 * @return
	 */
	public Declaration getDeclarationAtScope(String name) {
		for (int i = table.size() - 1; i >= 0; i--) {
			HashMap<String, Declaration> level = table.get(i);
			if (level.containsKey(name)) return level.get(name);
		}

		return null;
	}

	/**
	 * Add declaration to current table's table member.
	 * 
	 * @param name
	 */
	public void setDeclarationAtScope(Declaration decl) {
		for (int i = 0; i < table.size(); i++) {
			HashMap<String, Declaration> level = table.get(i);
			if (level.containsKey(decl.name)) {
				Declaration defined = level.get(decl.name);
				Reporter.report(ErrorType.REDEFINITION, decl, defined);
				return;
			}
		}

		table.get(table.size() - 1).put(decl.name, decl);
	}

	/**
	 * Checks whether the specified class has been declared.
	 * 
	 * @param t
	 * @return
	 */
	public boolean classExists(Type t) {
		if (t.typeKind == TypeKind.CLASS) {
			ClassType ct = (ClassType) t;
			return getDeclaration(ct.className.spelling) != null;
		}

		return true;
	}

	/**
	 * Determines whether two types match.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static boolean match(Type t1, Type t2) {
		return IdentificationTable.match(t1, t2, false);
	}

	/**
	 * Determines whether two type match, reporting an error if they do not.
	 * 
	 * @param t1
	 * @param t2
	 * @param report
	 * @return
	 */
	public static boolean match(Type t1, Type t2, boolean report) {

		if (t1.typeKind != t2.typeKind) {
			if (report) Reporter.report(ErrorType.TYPE_MISMATCH, t1, t2);
			return false;
		}

		// Check Class Types match
		else if (t1.typeKind == TypeKind.CLASS) {
			ClassType c1 = (ClassType) t1;
			ClassType c2 = (ClassType) t2;
			if (!c1.className.spelling.equals(c2.className.spelling)) {
				if (report) Reporter.report(ErrorType.TYPE_MISMATCH, t1, t2);
				return false;
			}
		}

		// Check array types match
		else if (t1.typeKind == TypeKind.ARRAY) {
			ArrayType a1 = (ArrayType) t1;
			ArrayType a2 = (ArrayType) t2;
			if (!IdentificationTable.match(a1.eltType, a2.eltType)) {
				if (report) Reporter.report(ErrorType.TYPE_MISMATCH, t1, t2);
				return false;
			}
		}

		return true;
	}

	/**
	 * Determines if the passed method is a valid entry point for the
	 * compilation phase.
	 * 
	 * @param md
	 * @return
	 */
	public static boolean isMainMethod(MethodDecl md) {

		// Check Declaration
		if (!md.isPrivate && md.isStatic && md.type.typeKind == TypeKind.VOID
				&& md.name.equals("main") && md.parameterDeclList.size() == 1) {

			// Check Parameter Declaration
			ParameterDecl pd = md.parameterDeclList.get(0);

			if (pd.type.typeKind != TypeKind.ARRAY) return false;
			ArrayType at = (ArrayType) pd.type;

			if (at.eltType.typeKind != TypeKind.CLASS) return false;
			ClassType ct = (ClassType) at.eltType;

			return ct.className.spelling.equals("String");
		}

		return false;
	}
}
