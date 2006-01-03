package argument_out;

public class TestLocalReferenceLoop1 {
	public void main() {
    	int z= 10;
    	while (condition()) {
    		/*[*/int i = z;
			i= i + 1;
    	}
	}
	
    public void toInline(int i) {
    	i= i + 1;
    }
    
    public boolean condition() {
    	return false;
    }
}
