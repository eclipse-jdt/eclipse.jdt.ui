package fieldInitializer_in;

public class A_test1252 {
	String fS= "foo";
	void m() {
		new Thread() {
			String fSub= /*]*/fS.substring(1)/*[*/;
			public void run() {
				System.out.println(fS.substring(1));
			};
		}.start();
	}
}
