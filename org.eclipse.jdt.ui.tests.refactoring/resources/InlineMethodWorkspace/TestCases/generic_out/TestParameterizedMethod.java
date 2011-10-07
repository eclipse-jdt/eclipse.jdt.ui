package generic_out;

import java.util.ArrayList;
import java.util.List;

public class TestParameterizedMethod {
	void foo(ArrayList<String> al) {
		/*]*/int size = al.size();
		for (int i = 0; i < size; i++)
		    al.set(i, "Hi");
	}
	
    public static <T> void fill(List<? super T> list, T obj) {
        int size = list.size();
        for (int i = 0; i < size; i++)
            list.set(i, obj);
    }
}
