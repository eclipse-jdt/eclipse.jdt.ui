package p;

class Outer {
	enum A {
		GREEN, DARK_GREEN, BLACK;
		A getNext() {
			switch (this) {
				case GREEN : return DARK_GREEN;
				case DARK_GREEN : return BLACK;
				case BLACK : return GREEN;
				default : return null;
			}
		}
	}
	{
		A a= A.GREEN.getNext();
	}
}