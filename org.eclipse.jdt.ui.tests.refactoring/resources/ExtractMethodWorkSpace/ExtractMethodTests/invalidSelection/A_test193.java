package invalidSelection;

public class A_test193 {
	private void foo(int i, int j){
		foo(/*]*/10, 10/*[*/);
	}
}

