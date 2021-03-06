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


package com.gigaspaces.internal.io;

import com.gigaspaces.internal.utils.pool.IMemoryAwareResourceFactory;
import com.gigaspaces.internal.utils.pool.IMemoryAwareResourcePool;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * @author yael
 * @since 9.0.1
 */
@com.gigaspaces.api.InternalApi
public class ContextClassResolverCompressedMarshObjectConvertor extends CompressedMarshObjectConvertor {

    private static ContextClassResolverCompressedMarshObjectConvertorFactory _factory;

    public ContextClassResolverCompressedMarshObjectConvertor(int level) {
        super(level);
    }

    public ContextClassResolverCompressedMarshObjectConvertor(int level, ISmartLengthBasedCacheCallback cacheCallback) {
        super(level, cacheCallback);
    }

    @Override
    protected ObjectInputStream getObjectInputStream(InputStream is) throws IOException {
        return new ContextClassResolverObjectInputStream(is);
    }

    /**
     * @return factory object
     */
    public static IMemoryAwareResourceFactory<CompressedMarshObjectConvertor> getFactory() {
        if (_factory == null)
            _factory = new ContextClassResolverCompressedMarshObjectConvertorFactory();

        return _factory;
    }


    /**
     * Factory for creating AnnotatedCompressedMarshObjectConvertor
     *
     * @author yael
     */
    private static class ContextClassResolverCompressedMarshObjectConvertorFactory extends CompressedMarshObjectConvertor.CompressedMarshObjectConvertorFactory {

        @Override
        public CompressedMarshObjectConvertor allocate() {
            return new ContextClassResolverCompressedMarshObjectConvertor(9);
        }

        @Override
        public ContextClassResolverCompressedMarshObjectConvertor allocate(
                final IMemoryAwareResourcePool resourcePool) {
            return new ContextClassResolverCompressedMarshObjectConvertor(9, SmartLengthBasedCache.toCacheCallback(resourcePool));
        }
    }
}
