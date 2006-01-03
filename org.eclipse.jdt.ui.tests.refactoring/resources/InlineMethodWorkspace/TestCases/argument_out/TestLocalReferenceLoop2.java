package argument_out;

public class TestLocalReferenceLoop2 {
	public int main() {
    	int z= 10;
    	while (condition()) {
    		/*[*/int i = z;
			i= i + 1;
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
