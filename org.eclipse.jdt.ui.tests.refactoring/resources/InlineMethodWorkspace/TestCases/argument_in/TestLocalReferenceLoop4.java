package argument_in;

public class TestLocalReferenceLoop4 {
	public int main() {
    	int z= 10;
    	while (condition()) {
    		/*[*/toInline(z)/*]*/;
    	}
		z= 10;
    	return z;
	}
	
    public void toInline(int i) {
    	i= i + 1;
    }
    
    public boolean condition() {
    	return false;
    }
}
