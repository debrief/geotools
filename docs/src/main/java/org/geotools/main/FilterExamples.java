/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */

package org.geotools.main;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.measure.Unit;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.util.SuppressFBWarnings;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.Parameter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * This class gathers up the filter examples shown in the sphinx documentation for Filters.
 *
 * @author Jody Garnett
 */
public class FilterExamples {
    @SuppressFBWarnings("NP_UNWRITTEN_FIELD")
    SimpleFeatureSource featureSource;

    /**
     * How to find Features using IDs?
     *
     * <p>Each Feature has a FeatureID; you can use these FeatureIDs to request the feature again
     * later.
     *
     * <p>If you have a Set<String> of feature IDs, which you would like to query from a shapefile:
     *
     * @param selection Set of FeatureIDs identifying requested content
     * @return Selected Features
     * @throws IOException
     */
    // grabSelectedIds start
    SimpleFeatureCollection grabSelectedIds(Set<String> selection) throws IOException {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        Set<FeatureId> fids = new HashSet<>();
        for (String id : selection) {
            FeatureId fid = ff.featureId(id);
            fids.add(fid);
        }
        Filter filter = ff.id(fids);
        return featureSource.getFeatures(filter);
    }
    // grabSelectedIds end

    /**
     * How to find a Feature by Name?
     *
     * <p>CQL is very good for one off queries like this:
     *
     * @param name
     * @return
     * @throws CQLException
     */
    // grabSelectedName start
    SimpleFeatureCollection grabSelectedName(String name) throws Exception {
        return featureSource.getFeatures(CQL.toFilter("Name = '" + name + "'"));
    }

    // grabSelectedName end

    /**
     * To select this feature while ignoring case we are going to have to use the FilterFactory
     * (rather than CQL):
     */
    // grabSelectedNameIgnoreCase start
    SimpleFeatureCollection grabSelectedNameIgnoreCase(String name) throws Exception {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        Filter filter = ff.equal(ff.property("Name"), ff.literal(name), false);
        return featureSource.getFeatures(filter);
    }

    // grabSelectedNameIgnoreCase end

    /**
     * How to find Features using a Set of Names?
     *
     * <p>If you have a Set<String> of "names" which you would like to query from PostGIS. In this
     * case we are doing a check for an attribute called "Name".
     */
    // grabSelectedNames start
    SimpleFeatureCollection grabSelectedNames(Set<String> selectedNames) throws Exception {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        List<Filter> match = new ArrayList<>();
        for (String name : selectedNames) {
            Filter aMatch = ff.equals(ff.property("Name"), ff.literal(name));
            match.add(aMatch);
        }
        Filter filter = ff.or(match);
        return featureSource.getFeatures(filter);
    }

    // grabSelectedNames end

    /**
     * What features on in this bounding Box?
     *
     * <p>You can make a bounding box query as shown below:
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     * @throws Exception
     */
    // grabFeaturesInBoundingBox start
    SimpleFeatureCollection grabFeaturesInBoundingBox(double x1, double y1, double x2, double y2)
            throws Exception {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        FeatureType schema = featureSource.getSchema();

        // usually "THE_GEOM" for shapefiles
        String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
        CoordinateReferenceSystem targetCRS =
                schema.getGeometryDescriptor().getCoordinateReferenceSystem();

        ReferencedEnvelope bbox = new ReferencedEnvelope(x1, y1, x2, y2, targetCRS);

        Filter filter = ff.bbox(ff.property(geometryPropertyName), bbox);
        return featureSource.getFeatures(filter);
    }

    // grabFeaturesInBoundingBox end

