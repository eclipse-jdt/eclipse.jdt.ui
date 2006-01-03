package invalidSelection;

public class A_test196 {
	public int foo(int y) {
        int tmp = 0, res = 1;
        while (res < y) {
            /*[*/tmp = res;
            res += 1;/*]*/
        }
        return tmp;
	}
}

