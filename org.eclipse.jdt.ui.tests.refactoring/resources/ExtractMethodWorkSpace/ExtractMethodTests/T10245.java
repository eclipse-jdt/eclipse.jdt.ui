public class T10245 {
	T10245 f;
	public T10245 a() {
		a().f= a();
		
		/*[*/
		int y= 0;
		a().f= a(); /*[*/
		return null;
	}

}