    // grabFeaturesInPolygon start
    SimpleFeatureCollection grabFeaturesInPolygon(double x1, double y1, double x2, double y2)
            throws Exception {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        FeatureType schema = featureSource.getSchema();
        CoordinateReferenceSystem worldCRS = DefaultGeographicCRS.WGS84;

        // usually "THE_GEOM" for shapefiles
        String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
        CoordinateReferenceSystem targetCRS =
                schema.getGeometryDescriptor().getCoordinateReferenceSystem();

        ReferencedEnvelope click = new ReferencedEnvelope(x1, y1, x2, y2, worldCRS);

        // will result in a slight larger BBOX then the original click
        ReferencedEnvelope bbox = click.transform(targetCRS, true);

        // will result in a polygon matching the original click
        Polygon clickPolygon = JTS.toGeometry(bbox, null, 10);
        MathTransform transform = CRS.findMathTransform(worldCRS, targetCRS);
        Polygon polygon = (Polygon) JTS.transform(clickPolygon, transform);

        Filter filter = ff.intersects(ff.property(geometryPropertyName), ff.literal(polygon));

        return featureSource.getFeatures(filter);
    }

    // grabFeaturesInPolygon end

    // grabFeaturesOnScreen start
    SimpleFeatureCollection grabFeaturesOnScreen(ReferencedEnvelope screen) throws Exception {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        FeatureType schema = featureSource.getSchema();

        // usually "THE_GEOM" for shapefiles
        String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
        CoordinateReferenceSystem targetCRS =
                schema.getGeometryDescriptor().getCoordinateReferenceSystem();
        CoordinateReferenceSystem worldCRS = screen.getCoordinateReferenceSystem();

        // will result in a slight larger BBOX then the original click
        ReferencedEnvelope bbox = screen.transform(targetCRS, true);

        // will result in a polygon matching the original click
        Polygon clickPolygon = JTS.toGeometry(bbox, null, 10);
        MathTransform transform = CRS.findMathTransform(worldCRS, targetCRS);
        Polygon polygon = (Polygon) JTS.transform(clickPolygon, transform);

        Filter filter1 = ff.bbox(ff.property(geometryPropertyName), bbox);
        Filter filter2 = ff.intersects(ff.property(geometryPropertyName), ff.literal(polygon));

        Filter filter = ff.and(filter1, filter2);

        return featureSource.getFeatures(filter);
    }

    // grabFeaturesOnScreen end

    @SuppressFBWarnings("NP_UNWRITTEN_FIELD")
    private JMapFrame mapFrame;

    // click1 start
    SimpleFeatureCollection click1(MapMouseEvent ev) throws Exception {
        // Construct a 3x3 pixel rectangle centred on the mouse click position
        java.awt.Point screenPos = ev.getPoint();

        Rectangle screenRect = new Rectangle(screenPos.x - 1, screenPos.y - 1, 3, 3);
        CoordinateReferenceSystem worldCRS =
                mapFrame.getMapContent().getCoordinateReferenceSystem();
        // Transform the screen rectangle into a bounding box in the
        // coordinate reference system of our map content.
        AffineTransform screenToWorld = mapFrame.getMapPane().getScreenToWorldTransform();
        Rectangle2D worldRect = screenToWorld.createTransformedShape(screenRect).getBounds2D();
        ReferencedEnvelope worldBBox = new ReferencedEnvelope(worldRect, worldCRS);

        // transform from world to target CRS
        SimpleFeatureType schema = featureSource.getSchema();
        CoordinateReferenceSystem targetCRS = schema.getCoordinateReferenceSystem();
        String geometryAttributeName = schema.getGeometryDescriptor().getLocalName();

        ReferencedEnvelope bbox = worldBBox.transform(targetCRS, true, 10);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        // Option 1 BBOX
        Filter filter = ff.bbox(ff.property(geometryAttributeName), bbox);

        // Option 2 Intersects
        // Filter filter = ff.intersects(ff.property(geometryAttributeName), ff.literal(bbox));

        return featureSource.getFeatures(filter);
    }

    // click1 end

