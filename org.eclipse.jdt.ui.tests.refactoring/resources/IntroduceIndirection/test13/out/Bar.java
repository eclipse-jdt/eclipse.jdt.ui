package p;

import p.Foo.Inner.MoreInner;

public class Bar {
	
	{
		MoreInner.bar(new Foo());
	}

}
