package try13_in;

public class A_testSwitchExpr1 {
	String foo(Day day) {
		int x = 0;
		var today = switch(day){
			/*]*/case SATURDAY, SUNDAY:/*[*/ break "Weekend day";
			case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY: {
				var kind = "Working day";
				break kind;
			}
			default: {
				var kind = day.name();
				System.out.println(kind + x);
				throw new IllegalArgumentException("Invalid day: " + kind);
			}
		};
		return today;
	}
}
