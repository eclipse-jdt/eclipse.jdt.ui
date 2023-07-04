package p; // 27, 19, 27, 31
import java.util.HashMap;
public class A {
	private HashMap<String,Person> map = new HashMap<String, Person>();
	public A() {
		Person myclass1= new Person("1",1);
		Person myclass2= new Person("2",2);
		Person myclass3= new Person("3",3);
		map.put("1", myclass1);
		map.put("2", myclass2);
		map.put("3", myclass3);
	}
	public void method1() {	
		String key= "1";
		Person mapKey= map.get(key);
		String a= element.getName();
		System.out.println(a);
	}
	public void method2() {	
		String key= "2";
		Person element= map.get(key);
		String a= element.getName();
		System.out.println(a);
	}
	public void method3() {	
		String key= "3";
		String a= map.get(key).getName();
		System.out.println(a);
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
