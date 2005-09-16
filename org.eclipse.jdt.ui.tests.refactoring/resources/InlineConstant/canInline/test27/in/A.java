package p;

class A {
	static final int[] array = {};
	{
		for (int i = array.length; i < array.length; array[i] = i)
			;
	}
}
