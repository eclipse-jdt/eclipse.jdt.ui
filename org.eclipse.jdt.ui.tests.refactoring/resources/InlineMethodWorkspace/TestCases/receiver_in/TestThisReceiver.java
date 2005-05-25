package receiver_in;

public class TestThisReceiver {
    private Object data;

    private void use(Object data) {
    	this.toInline(data);
    }

	private void toInline(Object data) {
		this.data= data;
	}
}
