package bugs_out;

public class Test_314407_2 {
	Object getImage(String s) {
		return null;
	}
	void foo() {
		Object x=getImage();
		Object y=null;
		Object z=getImage(x);
	}
}
