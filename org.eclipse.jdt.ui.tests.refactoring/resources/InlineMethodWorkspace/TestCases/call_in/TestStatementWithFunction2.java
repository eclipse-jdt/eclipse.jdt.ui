package call_in;

class TestStatementWithFunction2 {
    public void main(){
       /*]*/foo();/*[*/
    }
    
    public int foo(){
        return bar();
    }
    public int bar() {
    	System.out.println("Bar called");
    	return 10;
    }
}
