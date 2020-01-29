package try13_in;

public class A_testSwitchExpr1 {
	public String foo(Day day) {
		int x = 0;
		var today = /*]*/switch(day){
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
		}/*[*/;
		return today;
	}
}
