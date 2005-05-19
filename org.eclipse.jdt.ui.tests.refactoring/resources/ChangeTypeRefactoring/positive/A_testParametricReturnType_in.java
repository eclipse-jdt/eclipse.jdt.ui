import java.util.Vector;

//see bug #94715
public class A_testParametricReturnType_in {
	public Vector<String> foo() {
		return new Vector<String>();
	}
	public Vector bar(){
		return new Vector();
	}
	
}

