package miniJava.AbstractSyntaxTrees;

public class Declarators {
	
	public Declarators(boolean isPrivate, boolean isStatic, Type mt) {
		this.isPrivate = isPrivate;
		this.isStatic = isStatic;
		this.mt = mt;
	}
	
	public boolean isPrivate;
    public boolean isStatic;
    public Type mt;
}
