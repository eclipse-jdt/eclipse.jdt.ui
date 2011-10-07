package p;

import java.util.ArrayList;
import java.util.List;

public class Snippet {
	List<Integer> m() {
		ArrayList<Object> arrayList= new ArrayList<>();
		return foo(arrayList);
	}

	private ArrayList<Integer> foo(ArrayList<Object> arrayList) {
		return new ArrayList<>();
	}
}
