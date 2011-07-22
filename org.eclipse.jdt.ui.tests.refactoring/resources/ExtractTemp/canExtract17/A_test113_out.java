package p;

import java.util.ArrayList;
import java.util.List;

public class Snippet {
	List<Integer> m() {
		return foo(new ArrayList<>());
	}

	private ArrayList<Integer> foo(ArrayList<Object> arrayList) {
		ArrayList<Integer> arrayList2= new ArrayList<>();
		return arrayList2;
	}
}
