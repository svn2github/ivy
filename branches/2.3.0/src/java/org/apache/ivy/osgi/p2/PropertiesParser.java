/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.osgi.p2;

import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.osgi.util.DelegetingHandler;
import org.xml.sax.Attributes;

public class PropertiesParser {

    static class PropertiesHandler extends DelegetingHandler {

        private static final String PROPERTIES = "properties";

        private static final String SIZE = "size";

        Map properties;

        public PropertiesHandler() {
            super(PROPERTIES);
            addChild(new PropertyHandler(), new ChildElementHandler() {
                public void childHanlded(DelegetingHandler child) {
                    properties.put(((PropertyHandler) child).name, ((PropertyHandler) child).value);
                }
            });
        }

        protected void handleAttributes(Attributes atts) {
            int size = Integer.parseInt(atts.getValue(SIZE));
            properties = new HashMap(size);
        }

    }

    static class PropertyHandler extends DelegetingHandler {

        private static final String PROPERTY = "property";

        private static final String NAME = "name";

        private static final String VALUE = "value";

        String name;

        String value;

        public PropertyHandler() {
            super(PROPERTY);
        }

        protected void handleAttributes(Attributes atts) {
            name = atts.getValue(NAME);
            value = atts.getValue(VALUE);
        }

    }

}
