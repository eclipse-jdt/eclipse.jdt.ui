class X{
	void f(){
		int i= 0;
		int j= 1;
		switch (j){
			case 1:
				/*[*/i= 1;/*]*/
				// break;
			default:
				i--;
				i= -1;
				break;
		}
	}
}
