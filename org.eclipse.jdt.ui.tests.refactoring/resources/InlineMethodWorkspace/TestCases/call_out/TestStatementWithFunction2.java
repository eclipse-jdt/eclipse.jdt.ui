package call_out;

class TestStatementWithFunction2 {
    public void main(){
       bar();
    }
    
    public int foo(){
        return bar();
    }
    public int bar() {
    	System.out.println("Bar called");
    	return 10;
    }
}
