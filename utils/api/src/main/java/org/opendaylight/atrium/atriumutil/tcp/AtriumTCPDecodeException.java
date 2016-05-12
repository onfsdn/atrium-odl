/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.atrium.atriumutil.tcp;

public class AtriumTCPDecodeException extends Exception {
	// The parent exception that triggered this snoop exception.
	Exception parentException;

	/**
	 * Creates a new snoop exception with the provided message.
	 *
	 * @param message
	 *            The message explaining the reason for this exception.
	 */
	public AtriumTCPDecodeException(String message) {
		super(message);

		this.parentException = null;
	}

	/**
	 * Creates a new snoop exception with the provided message.
	 *
	 * @param message
	 *            The message explaining the reason for this exception.
	 * @param parentException
	 *            The parent exception that triggered this snoop exception.
	 */
	public AtriumTCPDecodeException(String message, Exception parentException) {
		super(message);

		this.parentException = parentException;
	}

	/**
	 * Retrieves the parent exception that triggered this snoop exception.
	 *
	 * @return The parent exception that triggered this snoop exception, or
	 *         <CODE>null</CODE> if no parent exception is available.
	 */
	public Exception getParentException() {
		return parentException;
	}
}
