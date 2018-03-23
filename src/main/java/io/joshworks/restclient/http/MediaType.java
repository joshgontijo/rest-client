/*
 * Copyright 2017 Josue Gontijo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.joshworks.restclient.http;

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.php
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * MediaType.java
 *
 * Created on March 22, 2007, 2:35 PM
 *
 * Modified on March 13 2017 22:14 PM by Josue Gontijo
 *
 */


import io.joshworks.restclient.http.utils.MimeMappings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * An abstraction for a media type. Instances are immutable.
 *
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7">HTTP/1.1 section 3.7</a>
 */
public class MediaType {

    /**
     * The value of a type or subtype wildcard: "*"
     */
    public static final String MEDIA_TYPE_WILDCARD = "*";
    /**
     * "*&#47;*"
     */
    public final static String WILDCARD = "*/*";
    /**
     * "*&#47;*"
     */
    public final static MediaType WILDCARD_TYPE = new MediaType();
    /**
     * "application/xml"
     */
    public final static String APPLICATION_XML = "application/xml";
    /**
     * "application/xml"
     */
    public final static MediaType APPLICATION_XML_TYPE = new MediaType("application", "xml");
    /**
     * "application/atom+xml"
     */
    public final static String APPLICATION_ATOM_XML = "application/atom+xml";
    /**
     * "application/atom+xml"
     */
    public final static MediaType APPLICATION_ATOM_XML_TYPE = new MediaType("application", "atom+xml");
    /**
     * "application/xhtml+xml"
     */
    public final static String APPLICATION_XHTML_XML = "application/xhtml+xml";

    // Common media type constants
    /**
     * "application/xhtml+xml"
     */
    public final static MediaType APPLICATION_XHTML_XML_TYPE = new MediaType("application", "xhtml+xml");
    /**
     * "application/svg+xml"
     */
    public final static String APPLICATION_SVG_XML = "application/svg+xml";
    /**
     * "application/svg+xml"
     */
    public final static MediaType APPLICATION_SVG_XML_TYPE = new MediaType("application", "svg+xml");
    /**
     * "application/json"
     */
    public final static String APPLICATION_JSON = "application/json";
    /**
     * "application/json"
     */
    public final static MediaType APPLICATION_JSON_TYPE = new MediaType("application", "json");
    /**
     * "application/x-www-form-urlencoded"
     */
    public final static String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    /**
     * "application/x-www-form-urlencoded"
     */
    public final static MediaType APPLICATION_FORM_URLENCODED_TYPE = new MediaType("application", "x-www-form-urlencoded");
    /**
     * "multipart/form-data"
     */
    public final static String MULTIPART_FORM_DATA = "multipart/form-data";
    /**
     * "multipart/form-data"
     */
    public final static MediaType MULTIPART_FORM_DATA_TYPE = new MediaType("multipart", "form-data");
    /**
     * "application/octet-stream"
     */
    public final static String APPLICATION_OCTET_STREAM = "application/octet-stream";
    /**
     * "application/octet-stream"
     */
    public final static MediaType APPLICATION_OCTET_STREAM_TYPE = new MediaType("application", "octet-stream");
    /**
     * "text/plain"
     */
    public final static String TEXT_PLAIN = "text/plain";
    /**
     * "text/plain"
     */
    public final static MediaType TEXT_PLAIN_TYPE = new MediaType("text", "plain");
    /**
     * "text/xml"
     */
    public final static String TEXT_XML = "text/xml";
    /**
     * "text/xml"
     */
    public final static MediaType TEXT_XML_TYPE = new MediaType("text", "xml");
    /**
     * "text/html"
     */
    public final static String TEXT_HTML = "text/html";
    /**
     * "text/html"
     */
    public final static MediaType TEXT_HTML_TYPE = new MediaType("text", "html");
    /**
     * Empty immutable map used for all instances without parameters
     */
    private static final String SUBTYPE_SEPARATOR = "/";
    private static final String PARAMETERS_SEPARATOR = ";";
    private static MimeMappings mimeMappings = MimeMappings.builder().build();
    private String type;
    private String subtype;
    private Map<String, String> parameters = new HashMap<>();

