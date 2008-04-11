package source;

public class BaseClass {
	protected int fProtected;
	public int fPublic;
	public BaseClass(int count) {
		
	}
	
	protected void baseMethod() {
		
	}
	
	protected int compareTo(BaseClass other) {
		return -1;
	}
	
	public final void referencedMethod() { }
	public static void referencedStaticMethod() { }
	public void referencedVirtualMethod() { }
	
	public void paintColor(Color color) {
		color.hashCode();
	}
}
