/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.j_spaces.core.exception.internal;

import net.jini.space.InternalSpaceException;

/**
 * an internal space exception thrown from the leaseManager
 *
 * @author asy ronen
 * @since 6.0
 */
@com.gigaspaces.api.InternalApi
public class LeaseInternalSpaceException extends InternalSpaceException {
    private static final long serialVersionUID = -1798322063880620270L;

    public LeaseInternalSpaceException(String str) {
        super(str);
    }

    public LeaseInternalSpaceException(String str, Throwable ex) {
        super(str, ex);
    }
}
