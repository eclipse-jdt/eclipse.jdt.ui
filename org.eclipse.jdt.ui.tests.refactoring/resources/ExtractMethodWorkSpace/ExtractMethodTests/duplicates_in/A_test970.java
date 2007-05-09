package duplicates_in;

public class A_test970 {
	static class Transaction {
		public void start(String string) {}
		public void run(Action action) {}
		public void run(String string) {}
		public void end() {}
	}
	static class Action {
		public Action(String string) {}
	}

	public void a3()
	{
		Transaction t = new Transaction();
		Action action = new Action(" A.a3");
		String string = "A.a3";
		/*[*/
		t.start(string);
		t.run(action);
		t.end();
		/*]*/
	}

	public void a4()
	{
		Transaction t = new Transaction();
		String t_name = "A.a4";

		Action action = new Action(t_name);

		t.start(t_name);
		t.run(t_name);
		t.end();
	}

	public void a6()
	{
		Transaction t = new Transaction();
		String t_name = "A.a6";

		Action action = new Action(t_name);
		// DUPLICATE!!!
		t.start(t_name);
		t.run(action);
		t.end();
	}
}