    // distance start
    SimpleFeatureCollection distance(MapMouseEvent ev) throws Exception {
        DirectPosition2D worldPosition = ev.getWorldPos();

        // get the unit of measurement
        SimpleFeatureType schema = featureSource.getSchema();
        CoordinateReferenceSystem crs =
                schema.getGeometryDescriptor().getCoordinateReferenceSystem();
        Unit<?> uom = crs.getCoordinateSystem().getAxis(0).getUnit();

        MathTransform transform =
                CRS.findMathTransform(worldPosition.getCoordinateReferenceSystem(), crs, true);

        DirectPosition dataPosition = transform.transform(worldPosition, null);

        Point point = JTS.toGeometry(dataPosition);

        // threshold distance
        double distance = 10.0d;

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter filter =
                ff.dwithin(ff.property("POLYGON"), ff.literal(point), distance, uom.toString());

        return featureSource.getFeatures(filter);
    }

    // distance end

    @SuppressFBWarnings
    // polygonInteraction start
    void polygonInteraction() {
        SimpleFeatureCollection polygonCollection = null;
        SimpleFeatureCollection fcResult = null;
        final DefaultFeatureCollection found = new DefaultFeatureCollection();

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        SimpleFeature feature = null;

        Filter polyCheck = null;
        Filter andFil = null;
        Filter boundsCheck = null;

        String qryStr = null;

        SimpleFeatureIterator it = polygonCollection.features();
        try {
            while (it.hasNext()) {
                feature = it.next();
                BoundingBox bounds = feature.getBounds();
                boundsCheck = ff.bbox(ff.property("the_geom"), bounds);

                Geometry geom = (Geometry) feature.getDefaultGeometry();
                polyCheck = ff.intersects(ff.property("the_geom"), ff.literal(geom));

                andFil = ff.and(boundsCheck, polyCheck);

                try {
                    fcResult = featureSource.getFeatures(andFil);
                    // go through results and copy out the found features
                    fcResult.accepts(
                            new FeatureVisitor() {
                                public void visit(Feature feature) {
                                    found.add((SimpleFeature) feature);
                                }
                            },
                            null);
                } catch (IOException e1) {
                    System.out.println("Unable to run filter for " + feature.getID() + ":" + e1);
                    continue;
                }
            }
        } finally {
            it.close();
        }
    }

    // polygonInteraction end

    private void expressionExamples() {
        Geometry geometry = null;
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        // expressionExamples start
        Expression propertyAccess = ff.property("THE_GEOM");
        Expression literal = ff.literal(geometry);
        Expression math = ff.add(ff.literal(1), ff.literal(2));
        Expression function = ff.function("length", ff.property("CITY_NAME"));
        // expressionExamples end
    }

    private static void functionList() {
        // functionList start
        Set<FunctionFactory> functionFactories = CommonFactoryFinder.getFunctionFactories(null);

        for (FunctionFactory factory : functionFactories) {
            System.out.println(factory.getClass().getName());
            List<FunctionName> functionNames = factory.getFunctionNames();
            ArrayList<FunctionName> sorted = new ArrayList<>(functionNames);
            Collections.sort(
                    sorted,
                    new Comparator<FunctionName>() {
                        public int compare(FunctionName o1, FunctionName o2) {
                            if (o1 == null && o2 == null) return 0;
                            if (o1 == null && o2 != null) return 1;
                            if (o1 != null && o2 == null) return -1;

                            return o1.getName().compareTo(o2.getName());
                        }
                    });
            for (FunctionName functionName : sorted) {
                System.out.print("    ");
                System.out.print(functionName.getName());
                System.out.print("(");
                int i = 0;
                for (Parameter<?> argument : functionName.getArguments()) {
                    if (i++ > 0) {
                        System.out.print(", ");
                    }
                    System.out.print(argument.getName());
                    if (argument.getType() == Object.class && argument.isRequired()) {
                        // no interesting description
                    } else {
                        System.out.print("{");
                        System.out.print(argument.getType().getSimpleName());
                        if (argument.isRequired()) {
                            System.out.print(",required");
                        } else if (argument.getMinOccurs() == 0 && argument.getMaxOccurs() == 1) {
                            System.out.print(",optional");
                        } else {
                            int min = argument.getMinOccurs();
                            int max = argument.getMaxOccurs();
                            System.out.print(",");
                            System.out.print(min);
                            System.out.print(":");
                            System.out.print(max == Integer.MAX_VALUE ? "unbounded" : max);
                        }
                        System.out.print("}");
                    }
                }
                Parameter<?> result = functionName.getReturn();

                System.out.print(")");
                System.out.print(":" + result.getName());
                if (result.getType() != Object.class) {
                    System.out.print("{");
                    Class<?> type = result.getType();
                    if (type != null) {
                        System.out.print(type.getSimpleName());
                    } else {
                        System.out.print("null");
                    }
                    System.out.print("}");
                }
                System.out.println();
            }
        }
        // functionList end
    }

