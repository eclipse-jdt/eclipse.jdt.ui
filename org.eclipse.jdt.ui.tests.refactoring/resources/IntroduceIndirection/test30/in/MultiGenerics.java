package p;

public class MultiGenerics<E> {

	{
		MultiGenerics<Integer> intVec = new MultiGenerics<Integer>();
		intVec.addElement(42);
		intVec.addElement(Integer.valueOf(42));
		MultiGenerics<String> sVec = new MultiGenerics<String>();
		sVec.addElement("X");
	}

	private void addElement(E e) {
	}

}
