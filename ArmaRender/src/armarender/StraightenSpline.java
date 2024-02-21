/*
 StraightenSpline.java
 
 Copyright (C) 2020 by Jon Taylor

 This program is free software; you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation; either version 2 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 
*/


package armarender;

import buoy.event.*;
import buoy.widget.*;
import java.util.*;
import armarender.object.*;
import armarender.math.*;
import java.lang.Math;

public class StraightenSpline {
    LayoutWindow layoutWindow;
    
    public StraightenSpline(LayoutWindow layoutWindow){
        this.layoutWindow = layoutWindow;
    }
    
    /**
     * straightenSpline
     *
     * Description: With a selected curve object this function will straighten it out and rotate children curves
     * so that it can be used by the CNC tube notcher.
     *
     * Note: This function only works on one axis.
     *
     *  @param vector objects - scene objects used to find a selected curve for processing.
     */
    public void straightenSpline(Vector<ObjectInfo> objects){
        System.out.println("Straighten Spline ");
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                //System.out.println("Object Info: ");
                Object co = (Object)obj.getObject();
                if((co instanceof Curve) == true){
                    CoordinateSystem cs = ((ObjectInfo)obj).getCoords();
                    
                    Mesh mesh = (Mesh) obj.getObject(); // Object3D
                    Vec3 [] verts = mesh.getVertexPositions();
                    
                    Vec3 vecPoints[] = new Vec3[verts.length];
                    for(int i = 0; i < verts.length; i++){
                        vecPoints[i] = verts[i];
                    }

                    Vector ignoreChildren = new Vector();
                    HashMap childSegmentDistances = new HashMap();
                    Vector ignoreVerticies = new Vector();
                    HashMap childVertexSegmentDistances = new HashMap();
                  
                    double linearLength = 0;
                    for(int i = 1; i < verts.length; i++){
                        Vec3 vertA = verts[i - 1];
                        Vec3 vertB = verts[i];
                        
                        System.out.println("Segment " + i);
                        
                        Vec3 worldVertA = new Vec3(vertA);
                        Vec3 worldVertB = new Vec3(vertB);
                        cs = ((ObjectInfo)obj).getCoords();
                        Mat4 mat4 = cs.duplicate().fromLocal();
                        mat4.transform(worldVertA);
                        mat4.transform(worldVertB);
                        
                        double distance = Math.sqrt(Math.pow(vertA.x - vertB.x, 2) + Math.pow(vertA.y - vertB.y, 2) + Math.pow(vertA.z - vertB.z, 2));
                        linearLength += distance;
                        //System.out.println("    vert: " +
                        //                   vertA.x + " " + vertA.y + "  " + vertA.z  + " - " +
                        //                   vertB.x + " " + vertB.y + "  " + vertB.z);
                        //System.out.println( "distance " + distance);
                        if(true){
                        //if(i == 1 || i == 2 || i == 3 || i == 4 || i == 5){
                            
                            // Rotate all remaining points around vertA to angle required to become straight.
                            double rotateXRequired = 0;
                            double rotateYRequired = 0;
                            
                            Vec3 vertBNew = new Vec3(vertB);
                            vertBNew.x = vertA.x - distance;
                            vertBNew.y = vertA.y;
                            vertBNew.z = vertA.z;
                            vecPoints[i] = vertBNew;
                            verts[i] = vertBNew;
                            
                            rotateXRequired = getAngleX(vertA, vertB, vertBNew);
                            //System.out.println("rotateXRequired " + rotateXRequired);
                            rotateYRequired = getAngleY(vertA, vertB, vertBNew);
                            //System.out.println("rotateYRequired " + rotateYRequired);
                            
                            // rotate any child objects between vertA and vertB around vertA
                            //
        
                            BoundingBox targetRegion = new BoundingBox(worldVertA.x, worldVertB.x,
                                                                       worldVertA.y, worldVertB.y,
                                                                       worldVertA.z, worldVertB.z);
                            
                            targetRegion.outset( Math.abs(worldVertA.x - worldVertB.x) / 50.0 ); // 2% of x
                            targetRegion.expandPercentage(10);
                            
                            
                            //targetRegion.expandZPercentage(10);
                            //targetRegion.expandYPercentage(10);
                            
                            
                            System.out.println(" target x " + targetRegion.minx  + " " + targetRegion.maxx +
                                               " y " + targetRegion.miny  + " " + targetRegion.maxy +
                                               " z " + targetRegion.minz  + " " + targetRegion.maxz);
                            
                            Vector allChildren = obj.getRecursiveChildren();
                            System.out.println("all " + allChildren.size());
                            for (int c = 0; c < allChildren.size(); c++){
                                ObjectInfo child = (ObjectInfo)allChildren.elementAt(c);
                                
                            //for (int c = 0; c < obj.getChildren().length; c++){
                                //ObjectInfo child = obj.getChildren()[c];
                                Object childco = (Object)child.getObject();
                                
                                //
                                // Small Curve
                                //
                                if((childco instanceof Curve) == true){                 // Roate child curves
                                    
                                    //System.out.println(" child " + child.getName() );
                                    
                                    BoundingBox childBox = getTranslatedBounds(child);
                                    Vec3 childCentre = childBox.getCenter();
                                    //System.out.println("   child " + child.getName()+ " centre x " + childCentre.x +
                                    //                   " y " + childCentre.y +
                                    //                   " z " + childCentre.z );
                                    
                                    boolean moveChild = true; // Move all children until we iterate to their region on the curve.
                                    for(int x = 0; x < ignoreChildren.size(); x++){
                                        ObjectInfo oi = (ObjectInfo)ignoreChildren.elementAt(x);
                                        if(oi == child){
                                            moveChild = false;
                                        }
                                    }
                                    
                                    
                                    // if segment distance is getting larger
                                    Vec3 segmentCentre = targetRegion.getCenter();
                                    //Vec3 childCentre = childBox.getCenter();
                                    double segmentToChildDistance = Math.sqrt(Math.pow(segmentCentre.x - childCentre.x, 2) + Math.pow(segmentCentre.y - childCentre.y, 2) + Math.pow(segmentCentre.z - childCentre.z, 2));
                                    //System.out.println("   seg dist " + segmentToChildDistance);
                                    if(childSegmentDistances.containsKey(child) && ignoreChildren.contains(child) == false){
                                        double previousObjectToSegmentDistance = (Double)childSegmentDistances.get(child);
                                        if(segmentToChildDistance > previousObjectToSegmentDistance){
                                            // if we are moving away from (past) the child object on our traversal across the line then consider it processed.
                                            System.out.println("        *** PASSING " + child.getName() );
                                            ignoreChildren.addElement(child);
                                            moveChild = false;
                                        }
                                    }
                                    childSegmentDistances.put(child, segmentToChildDistance); // Save curr segment distance to child object.
                                    
                                    
                                    if(moveChild){
                                        System.out.println("  move child " + child.getName() + " angle: " + rotateXRequired);
                                        //System.out.println("       worldVertA  x " + worldVertA.x +   // correct
                                        //" y " + worldVertA.y +
                                        //" z " + worldVertA.z );
                                        // if not ignored child
                                        CoordinateSystem childCs = ((ObjectInfo)child).getCoords();
                                        //System.out.println(" child coord x " +  childCs.getOrigin().x +  " y "+ childCs.getOrigin().y );
                                        Mat4 childMat4 = childCs.duplicate().fromLocal();
                                        Mesh childMesh = (Mesh) child.getObject(); // Object3D
                                        Vec3 [] childVerts = childMesh.getVertexPositions();
                                        for(int d = 0; d < childVerts.length; d++){
                                            Vec3 childVert = childVerts[d];
                                            childMat4.transform(childVert);
                                            //System.out.println("           childVert  x " + childVert.x +
                                            //" y " + childVert.y +
                                            //" z " + childVert.z );
                                            childVert = rotatePointX(childVert, worldVertA, rotateXRequired);  // rotate child around vec A by angle.
                                            //childVert = rotatePointY(childVert, worldVertA, rotateYRequired);
                                            //System.out.println("           ->childVert  x " + childVert.x +
                                            //" y " + childVert.y +
                                            //" z " + childVert.z );
                                            childVerts[d] = childVert; // ISSUE because of translation
                                        }
                                        CoordinateSystem zeroCS = new CoordinateSystem();
                                        child.setCoords(zeroCS);
                                        ((Mesh)child.getObject()).setVertexPositions(childVerts);
                                        child.clearCachedMeshes();
                                    }
                                    
                                    
                                    // bounds maintinance
                                    if(targetRegion.contains(childCentre) ){
                                        System.out.println("        *** INSIDE " + child.getName() );
                                        // Don't move this child object any more because it's region has been bent to the correct place.
                                        ignoreChildren.addElement(child);
                                    }
                                } // end curve object
                                
                                //
                                // Large Mesh object
                                //
                                if((childco instanceof Mesh) == true){ // Straighten mesh vertecies
                                    //System.out.println(" child mesh " + child.getName() );
                                    
                                    //boolean move = true; // Don't touch objects that have been moved.
                                    //for(int x = 0; x < ignoreChildren.size(); x++){
                                    //    ObjectInfo oi = (ObjectInfo)ignoreChildren.elementAt(x);
                                    //    if(oi == child){
                                    //        move = false;
                                    //    }
                                    //}
                                    
                                    // Object must be large and not moved by previous function.
                                    BoundingBox childBox = getTranslatedBounds(child);
                                    BoundingBox splineBox = getTranslatedBounds(obj);
                                    if( (childBox.maxx - childBox.minx) > ( (splineBox.maxx - splineBox.minx) / 2) ){
                                        // Object is big enough to modify vertivies.
                                        //System.out.println(" big enouigh " + child.getName()  );
                                        
                                        Vec3 segmentCentre = targetRegion.getCenter();
                                        
                                        CoordinateSystem childCs = ((ObjectInfo)child).getCoords();
                                        Mat4 childMat4 = childCs.duplicate().fromLocal();
                                        Mesh childMesh = (Mesh) child.getObject(); // Object3D
                                        Vec3 [] childVerts = childMesh.getVertexPositions();
                                        for(int d = 0; d < childVerts.length; d++){
                                            String childVertKey = child.getName() + "." + d;
                                            Vec3 childVert = childVerts[d];
                                            childMat4.transform(childVert);
                                            
                                            boolean move = true;
                                            for(int x = 0; x < ignoreVerticies.size(); x++){
                                                String currIgnoreKey = (String)ignoreVerticies.elementAt(x);
                                                if(currIgnoreKey.equals(childVertKey)){
                                                    move = false;
                                                }
                                            }
                                        
                                            double vertDistance = Math.sqrt(Math.pow(segmentCentre.x - childVert.x, 2) + Math.pow(segmentCentre.y - childVert.y, 2) + Math.pow(segmentCentre.z - childVert.z, 2));
                                            //System.out.println(" vertDistance: " + vertDistance);
                                            
                                            // scan passes vert so its done.
                                            if(childVertexSegmentDistances.containsKey(childVertKey) && ignoreVerticies.contains(childVertKey) == false){
                                                double previousObjectToSegmentDistance = (Double)childVertexSegmentDistances.get(childVertKey);
                                                //System.out.println(" vertDistance " + vertDistance + " >  previousObjectToSegmentDistance " + previousObjectToSegmentDistance);
                                                if(vertDistance > previousObjectToSegmentDistance){
                                                    // if we are moving away from (past) the child object on our traversal across the line then consider it processed.
                                                    //System.out.println("        *** PASSING V " + child.getName() + " " + d );
                                                    ignoreVerticies.addElement(childVertKey);
                                                    move = false;
                                                }
                                            }
                                            childVertexSegmentDistances.put(childVertKey, vertDistance);
                                            //System.out.println("size " + childVertexSegmentDistances.size());
                                            
                                            if(move){
                                                // Move
                                                //System.out.print(". " + vertDistance);
                                                childVerts[d] = rotatePointX(childVert, worldVertA, rotateXRequired);  // rotate child around vec A by angle.
                                                   
                                                CoordinateSystem zeroCS = new CoordinateSystem();
                                                child.setCoords(zeroCS);
                                                ((Mesh)child.getObject()).setVertexPositions(childVerts);
                                                child.clearCachedMeshes();
                                            }
                                            move = true; // reset for remaining verts in obj
                                        }
                                        
                                    }
                                    
                                    // Find vertecies in region.
                                    
                                    // TODO:
                                    // ignoreChildren
                                    // ignoreVerticies
                                } // end mesh object
                            }
                            
                            
                            // Rotate following points by angle around vertA
                            for(int j = i + 1; j < verts.length; j++){
                                Vec3 next = verts[j];
                                Vec3 rotated = rotatePointX(next, vertA, rotateXRequired);
                                
                                //rotated = rotatePointY(rotated, vertA, rotateYRequired);
                                
                                verts[j] = rotated;
                                vecPoints[ j ] = rotated;
                            }
                            ((Mesh)obj.getObject()).setVertexPositions(vecPoints); // Update object coords
                        }
                        
                    } // verts pairs
                    
                    System.out.println(" len " + linearLength);
                    
                    // Update scene
                    //((Mesh)obj.getObject()).setVertexPositions(vecPoints); // todo: check object is instance of type.
                    obj.clearCachedMeshes();
                    ((LayoutWindow)layoutWindow).setModified();
                    ((LayoutWindow)layoutWindow).updateImage();
                    
                } else {
                    //
                    System.out.println("No curve selected.");
                }
            }
        }
    }
    
    /**
     * getAngleX
     *
     * Description: get an angle between the vectors (a,b) and (a, b2) only on the XY axis plane. Facing down Z axis.
     *  Note: Maybe the name X is bad for this function. Should be XY or Z?
     *
     * @param: Vec3 a.
     * @param: Vec3 b.
     * @param: Vec3 c.
     */
    double getAngleX(Vec3 a, Vec3 b, Vec3 c){
        double angle = 0;
        // Scale to a.
        double x1 = b.x - a.x;     // 1 = b
        double x2 = c.x - a.x;     // 2 = b2
        double y1 = b.y - a.y;
        double y2 = c.y - a.y;
        //System.out.println("     - x1: " + x1 + " y1: " + y1 + "   x2: " + x2 + " y2: " + y2 );
        //System.out.println(" angle  " + ( x1 * x2 + y1 * y2 ));
        
        double angleA = (float) Math.atan2(a.y - b.y, a.x - b.x);
        float angleB = (float) Math.atan2(a.y - c.y, a.x - c.x);
        angle = angleB - angleA;
        /*double angleX = Math.acos(
                          ( x1 * x2 + y1 * y2 ) /
                          ( Math.sqrt(x1*x1 + y1*y1) * Math.sqrt(x2*x2 + y2*y2) )
                          ); // only returns absolute angle which is not useful.*/
        //System.out.println("  angle " + angle + "  x " + angleX);
        return angle;
    }
    
    /**
     * getAngleY
     *
     * Description: get an angle between the vectors (a,b) and (a, b2) only on the XZ axis. Facing down Y aaxis.
     */
    double getAngleY(Vec3 a, Vec3 b, Vec3 b2){
        double angle = 0;
        // Scale to a.
        double x1 = b.x - a.x;     // 1 = b
        double x2 = b2.x - a.x;     // 2 = b2
        double y1 = b.z - a.z;
        double y2 = b2.z - a.z;
        //System.out.println("     - x1: " + x1 + " y1: " + y1 + "   x2: " + x2 + " y2: " + y2 );
        angle = Math.acos((x1*x2 + y1*y2) / ( Math.sqrt(x1*x1 + y1*y1) * Math.sqrt(x2*x2 + y2*y2)));
        return angle;
    }
    
    /**
     * rotatePoint
     *
     * Description:
     */
    Vec3 rotatePointX(Vec3 point, Vec3 origin, double angle){
        Vec3 rotatedPoint = new Vec3();
        rotatedPoint.x = origin.x + (point.x-origin.x)*Math.cos(angle) - (point.y - origin.y)*Math.sin(angle);
        rotatedPoint.y = origin.y + (point.x-origin.x)*Math.sin(angle) + (point.y - origin.y)*Math.cos(angle);
        rotatedPoint.z = point.z;
        return rotatedPoint;
    }
    
    /**
     * rotatePointY
     *
     * Description:
     */
    Vec3 rotatePointY(Vec3 point, Vec3 origin, double angle){
        Vec3 rotatedPoint = new Vec3();
        rotatedPoint.x = origin.x + (point.x-origin.x)*Math.cos(angle) - (point.y - origin.y)*Math.sin(angle);
        rotatedPoint.y = point.y;
        rotatedPoint.z = origin.y + (point.x-origin.x)*Math.sin(angle) + (point.y - origin.y)*Math.cos(angle);
        return rotatedPoint;
    }
     
    
    public BoundingBox getTranslatedBounds(ObjectInfo object){
        BoundingBox bounds = null;
        
        LayoutModeling layout = new LayoutModeling();
        Object3D o3d = object.getObject().duplicate();
        bounds = o3d.getBounds();           // THIS DOES NOT WORK
        
        bounds.minx = 999; bounds.maxx = -999;
        bounds.miny = 999; bounds.maxy = -999;
        bounds.minz = 999; bounds.maxz = -999;
        
        CoordinateSystem c;
        c = layout.getCoords(object);
        Vec3 objOrigin = c.getOrigin();
        
        //System.out.println("getTranslatedBounds: " + object.getName());
        
        Mesh mesh = (Mesh) object.getObject(); // Object3D
        Vec3 [] verts = mesh.getVertexPositions();
        
        //if(object.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
            //TriangleMesh triangleMesh = null;
            //triangleMesh = object.getObject().convertToTriangleMesh(0.05);
            
            //MeshVertex[] points = triangleMesh.getVertices();
            for(int i = 0; i < verts.length; i++){
                Vec3 point = verts[i];
                
                Mat4 mat4 = c.duplicate().fromLocal();
                mat4.transform(point);
                
                if(point.x < bounds.minx){
                    bounds.minx = point.x;
                }
                if(point.x > bounds.maxx){
                    bounds.maxx = point.x;
                }
                if(point.y < bounds.miny){
                    bounds.miny = point.y;
                }
                if(point.y > bounds.maxy){
                    bounds.maxy = point.y;
                }
                if(point.z < bounds.minz){
                    bounds.minz = point.z;
                }
                if(point.z > bounds.maxz){
                    bounds.maxz = point.z;
                }
            }
        //}
        //System.out.println("getTranslatedBounds: " + object.getName());
        return bounds;
    }
}