    private static void functionListPretty() {
        Set<FunctionFactory> functionFactories = CommonFactoryFinder.getFunctionFactories(null);

        for (FunctionFactory factory : functionFactories) {
            String factoryName = factory.getClass().getSimpleName();
            System.out.println(codeBlock + factoryName + codeBlock);
            System.out.println(
                    "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^"
                            .substring(0, factoryName.length() + 4));
            System.out.println();

            List<FunctionName> functionNames = factory.getFunctionNames();
            ArrayList<FunctionName> sorted = new ArrayList<>(functionNames);
            Collections.sort(
                    sorted,
                    new Comparator<FunctionName>() {
                        public int compare(FunctionName o1, FunctionName o2) {
                            if (o1 == null && o2 == null) return 0;
                            if (o1 == null && o2 != null) return 1;
                            if (o1 != null && o2 == null) return -1;

                            return o1.getName().compareTo(o2.getName());
                        }
                    });

            System.out.println("Contains " + sorted.size() + " functions.");
            System.out.println();

            for (FunctionName functionName : sorted) {
                Parameter<?> result = functionName.getReturn();

                StringBuilder fn = new StringBuilder();
                fn.append(codeBlock);
                fn.append(functionName.getName());

                fn.append("(");
                int i = 0;
                for (Parameter<?> argument : functionName.getArguments()) {
                    if (i++ > 0) {
                        fn.append(", ");
                    }
                    fn.append(argument.getName());
                }
                fn.append(")");
                fn.append(codeBlock);
                fn.append(": returns " + codeBlock + result.getName() + codeBlock);

                System.out.println(fn.toString());
                for (int h = 0; h < fn.length(); h++) {
                    System.out.print("'");
                }
                System.out.println();

                System.out.println();
                for (Parameter<?> argument : functionName.getArguments()) {
                    System.out.println("* " + argument(argument));
                    System.out.println();
                }
                System.out.println("* " + argument(result, true));
                System.out.println();
            }
        }
    }

    static final String codeBlock = "``";

    public static String argument(Parameter<?> argument) {
        return argument(argument, false);
    }

    public static String argument(Parameter<?> argument, boolean result) {

        StringBuilder arg = new StringBuilder();
        arg.append(codeBlock);
        arg.append(argument.getName());
        arg.append(codeBlock);
        Class<?> type = argument.getType();

        if (type == null || (type == Object.class && argument.isRequired())) {
            // nothing more is known
        } else {
            arg.append(" (" + codeBlock);
            arg.append(type.getSimpleName());
            arg.append(codeBlock + ")");
            int min = argument.getMinOccurs();
            int max = argument.getMaxOccurs();
            if (min > 1 && max > 1) {
                arg.append(": ");
                arg.append(" min=");
                arg.append(min);
                arg.append(" max=");
                arg.append(max == Integer.MAX_VALUE ? "unbounded" : max);
            } else {
                if (!result) {
                    if (argument.isRequired()) {
                        arg.append(" required");
                    } else if (argument.getMinOccurs() == 0 && argument.getMaxOccurs() == 1) {
                        arg.append(" optional");
                    }
                }
            }
        }
        return arg.toString();
    }

    public static void main(String args[]) {
        functionListPretty();
    }
}
