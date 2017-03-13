/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * The software source code is proprietary and confidential information of GigaSpaces.
 * You may use the software source code solely under the terms and limitations of
 * The license agreement granted to you by GigaSpaces.
 *******************************************************************************/
package com.gigaspaces.internal.sync.hybrid;

import net.jini.core.transaction.UnknownTransactionException;

/**
 * @author Boris
 * @since 11.0.1
 * Exception which is thrown when space failed to prepare a transaction on sync hybrid mode.
 */
public class SyncHybridTransactionException extends UnknownTransactionException {

    private static final long serialVersionUID = 1L;

    public SyncHybridTransactionException(String desc, Throwable t){
        super(desc, t);
    }
}
