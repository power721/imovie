/*******************************************************************************
 * Copyright (c) 2006, 2008 Remy Suen, Composent Inc., and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Remy Suen <remy.suen@gmail.com> - initial API and implementation
 ******************************************************************************/
package org.har01d.bittorrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BEncodedDictionary {

    private final List<Entry> list;

    BEncodedDictionary() {
        list = new ArrayList<>();
    }

    public void put(Object key, Object value) {
        list.add(new Entry(key, value));
    }

    public Object get(Object key) {
        for (Entry entry : list) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        synchronized (buffer) {
            buffer.append('d');
            for (Object aList : list) {
                Entry entry = (Entry) aList;
                String key = (String) entry.getKey();
                Object value = entry.getValue();
                buffer.append(key.length()).append(':').append(key);
                if (value instanceof String) {
                    String string = (String) value;
                    buffer.append(string.length()).append(':').append(string);
                } else if (value instanceof Long) {
                    buffer.append('i').append(String.valueOf(value)).append('e');
                } else if (value instanceof List
                    || value instanceof BEncodedDictionary) {
                    buffer.append(value.toString());
                }
            }
            buffer.append('e');
        }
        return buffer.toString();
    }

    private class Entry implements Map.Entry {

        private Object key;

        private Object value;

        private Entry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object value) {
            Object tempValue = this.value;
            this.value = value;
            return tempValue;
        }

    }

}
