//selection: 25, 33, 25, 41
//name: string -> name
package simple;

/**
 * @see Constructor1#create(int num)
 * @see #create(int)
 * 
 * @see Constructor1#Constructor1(String)
 * @see #Constructor1(String name)
 */
public class Constructor1 {
	/**
	 * @param name the name
	 */
	private Constructor1(String name) {
		System.out.println(name);
	}
	/**
	 * Creator.
	 * @param num the count
	 * @return a Constructor1
	 */
	public Constructor1 create(int num) {
		return new Constructor1("secret" + " #" + num);
	}
}