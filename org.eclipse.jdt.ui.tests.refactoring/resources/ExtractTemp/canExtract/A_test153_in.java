package p; // 23, 32, 23, 49
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class A {
	public static void main(String[] args) {
		List<Person> list= new ArrayList<Person>();

		Person p1= new Person("Wang", 19);
		Person p2= new Person("Li", 20);
		Person p3= new Person("Zhang", 21);
		Person p4= new Person("Liu", 18);

		list.add(p1);
		list.add(p2);
		list.add(p3);
		list.add(p4);

		Iterator<Person> iterPeople= list.iterator();

		while (iterPeople.hasNext()) {
			System.out.println(iterPeople.next().getName() + " " + iterPeople.next().getAge());
		}
	}
}

class Person {
	public Person(String name, int age) {
		this.name= name;
		this.age= age;
	}

	String name;

	int age;

	public String getName() {
		return name;
	}

	public int getAge() {
		return age;
	}
}
