public class Foo<T> {
	public void bar(T[] array, int index1, int index2) {
		T temp= array[index1];
		array[index1]= array[index2];
		array[index2]= temp;
	}
}
