package argument_in;

public class TestLocalReferenceLoop2 {
	public int main() {
    	int z= 10;
    	while (condition()) {
    		/*[*/toInline(z)/*]*/;
    	}
    	return z;
	}
	
    public void toInline(int i) {
    	i= i + 1;
    }
    
    public boolean condition() {
    	return false;
    }
}
