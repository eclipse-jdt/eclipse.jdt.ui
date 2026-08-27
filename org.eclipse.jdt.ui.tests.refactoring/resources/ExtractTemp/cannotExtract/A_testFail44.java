package p; //10, 39, 10, 48

public class A {
	private Object elementName;
	private Object parent;
	
    public void foo(boolean enabled) {
        int value = 0;

        boolean result = enabled && ((value = 1) > 0);
    }

}