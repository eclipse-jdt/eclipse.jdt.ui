package p;

import java.util.ArrayList;
import java.util.List;

interface J {
	public List getList(); // cannot infer a return type (except List<?>)
}

class D implements J {
	public List getList() {
		List<Double> dList= new ArrayList<Double>();
		dList.add(new Double(1.2d));
		Double d= dList.get(0);
		return dList;
	}
}

class S implements J {
	public List getList() {
		List<String> sList= new ArrayList<String>();
		sList.add("String");
		String s= sList.get(0);
		return sList;
	}
}