    /**
     * Creates a new instance of MediaType with the supplied type, subtype and
     * pathParameter.
     *
     * @param type       the primary type, null is equivalent to
     *                   {@link #MEDIA_TYPE_WILDCARD}.
     * @param subtype    the subtype, null is equivalent to
     *                   {@link #MEDIA_TYPE_WILDCARD}.
     * @param parameters a map of media type pathParameter, null is the same as an
     *                   empty map.
     */
    public MediaType(String type, String subtype, Map<String, String> parameters) {
        this.type = type == null ? MEDIA_TYPE_WILDCARD : type;
        this.subtype = subtype == null ? MEDIA_TYPE_WILDCARD : subtype;
        if (parameters == null) {
            this.parameters = new HashMap<>();
        } else {
            Map<String, String> map = new TreeMap<>(String::compareToIgnoreCase);
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                map.put(e.getKey().toLowerCase(), e.getValue());
            }
            this.parameters = Collections.unmodifiableMap(map);
        }
    }

    /**
     * Creates a new instance of MediaType with the supplied type and subtype.
     *
     * @param type    the primary type, null is equivalent to
     *                {@link #MEDIA_TYPE_WILDCARD}
     * @param subtype the subtype, null is equivalent to
     *                {@link #MEDIA_TYPE_WILDCARD}
     */
    public MediaType(String type, String subtype) {
        this(type, subtype, new HashMap<>());
    }

    /**
     * Creates a new instance of MediaType, both type and subtype are wildcards.
     * Consider using the constant {@link #WILDCARD_TYPE} instead.
     */
    public MediaType() {
        this(MEDIA_TYPE_WILDCARD, MEDIA_TYPE_WILDCARD);
    }

    /**
     * Creates a new instance of MediaType by parsing the supplied string.
     *
     * @param type the media type string
     * @return the newly created MediaType
     * @throws IllegalArgumentException if the supplied string cannot be parsed
     *                                  or is null
     */
    public static MediaType valueOf(String type) throws IllegalArgumentException {
        if (type == null || type.trim().isEmpty() || type.startsWith(SUBTYPE_SEPARATOR) || type.endsWith(SUBTYPE_SEPARATOR)) {
            throw new IllegalArgumentException("Invalid mime type '" + type + "'");
        }
        String[] splitType = type.split(SUBTYPE_SEPARATOR);
        if (splitType.length == 2) {
            nonEmpty(type, splitType[0]);
            nonEmpty(type, splitType[1]);
            Map<String, String> parameters = new HashMap<>();
            String subType = splitType[1];
            if (splitType[1].contains(PARAMETERS_SEPARATOR)) {
                String[] subTypeWithparameter = splitType[1].split(PARAMETERS_SEPARATOR);
                String paramString = subTypeWithparameter[1].trim();
                subType = subTypeWithparameter[0];
                parameters = parseParameters(paramString);
            }
            return new MediaType(splitType[0].trim(), subType.trim(), parameters);
        }
        if (!type.contains(SUBTYPE_SEPARATOR)) {
            String mimeType = mimeMappings.getMimeType(type);
            if (mimeType != null) {
                return valueOf(mimeType);
            }
        }
        throw new IllegalArgumentException("Invalid mime type '" + type + "'");
    }

    private static void nonEmpty(String original, String val) throws IllegalArgumentException {
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid mime type '" + original + "'");
        }
        if ((val.length() - val.replace("/", "").length()) > 0) {
            throw new IllegalArgumentException("Invalid mime type '" + original + "'");
        }
    }

    private static Map<String, String> parseParameters(String paramString) {
        Map<String, String> parameters = new HashMap<>();
        String[] split = paramString.split(" ");
        for (String kv : split) {
            String[] keyValue = kv.split("=");
            if (keyValue.length == 2) {
                parameters.put(keyValue[0], keyValue[1]);
            }
        }
        return parameters;
    }

    public static MediaType getMimeForFile(String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        String mimeType = mimeMappings.getMimeType(fileExtension);
        if (mimeType == null || mimeType.isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
        try {
            return valueOf(mimeType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
    }

    /**
     * Getter for primary type.
     *
     * @return value of primary type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Checks if the primary type is a wildcard.
     *
     * @return true if the primary type is a wildcard
     */
    public boolean isWildcardType() {
        return this.getType().equals(MEDIA_TYPE_WILDCARD);
    }

    /**
     * Getter for subtype.
     *
     * @return value of subtype.
     */
    public String getSubtype() {
        return this.subtype;
    }

    /**
     * Checks if the subtype is a wildcard
     *
     * @return true if the subtype is a wildcard
     */
    public boolean isWildcardSubtype() {
        return this.getSubtype().equals(MEDIA_TYPE_WILDCARD);
    }

    /**
     * Getter for a read-only pathParameter map. Keys are case-insensitive.
     *
     * @return an immutable map of pathParameter.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Check if this media type is compatible with another media type. E.g.
     * image/* is compatible with image/jpeg, image/png, etc. Media type
     * pathParameter are ignored. The function is commutative.
     *
     * @param other the media type to compare with
     * @return true if the types are compatible, false otherwise.
     */
    public boolean isCompatible(MediaType other) {
        if (other == null)
            return false;
        if (type.equals(MEDIA_TYPE_WILDCARD) || other.type.equals(MEDIA_TYPE_WILDCARD))
            return true;
        else if (type.equalsIgnoreCase(other.type) && (subtype.equals(MEDIA_TYPE_WILDCARD) || other.subtype.equals(MEDIA_TYPE_WILDCARD)))
            return true;
        else
            return this.type.equalsIgnoreCase(other.type)
                    && this.subtype.equalsIgnoreCase(other.subtype);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof MediaType))
            return false;
        MediaType other = (MediaType) obj;
        return (this.type.equalsIgnoreCase(other.type)
                && this.subtype.equalsIgnoreCase(other.subtype)
                && this.parameters.equals(other.parameters));
    }


    @Override
    public int hashCode() {
        return (this.type.toLowerCase() + this.subtype.toLowerCase()).hashCode() + this.parameters.hashCode();
    }


    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(" ");
        for (Map.Entry<String, String> kv : parameters.entrySet()) {
            joiner.add(kv.getKey() + "=" + kv.getValue());
        }
        String params = joiner.toString();
        String type = this.type + SUBTYPE_SEPARATOR + subtype;
        return (params != null && !params.isEmpty()) ? type + "; " + params : type;
    }
}