package p2;

public class A {

	private static class Inner {

		static int a = 0;

		private static class MoreInner {

			{
				a++;
			}

		}
	}

}
