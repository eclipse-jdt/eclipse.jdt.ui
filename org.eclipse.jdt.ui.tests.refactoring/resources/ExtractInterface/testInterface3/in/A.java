package p;
interface A{
	/** method m javadoc comment */
	void m();

	/** field I javadoc comment */
	int I= 9;

	/* method m1 regular comment */
	void m1();

	/* field i1 regular comment */
	int I1= 9;

	// method m2 line comment
	void m2();

	// field i2 line comment
	int I2= 9;

	void m4(); /* method m4 regular comment */

	int I4= 9; /* field i4 regular comment */

	void m5(); // method m5 line comment

	int I5= 9; // field i5 line comment
}