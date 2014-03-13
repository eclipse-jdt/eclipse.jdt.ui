package generics_out;
import java.util.Map;

public class A_test1121<V> {
    public A_test1121(Map<?, ? extends V> c) {
        this(extracted(c));
    }

	protected static <V> int extracted(Map<?, ? extends V> c) {
		return /*[*/c.size()/*]*/;
	}

    public A_test1121(int size) {
    }
}