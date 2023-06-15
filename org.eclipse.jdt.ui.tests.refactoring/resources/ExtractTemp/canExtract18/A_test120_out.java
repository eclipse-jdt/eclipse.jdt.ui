package p; //13, 25, 13, 34

import java.util.List;
import java.util.ArrayList;

public class A {
	private List<String> getList() {
		return new ArrayList<>();
	}

	private int foo() {
		int sum = 0;
		List<String> list= getList();
		for (String s : list) {
			sum += s.length();
		}
		return sum;
	}
}
