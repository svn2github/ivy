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
package org.apache.ivy.osgi.obr.xml;


import java.io.OutputStream;
import java.text.ParseException;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.osgi.core.BundleCapability;
import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.core.ExportPackage;
import org.apache.ivy.osgi.core.ManifestParser;
import org.apache.ivy.osgi.repo.ManifestAndLocation;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.osgi.util.VersionRange;
import org.apache.ivy.util.Message;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.CAPABILITY;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.CAPABILITY_NAME;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.CAPABILITY_PROPERTY;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.CAPABILITY_PROPERTY_NAME;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.CAPABILITY_PROPERTY_TYPE;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.CAPABILITY_PROPERTY_VALUE;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.REPOSITORY;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.REQUIRE;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.REQUIRE_FILTER;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.REQUIRE_NAME;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.REQUIRE_OPTIONAL;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.RESOURCE;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.RESOURCE_SYMBOLIC_NAME;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.RESOURCE_URI;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.RESOURCE_VERSION;
import static org.apache.ivy.osgi.obr.xml.OBRXMLParser.TRUE;

public class OBRXMLWriter {

    public static ContentHandler newHandler(OutputStream out, String encoding, boolean indent)
            throws TransformerConfigurationException {
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler hd = tf.newTransformerHandler();
        Transformer serializer = tf.newTransformer();
        StreamResult stream = new StreamResult(out);
        serializer.setOutputProperty(OutputKeys.ENCODING, encoding);
        serializer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
        hd.setResult(stream);
        return hd;
    }

    public static void writeManifests(Iterable<ManifestAndLocation> it, ContentHandler handler, boolean quiet)
            throws SAXException {
        int level = quiet ? Message.MSG_DEBUG : Message.MSG_WARN;
        handler.startDocument();
        AttributesImpl atts = new AttributesImpl();
        handler.startElement("", REPOSITORY, REPOSITORY, atts);
        int nbOk = 0;
        int nbRejected = 0;
        for (ManifestAndLocation manifestAndLocation : it) {
            BundleInfo bundleInfo;
            try {
                bundleInfo = ManifestParser.parseManifest(manifestAndLocation.getManifest());
                bundleInfo.setUri(manifestAndLocation.getLocation());
                nbOk++;
            } catch (ParseException e) {
                nbRejected++;
                IvyContext.getContext().getMessageLogger().log(
                        "Rejected " + manifestAndLocation.getLocation() + ": " + e.getMessage(), level);
                continue;
            }
            saxBundleInfo(bundleInfo, handler);
        }
        handler.endElement("", REPOSITORY, REPOSITORY);
        handler.endDocument();
        Message.info(nbOk + " bundle" + (nbOk > 1 ? "s" : "") + " added, " + nbRejected + " bundle"
                + (nbRejected > 1 ? "s" : "") + " rejected.");
    }

    public static void writeBundles(Iterable<BundleInfo> it, ContentHandler handler) throws SAXException {
        handler.startDocument();
        AttributesImpl atts = new AttributesImpl();
        handler.startElement("", REPOSITORY, REPOSITORY, atts);
        for (BundleInfo bundleInfo : it) {
            saxBundleInfo(bundleInfo, handler);
        }
        handler.endElement("", REPOSITORY, REPOSITORY);
        handler.endDocument();
    }

