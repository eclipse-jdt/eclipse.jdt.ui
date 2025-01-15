package validSelection_in;

import java.util.ArrayList;

public class A_testIssue1913_1 {

	public void foo() {
		ArrayList<Integer> list= new ArrayList<Integer>();
		for (Integer var : list) {}
		int a= 0;
		for (int c= 0; c < 10; c++) {
			/*]*/list.add(a++);/*[*/
		}
		System.out.println(list.toString());
	}

}
