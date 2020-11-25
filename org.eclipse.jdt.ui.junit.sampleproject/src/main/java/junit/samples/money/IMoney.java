package junit.samples.money;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

/**
 * The common interface for simple Monies and MoneyBags
 *
 */
public interface IMoney {
	/**
	 * Adds a money to this money.
	 */
	IMoney add(IMoney m);

	/**
	 * Adds a simple Money to this money. This is a helper method for implementing
	 * double dispatch
	 */
	IMoney addMoney(Money m);

	/**
	 * Adds a MoneyBag to this money. This is a helper method for implementing
	 * double dispatch
	 */
	IMoney addMoneyBag(MoneyBag s);

	/**
	 * Tests whether this money is zero
	 */
	boolean isZero();

	/**
	 * Multiplies a money by the given factor.
	 */
	IMoney multiply(int factor);

	/**
	 * Negates this money.
	 */
	IMoney negate();

	/**
	 * Subtracts a money from this money.
	 */
	IMoney subtract(IMoney m);

	/**
	 * Append this to a MoneyBag m.
	 */
	void appendTo(MoneyBag m);
}