    private static void saxBundleInfo(BundleInfo bundleInfo, ContentHandler handler)
            throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        addAttr(atts, RESOURCE_SYMBOLIC_NAME, bundleInfo.getSymbolicName());
        addAttr(atts, RESOURCE_VERSION, bundleInfo.getRawVersion());
        if (bundleInfo.getUri() != null) {
            addAttr(atts, RESOURCE_URI, bundleInfo.getUri());
        }
        handler.startElement("", RESOURCE, RESOURCE, atts);
        for (BundleCapability capability : bundleInfo.getCapabilities()) {
            saxCapability(capability, handler);
        }
        for (BundleRequirement requirement : bundleInfo.getRequirements()) {
            saxRequirement(requirement, handler);
        }
        handler.endElement("", RESOURCE, RESOURCE);
        handler.characters("\n".toCharArray(), 0, 1);
    }

    private static void saxCapability(BundleCapability capability, ContentHandler handler) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        String type = capability.getType();
        addAttr(atts, CAPABILITY_NAME, type);
        handler.startElement("", CAPABILITY, CAPABILITY, atts);
        if (type.equals(BundleInfo.BUNDLE_TYPE)) {
            // nothing to do, already handled with the resource tag
        } else if (type.equals(BundleInfo.PACKAGE_TYPE)) {
            saxCapabilityProperty("package", capability.getName(), handler);
            Version v = capability.getRawVersion();
            if (v != null) {
                saxCapabilityProperty("version", v.toString(), handler);
            }
            Set<String> uses = ((ExportPackage) capability).getUses();
            if (uses != null && !uses.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (String use : uses) {
                    if (builder.length() != 0) {
                        builder.append(',');
                    }
                    builder.append(use);
                }
                saxCapabilityProperty("uses", builder.toString(), handler);
            }
        } else if (type.equals(BundleInfo.SERVICE_TYPE)) {
            saxCapabilityProperty("service", capability.getName(), handler);
            Version v = capability.getRawVersion();
            if (v != null) {
                saxCapabilityProperty("version", v.toString(), handler);
            }
        } else {
            // oups
        }
        handler.endElement("", CAPABILITY, CAPABILITY);
        handler.characters("\n".toCharArray(), 0, 1);
    }

    private static void saxCapabilityProperty(String n, String v, ContentHandler handler) throws SAXException {
        saxCapabilityProperty(n, null, v, handler);
    }

    private static void saxCapabilityProperty(String n, String t, String v, ContentHandler handler) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        addAttr(atts, CAPABILITY_PROPERTY_NAME, n);
        if (t != null) {
            addAttr(atts, CAPABILITY_PROPERTY_TYPE, t);
        }
        addAttr(atts, CAPABILITY_PROPERTY_VALUE, v);
        handler.startElement("", CAPABILITY_PROPERTY, CAPABILITY_PROPERTY, atts);
        handler.endElement("", CAPABILITY_PROPERTY, CAPABILITY_PROPERTY);
        handler.characters("\n".toCharArray(), 0, 1);
    }

    private static void saxRequirement(BundleRequirement requirement, ContentHandler handler) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        addAttr(atts, REQUIRE_NAME, requirement.getType());
        if ("optional".equals(requirement.getResolution())) {
            addAttr(atts, REQUIRE_OPTIONAL, TRUE);
        }
        addAttr(atts, REQUIRE_FILTER, buildFilter(requirement));
        handler.startElement("", REQUIRE, REQUIRE, atts);
        handler.endElement("", REQUIRE, REQUIRE);
        handler.characters("\n".toCharArray(), 0, 1);
    }

    private static String buildFilter(BundleRequirement requirement) {
        StringBuilder filter = new StringBuilder();
        VersionRange v = requirement.getVersion();
        if (v != null) {
            appendVersion(filter, v);
        }
        filter.append('(');
        filter.append(requirement.getType());
        filter.append("=");
        filter.append(requirement.getName());
        filter.append(')');
        if (v != null) {
            filter.append(')');
        }
        return filter.toString();
    }

    private static void appendVersion(StringBuilder filter, VersionRange v) {
        filter.append("(&");
        Version start = v.getStartVersion();
        if (start != null) {
            filter.append("(version>");
            if (!v.isStartExclusive()) {
                filter.append('=');
            }
            filter.append(start.toString());
            filter.append(')');
        }
        Version end = v.getEndVersion();
        if (end != null) {
            filter.append("(version<");
            if (!v.isEndExclusive()) {
                filter.append('=');
            }
            filter.append(end.toString());
            filter.append(')');
        }
    }

    private static void addAttr(AttributesImpl atts, String name, String value) {
        if (value != null) {
            atts.addAttribute("", name, name, "CDATA", value);
        }
    }

    private static void addAttr(AttributesImpl atts, String name, Object value) {
        if (value != null) {
            atts.addAttribute("", name, name, "CDATA", value.toString());
        }
    }

}
