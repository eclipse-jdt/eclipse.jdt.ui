//5, 27, 5, 40
package p;

@interface A {
	public static final String DEFAULT_NAME= "Jean-Pierre";

	String name() default DEFAULT_NAME;
}