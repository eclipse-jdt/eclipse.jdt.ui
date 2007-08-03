package p;

import p.subPack.PackEx;

public class PackageReferences extends PackEx{
	private class PrivateInner {
		
	}
	
	protected class ProtectedInner {
		
	}
	ProtectedInner protectedInner= new ProtectedInner();
	PrivateInner privateInner= new PrivateInner();
	OtherPackageProteced packEx= new OtherPackageProteced();
}
