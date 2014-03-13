package generics_out;
import java.util.ArrayList;
import java.util.List;

public class A_test1122{
	public <E> void foo() {
		List<? extends E> t = new ArrayList<E>();
		extracted(t);
	}

	protected <E> void extracted(List<? extends E> t) {
		/*[*/t.size();/*]*/
	}
}