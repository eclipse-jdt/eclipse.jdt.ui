package argument_in;

public class TestLocalReferenceLoop1 {
	public void main() {
    	int z= 10;
    	while (condition()) {
    		/*[*/toInline(z)/*]*/;
    	}
	}
	
    public void toInline(int i) {
    	i= i + 1;
    }
    
    public boolean condition() {
    	return false;
    }
}
