package source;

import source.sub.InSubPack;

public class BaseClass {
	protected int fProtected;
	public int fPublic;
	public static final int CONST= 42;
	public BaseClass(int count) {
		
	}
	
	protected void baseMethod() {
		referencedMethod(Color.RED); // keep as first
		new InSubPack();
	}
	
	protected int compareTo(BaseClass other) {
		return -1;
	}
	
	public final void referencedMethod(Color c) { }
	public static int referencedStaticMethod() { return 0; }
	public void referencedVirtualMethod() { }
	
	public void paintColor(Color color) {
		color.hashCode();
	}
}
