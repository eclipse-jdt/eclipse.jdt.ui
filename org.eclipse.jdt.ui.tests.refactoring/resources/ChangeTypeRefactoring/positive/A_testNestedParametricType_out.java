import java.util.AbstractCollection;
import java.util.Vector;

public class A_testNestedParametricType_in {
	void foo(){
		AbstractCollection<Vector<String>> v = new Vector<Vector<String>>();
	}
}
