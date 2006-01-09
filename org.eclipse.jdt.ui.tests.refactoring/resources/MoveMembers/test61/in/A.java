package p;

public class A {
	
	private static String NAME= "N";

    private static String getNAME() { 
        return NAME;
    }
    
    private static void setNAME(String name) {
    	NAME= name;
    }
    
    private static void foo() {
    	System.out.println(NAME);
    }

}
