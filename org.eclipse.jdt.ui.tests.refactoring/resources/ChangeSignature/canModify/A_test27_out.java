package p;

class Query {
	void doit() {
		Listen listener= null;
		String msg= "m";
		Object xml= new Object();;
		int id= 12;
		listener.handlePoolMessage( new PoolMessageEvent( msg, xml, id, null ) );
	}
	
	class PoolMessageEvent {
		/**
		 * @deprecated Use {@link #PoolMessageEvent(String,Object,int,Object)} instead
		 */
		PoolMessageEvent( String msg, Object xml, int id ) {
			this(msg, xml, id, null);
		}

		PoolMessageEvent( String msg, Object xml, int id, Object newParam ) {
			//empty
		}
	}
}

class Listen {
	public void handlePoolMessage(Query.PoolMessageEvent evt) {
		Query q= new Query();
		q.new PoolMessageEvent( null, null, 0, null ) ;
	}
}