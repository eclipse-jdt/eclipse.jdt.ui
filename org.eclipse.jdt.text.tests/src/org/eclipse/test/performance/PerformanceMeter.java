/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.test.performance;

import org.eclipse.jdt.text.tests.performance.data.MeteringSession;

public abstract class PerformanceMeter {

	public abstract void start();

	public abstract void stop();

	public abstract void commit();

	public abstract MeteringSession getSessionData();
	
	public abstract String getScenarioName();

}
