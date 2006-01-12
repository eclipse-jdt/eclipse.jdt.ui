package p;

public class Foo {
	
	<E> void setE(E e) {
	}
	
	{
		this.<String>setE("");	// <-- invoke here
	}

}
