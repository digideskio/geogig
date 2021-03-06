/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Victor Olaya (Boundless) - initial implementation
 */
package org.locationtech.geogig.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Enumeration of types supported as {@link RevFeature} attribute values.
 * <p>
 * The constants in this enum define the object types attribute values for GeoGig {@link RevFeature}
 * instances may assume.
 * <p>
 * The {@link FieldType} bound to a given attribute instance can be obtained through
 * {@link #forValue(Object)}, and the one bound to a given attribute class through
 * {@link #forBinding(Class)}.
 * <p>
 * {@code null} values will be bound to the {@link #NULL} constant, as well as calls to
 * {@link #forBinding(Class)} with {@link Void Void.class} as argument; {@link Integer} values to
 * the {@link #INTEGER} constant, and so on.
 * <p>
 * When the attribute value or class is not bound to a constant in this enum, the {@link #UNKNOWN}
 * constant will be returned by both {@link #forValue(Object)} and {@link #forBinding(Class)}, which
 * should be treated by the calling code as an exceptional condition and act accordingly.
 */
public enum FieldType {
    NULL(0x00, Void.class), //
    BOOLEAN(0x01, Boolean.class), //
    BYTE(0x02, Byte.class), //
    SHORT(0x03, Short.class), //
    INTEGER(0x04, Integer.class), //
    LONG(0x05, Long.class), //
    FLOAT(0x06, Float.class), //
    DOUBLE(0x07, Double.class), //
    STRING(0x08, String.class), //
    BOOLEAN_ARRAY(0x09, boolean[].class, (v) -> ((boolean[]) v).clone()), //
    BYTE_ARRAY(0x0A, byte[].class, (v) -> ((byte[]) v).clone()), //
    SHORT_ARRAY(0x0B, short[].class, (v) -> ((short[]) v).clone()), //
    INTEGER_ARRAY(0x0C, int[].class, (v) -> ((int[]) v).clone()), //
    LONG_ARRAY(0x0D, long[].class, (v) -> ((long[]) v).clone()), //
    FLOAT_ARRAY(0x0E, float[].class, (v) -> ((float[]) v).clone()), //
    DOUBLE_ARRAY(0x0F, double[].class, (v) -> ((double[]) v).clone()), //
    STRING_ARRAY(0x10, String[].class, (v) -> ((String[]) v).clone()), //
    POINT(0x11, Point.class, (v) -> ((Geometry) v).clone()), //
    LINESTRING(0x12, LineString.class, (v) -> ((Geometry) v).clone()), //
    POLYGON(0x13, Polygon.class, (v) -> ((Geometry) v).clone()), //
    MULTIPOINT(0x14, MultiPoint.class, (v) -> ((Geometry) v).clone()), //
    MULTILINESTRING(0x15, MultiLineString.class, (v) -> ((Geometry) v).clone()), //
    MULTIPOLYGON(0x16, MultiPolygon.class, (v) -> ((Geometry) v).clone()), //
    GEOMETRYCOLLECTION(0x17, GeometryCollection.class, (v) -> ((Geometry) v).clone()), //
    GEOMETRY(0x18, Geometry.class, (v) -> ((Geometry) v).clone()), //
    UUID(0x19, java.util.UUID.class), //
    BIG_INTEGER(0x1A, BigInteger.class), //
    BIG_DECIMAL(0x1B, BigDecimal.class), //
    DATETIME(0x1C, java.util.Date.class), //
    DATE(0x1D, java.sql.Date.class), //
    TIME(0x1E, java.sql.Time.class), //
    TIMESTAMP(0x1F, java.sql.Timestamp.class), //
    @SuppressWarnings({ "unchecked", "rawtypes" }) //
    MAP(0x20, java.util.Map.class, (v) -> new HashMap<>((Map) v)), //
    CHAR(0x21, Character.class), //
    CHAR_ARRAY(0x22, char[].class), //
    UNKNOWN(-1, null);

    private final byte tagValue;

    private final Class<?> binding;

    /**
     * A function that creates a "safe copy" for an attribute value of the type denoted by this enum
     * member instance.
     */
    private final Function<Object, Object> safeCopyBuilder;

    private static final Map<Class<?>, FieldType> BINDING_MAPPING = Maps.newHashMap();
    static {
        for (FieldType t : FieldType.values()) {
            BINDING_MAPPING.put(t.getBinding(), t);
        }
    }

    private FieldType(int tagValue, Class<?> binding) {
        this(tagValue, binding, (val) -> val);
    }

    private FieldType(int tagValue, Class<?> binding,
            Function<Object, Object> immutableCopyBuilder) {
        this.tagValue = (byte) tagValue;
        this.binding = binding;
        this.safeCopyBuilder = immutableCopyBuilder;
    }

    public Class<?> getBinding() {
        return binding;
    }

    /**
     * A unique identifier for this enum member, in order not to rely in {@link #ordinal()}, that
     * can be used, for example, by serializers to identify the kind of value that's to be encoded.
     */
    public byte getTag() {
        return tagValue;
    }

    /**
     * Obtain a {@code FieldType} constant by it's {@link #getTag() tag}
     */
    public static FieldType valueOf(final int tagValue) {
        if (tagValue == -1) {
            return UNKNOWN;
        }
        // NOTE: we're using the tagValue as the ordinal index because they match, the moment they
        // don't we need to reimplement this method accordingly.
        return values()[tagValue];
    }

    /**
     * @return the {@code FieldType} corresponding to the {@link Optional}'s value.
     * @see #forValue(Object)
     */
    public static FieldType forValue(Optional<Object> field) {
        return forValue(field.orNull());
    }

    /**
     * Resolves the {@code FieldType} corresponding to the {@code value}'s class
     * 
     * @see #forBinding(Class)
     */
    public static FieldType forValue(@Nullable Object value) {
        if (value == null) {
            return NULL;
        }
        Class<?> fieldClass = value.getClass();
        return forBinding(fieldClass);
    }

    /**
     * @return the {@code FieldType} associated to the provided binding, or
     *         {@link FieldType#UNKNOWN} if no {@code FieldType} relates to the argument attribute
     *         type.
     */
    public static FieldType forBinding(@Nullable Class<?> binding) {
        if (binding == null || Void.class.equals(binding)) {
            return NULL;
        }
        // try a hash lookup first
        FieldType fieldType = BINDING_MAPPING.get(binding);
        if (fieldType != null) {
            return fieldType;
        }
        // not in the map, lets check one by one anyways
        // beware for this to work properly FieldTypes for super classes must be defined _after_
        // any subclass (i.e. Point before Geometry)
        for (FieldType t : values()) {
            if (t.getBinding() != null && t.getBinding().isAssignableFrom(binding)) {
                return t;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns a safe copy (e.g. a clone or immutable copy) of {@code value}, if it is of a mutable
     * object type (e.g. {@code java.util.Map, com.vividsolutions.jts.geom.Geometry, etc}), or the
     * same object if it's already immutable (e.g. {@code java.lang.Integer,etc}).
     * 
     * @param value an object of this {@code FiledType}'s {@link FieldType#getBinding() binding}
     *        type.
     */
    @Nullable
    public Object safeCopy(@Nullable Object value) {
        return safeCopyBuilder.apply(value);
    }

}
