package p;

public class A {
	class B {
		@Deprecated
		private static final String TAG1= "tag1", TAG_m= null, TAG2= "tag2";
	}

	String X= B.TAG1, Y= B.TAG2;
}
