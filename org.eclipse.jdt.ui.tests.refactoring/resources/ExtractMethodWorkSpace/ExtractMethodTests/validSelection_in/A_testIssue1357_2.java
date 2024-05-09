package validSelection_in;

public class A_testIssue1357_2 {

    public synchronized int calculate() {
        int result;
        /*]*/switch (value) {
        case 1:
            result = value * 2;
            break;
        case 2:
            result = value * 3;
            break;
        default:
            result = value * 4;
            break;
        }/*[*/
        return result;
    }

}