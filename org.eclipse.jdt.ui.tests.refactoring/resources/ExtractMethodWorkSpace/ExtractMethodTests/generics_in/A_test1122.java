package generics_in;
import java.util.ArrayList;
import java.util.List;

public class A_test1122{
	public <E> void foo() {
		List<? extends E> t = new ArrayList<E>();
		/*[*/t.size();/*]*/
	}
}