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

package com.gigaspaces.internal.cluster.node.impl.view;

import com.gigaspaces.document.DocumentProperties;
import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.internal.metadata.EntryTypeDesc;
import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.query.valuegetter.SpaceEntryPathGetter;
import com.gigaspaces.internal.server.space.metadata.SpaceTypeManager;
import com.gigaspaces.internal.server.space.redolog.storage.bytebuffer.ISwapExternalizable;
import com.gigaspaces.internal.server.storage.EntryDataType;
import com.gigaspaces.internal.server.storage.ICustomTypeDescLoader;
import com.gigaspaces.internal.server.storage.IEntryData;
import com.gigaspaces.internal.transport.IEntryPacket;
import com.gigaspaces.metadata.SpaceMetadataException;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.j_spaces.core.LeaseManager;
import com.j_spaces.core.UnknownTypeException;

import net.jini.core.entry.UnusableEntryException;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

/**
 * @author Niv Ingberg
 * @since 8.0.5
 */
@com.gigaspaces.api.InternalApi
public class EntryPacketServerEntryAdapter implements IEntryData, ICustomTypeDescLoader, Externalizable, ISwapExternalizable {
    private static final long serialVersionUID = -4521887144678238254L;

    private IEntryPacket _entryPacket;

    public EntryPacketServerEntryAdapter() {
    }

    public EntryPacketServerEntryAdapter(IEntryPacket entryPacket) {
        this._entryPacket = entryPacket;
    }

    @Override
    public void loadTypeDescriptor(SpaceTypeManager typeManager) {
        try {
            typeManager.loadServerTypeDesc(_entryPacket);
        } catch (UnusableEntryException e) {
            throw new SpaceMetadataException("Failed to load type descriptor", e);
        } catch (UnknownTypeException e) {
            throw new SpaceMetadataException("Failed to load type descriptor", e);
        }
    }

    @Override
    public SpaceTypeDescriptor getSpaceTypeDescriptor() {
        return _entryPacket.getTypeDescriptor();
    }

    @Override
    public Object getFixedPropertyValue(int position) {
        return _entryPacket.getFieldValue(position);
    }

    @Override
    public Object getPropertyValue(String name) {
        ITypeDesc typeDesc = _entryPacket.getTypeDescriptor();
        int pos = typeDesc.getFixedPropertyPosition(name);
        if (pos != -1)
            return getFixedPropertyValue(pos);

        if (typeDesc.supportsDynamicProperties()) {
            Map<String, Object> dynamicProperties = _entryPacket.getDynamicProperties();
            return dynamicProperties != null ? dynamicProperties.get(name) : null;
        }

        throw new IllegalArgumentException("Unknown property name '" + name + "'");
    }

    @Override
    public Object getPathValue(String path) {
        if (!path.contains("."))
            return getPropertyValue(path);
        return new SpaceEntryPathGetter(path).getValue(this);
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        IOUtils.writeObject(out, _entryPacket);
    }

    @Override
    public void writeToSwap(ObjectOutput out) throws IOException {
        IOUtils.writeNullableSwapExternalizableObject(out, _entryPacket);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        _entryPacket = IOUtils.readObject(in);
    }

    @Override
    public void readFromSwap(ObjectInput in) throws IOException,
            ClassNotFoundException {
        _entryPacket = IOUtils.readNullableSwapExternalizableObject(in);
    }

    @Override
    public int getVersion() {
        return _entryPacket.getVersion();
    }

    @Override
    public long getExpirationTime() {
        return LeaseManager.toAbsoluteTime(_entryPacket.getTTL());
    }

    @Override
    public EntryDataType getEntryDataType() {
        return EntryDataType.FLAT;
    }

    @Override
    public EntryTypeDesc getEntryTypeDesc() {
        return _entryPacket.getTypeDescriptor().getEntryTypeDesc(_entryPacket.getEntryType());
    }

    @Override
    public int getNumOfFixedProperties() {
        return _entryPacket.getTypeDescriptor().getNumOfFixedProperties();
    }

    @Override
    public void setFixedPropertyValue(int index, Object value) {
        _entryPacket.setFieldValue(index, value);
    }

    @Override
    public void setFixedPropertyValues(Object[] values) {
        _entryPacket.setFieldsValues(values);
    }

    @Override
    public Object[] getFixedPropertiesValues() {
        return _entryPacket.getFieldValues();
    }

    @Override
    public Map<String, Object> getDynamicProperties() {
        return _entryPacket.getDynamicProperties();
    }

    @Override
    public long getTimeToLive(boolean useDummyIfRelevant) {
        return _entryPacket.getTTL();
    }

    @Override
    public void setDynamicProperties(Map<String, Object> dynamicProperties) {
        _entryPacket.setDynamicProperties(dynamicProperties);
    }

    @Override
    public void setDynamicPropertyValue(String propertyName, Object value) {
        Map<String, Object> dynamicProperties = _entryPacket.getDynamicProperties();
        if (dynamicProperties == null)
            dynamicProperties = new DocumentProperties();
        dynamicProperties.put(propertyName, value);
        _entryPacket.setDynamicProperties(dynamicProperties);
    }

    @Override
    public void unsetDynamicPropertyValue(String propertyName) {
        Map<String, Object> dynamicProperties = _entryPacket.getDynamicProperties();
        if (dynamicProperties != null)
            dynamicProperties.remove(propertyName);
    }

}
