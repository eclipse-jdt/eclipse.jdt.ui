package p;

import java.util.ArrayList;
import java.util.List;

class A {
	{
		ArrayList arrayList = new ArrayList();
		arrayList.add(12L);
		take("");
		take("", arrayList);
		take("", new ArrayList[] {arrayList});
		take("", arrayList, arrayList);
	}
	void take(String format, List... args) {}
}