package ppp;

class Query {
	void doit() {
		Listen listener= null;
		String msg= "m";
		Object xml= new Object();;
		int id= 12;
		listener.handlePoolMessage( new PoolMessageEvent( msg, xml, id, null ) );
	}
	
	class PoolMessageEvent {
		PoolMessageEvent( String msg, Object xml, int id, Object newParam ) {
			//empty
		}
	}
}

class Listen {
	public void handlePoolMessage(Query.PoolMessageEvent evt) {
		// TODO Auto-generated method stub
	}
}