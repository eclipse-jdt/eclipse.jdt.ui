package try12_0ut;

public class A_testSwitchExpr1 {
	public static void main(String[] args) {
		String foo(Day day) {
			int x = 0;
			var today = /*]*/extracted(day, x)/*[*/;
			return today;
		}
	}

	protected static String extracted(Day day, int x) {
		return switch(day){
			case SATURDAY, SUNDAY: break "Weekend day";
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
	}
}
