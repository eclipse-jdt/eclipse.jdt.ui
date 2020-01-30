package try13_0ut;

public class A_testSwitchExpr1 {
	public String foo(Day day) {
		int x = 0;
		var today = /*]*/extracted(day, x)/*[*/;
		return today;
	}

	protected String extracted(Day day, int x) {
		return switch(day){
			case SATURDAY, SUNDAY: yield "Weekend day";
			case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY: {
		   	 var kind = "Working day";
		    	yield kind;
			}
			default: {
		    	var kind = day.name();
		   	 System.out.println(kind + x);
		   	 throw new IllegalArgumentException("Invalid day: " + kind);
			}
		};
	}
}
