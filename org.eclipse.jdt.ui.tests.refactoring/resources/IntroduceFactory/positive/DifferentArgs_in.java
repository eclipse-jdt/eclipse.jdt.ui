package p;

import java.lang.StringBuffer;

public class DifferentArgs_in {
	public void foo(String[] args)
	{
		StringBuffer	buf = /*[*/new StringBuffer(16)/*]*/;

		buf.append("Args:");
		for(int i=0; i < args.length; i++)
			buf.append(" '")
			   .append(args[i])
			   .append("'");
		System.out.println(buf.toString());
	}
	public void bar(String[] args)
	{
		StringBuffer	buf = new StringBuffer(24);

		buf.append("Args:");
		for(int i=0; i < args.length; i++)
			buf.append(" '")
			   .append(args[i])
			   .append("'");
		System.out.println(buf.toString());
	}
}
