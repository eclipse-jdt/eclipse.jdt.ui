public class BBB {
	public void foo() {
		int i= 0;
		BBB b= new BBB();
		BBB[] bb= new BBB[10];
		if (i == 0 && i == 10 && b instanceof BBB)
			foo();
		while(i++ == 10) {
			foo();
		}
		i--;
		
		while(i++ == 10)
			foo();
		i--;
		
		do {
		} while (i < 10);	
		
		for (int x= 0, o= 0; x < 10; x++, o++);
		 {
			foo();
			int z;//= x;
		}
		
		try {
			foo();
		} catch (Exception e) {
		} finally {
		}
		
		switch (i) {
			case 10:
				foo();
			case 20:
				foo();
			default:
				foo();
		}
	}
	public int g() {
		g();
		return 1;
	}
}

