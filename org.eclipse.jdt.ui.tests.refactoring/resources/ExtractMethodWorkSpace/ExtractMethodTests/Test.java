import java.util.ArrayList;import java.util.Iterator;import java.util.List;public class Test {
	public class Inner {	}		public void pre() {		foo();	}
	public void foo() {		foo();
	}
	
	public void post() {		foo();
	}
}
