/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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