package generic_in;

import java.util.ArrayList;
import java.util.List;

public class TestParameterizedMethod {
	void foo(ArrayList<String> al) {
		/*]*/fill/*[*/(al, "Hi");
	}
	
    public static <T> void fill(List<? super T> list, T obj) {
        int size = list.size();
        for (int i = 0; i < size; i++)
            list.set(i, obj);
    }
}
