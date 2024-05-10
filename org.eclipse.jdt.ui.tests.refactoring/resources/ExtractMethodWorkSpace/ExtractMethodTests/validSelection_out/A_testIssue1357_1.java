package validSelection_out;

public class A_testIssue1357_1 {

    public synchronized int calculate() {
        return extracted();/*[*/
    }

	protected int extracted() {
		int result;
		switch (value) {
        case 1:
            result = value * 2;
            break;
        case 2:
            result = value * 3;
            break;
        default:
            result = value * 4;
            break;
        }
        return result;
	}

}