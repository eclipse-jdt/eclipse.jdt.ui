package argument_out;

public class TestLocalReferenceLoop5 {
	public int main() {
    	int z= 10;
    	while (condition()) {
    		z= 10;
    		/*[*/z= z + 1;
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
