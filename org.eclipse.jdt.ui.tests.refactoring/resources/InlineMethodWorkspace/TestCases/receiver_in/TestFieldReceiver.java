package receiver_in;

public class TestFieldReceiver {
    public Object object;
    public Object getObject() {
        return object;
    }
}
class Client2 {
    void test() {
    	TestFieldReceiver r= new TestFieldReceiver();
        Object o= /*]*/r.getObject()/*[*/;
    }
}
