package p;

class Query {
	void doit() {
		Listen listener= null;
		String msg= "m";
		Object xml= new Object();;
		int id= 12;
		listener.handlePoolMessage( new PoolMessageEvent( msg, xml, id ) );
	}
	
	class PoolMessageEvent {
		PoolMessageEvent( String msg, Object xml, int id ) {
			//empty
		}
	}
}

class Listen {
	public void handlePoolMessage(Query.PoolMessageEvent evt) {
		Query q= new Query();
		q.new PoolMessageEvent( null, null, 0 ) ;
	}
}