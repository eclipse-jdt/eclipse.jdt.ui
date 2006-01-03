package argument_in;

public class TestLocalReferenceLoop3 {
	public int main() {
    	int z= 10;
    	while (condition()) {
    		z= 10;
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
