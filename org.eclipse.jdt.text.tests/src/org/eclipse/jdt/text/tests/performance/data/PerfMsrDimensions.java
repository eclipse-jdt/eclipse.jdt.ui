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
package org.eclipse.jdt.text.tests.performance.data;

import java.util.HashMap;
import java.util.Map;


/**
 * @since 3.1
 */
public class PerfMsrDimensions {
	public static final Dimension WORKING_SET= new Dimension(DimensionMessages.getString("PerfMsrDimensions.WorkingSet.name"), Unit.BYTE); //$NON-NLS-1$
	public static final Dimension USER_TIME= new Dimension(DimensionMessages.getString("PerfMsrDimensions.UserTime.name"), Unit.SECOND, 1000); //$NON-NLS-1$
	public static final Dimension KERNEL_TIME= new Dimension(DimensionMessages.getString("PerfMsrDimensions.KernelTime.name"), Unit.SECOND, 1000); //$NON-NLS-1$
	public static final Dimension CPU_TIME= new Dimension(DimensionMessages.getString("PerfMsrDimensions.CPUTime.name"), Unit.SECOND, 1000); //$NON-NLS-1$
	public static final Dimension HARD_PAGE_FAULTS= new Dimension(DimensionMessages.getString("PerfMsrDimensions.HardPageFaults.name"), Unit.CARDINAL); //$NON-NLS-1$
	public static final Dimension SOFT_PAGE_FAULTS= new Dimension(DimensionMessages.getString("PerfMsrDimensions.SoftPageFaults.name"), Unit.CARDINAL); //$NON-NLS-1$
	public static final Dimension TEXT_SIZE= new Dimension(DimensionMessages.getString("PerfMsrDimensions.TextSize.name"), Unit.BYTE); //$NON-NLS-1$
	public static final Dimension DATA_SIZE= new Dimension(DimensionMessages.getString("PerfMsrDimensions.DataSize.name"), Unit.BYTE); //$NON-NLS-1$
	public static final Dimension LIBRARY_SIZE= new Dimension(DimensionMessages.getString("PerfMsrDimensions.LibrarySize.name"), Unit.BYTE); //$NON-NLS-1$

	private static final Map fgRegisteredDimensions;
	private static final Map fgIdMap;

	static {
		fgRegisteredDimensions= new HashMap();
		fgRegisteredDimensions.put("4", WORKING_SET); //$NON-NLS-1$
		fgRegisteredDimensions.put("10", USER_TIME); //$NON-NLS-1$
		fgRegisteredDimensions.put("11", KERNEL_TIME); //$NON-NLS-1$
		fgRegisteredDimensions.put("20", CPU_TIME); //$NON-NLS-1$
		fgRegisteredDimensions.put("42", HARD_PAGE_FAULTS); //$NON-NLS-1$
		fgRegisteredDimensions.put("43", SOFT_PAGE_FAULTS); //$NON-NLS-1$
		fgRegisteredDimensions.put("44", TEXT_SIZE); //$NON-NLS-1$
		fgRegisteredDimensions.put("45", DATA_SIZE); //$NON-NLS-1$
		fgRegisteredDimensions.put("46", LIBRARY_SIZE); //$NON-NLS-1$
		
		fgIdMap= new HashMap();
		fgIdMap.put(WORKING_SET, "4"); //$NON-NLS-1$
		fgIdMap.put(USER_TIME, "10"); //$NON-NLS-1$
		fgIdMap.put(KERNEL_TIME, "11"); //$NON-NLS-1$
		fgIdMap.put(CPU_TIME, "20"); //$NON-NLS-1$
		fgIdMap.put(HARD_PAGE_FAULTS, "42"); //$NON-NLS-1$
		fgIdMap.put(SOFT_PAGE_FAULTS, "43"); //$NON-NLS-1$
		fgIdMap.put(TEXT_SIZE, "44"); //$NON-NLS-1$
		fgIdMap.put(DATA_SIZE, "45"); //$NON-NLS-1$
		fgIdMap.put(LIBRARY_SIZE, "46"); //$NON-NLS-1$
	}
	
	public static Dimension getDimension(String name) {
		return (Dimension) fgRegisteredDimensions.get(name);
	}
	
	public static String getPerfMsrId(Dimension dimension) {
		return (String) fgIdMap.get(dimension);
	}
	
}
