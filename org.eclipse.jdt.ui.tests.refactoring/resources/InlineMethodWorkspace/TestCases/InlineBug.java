import java.io.Writer;

public class InlineBug
{
	public void caller(Writer out) throws Exception
	{
		out.write("start caller");
		inlineMe(out);
		out.write("end caller");
	}

	public void inlineMe(Writer out) throws Exception
	{
		out.write("start render");
		subroutine(true, out);
		out.write("end render");
	}

	void subroutine(boolean isSelected, Writer out) throws Exception
	{
		if (isSelected)
		{
			out.write("selected");
		}
	}
}
