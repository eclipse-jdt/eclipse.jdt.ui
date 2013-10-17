package generics_in;
import java.util.Map;

public class A_test1121<V> {
    public A_test1121(Map<?, ? extends V> c) {
        this(/*[*/c.size()/*]*/);
    }

    public A_test1121(int size) {
    }
}