/* Copyright (C) 2023 by Jon Taylor

This program is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.object.*;
import armarender.math.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.lang.Math;
import javax.swing.JOptionPane;
import armarender.texture.*;

public class ArcTubeUtils {
    private LayoutWindow window = null;
    
    // Constructor requires the Scene object and layout window in order to add generated objects.
    ArcTubeUtils(LayoutWindow window){
        this.window = window;
    }
    
    /**
     * rotateNotchNinty
     * Description: Rotate a given or selected notch ninty degrees around the centre of an arc tube.
     * @param: ObjectInfo info - given object. If null the selected object will be used.
     */
    public void rotateNotchNinty(ObjectInfo info){
        LayoutModeling layout = new LayoutModeling();
        if(window == null)
            return;
        
        // Prompt for angle
        String degreeAngleString = JOptionPane.showInputDialog("Angle to rotate in degrees?", 90);
        double angle = 90;
        try {
            angle = Double.parseDouble(degreeAngleString);
        } catch (Exception e){
            
        }
        double angleRadians = Math.toRadians(angle);
        if(angleRadians < 0){
            angleRadians += Math.PI * 2;
        }
        if(angleRadians > Math.PI * 2){
            angleRadians -= Math.PI * 2;
        }
        
        
        if(info == null){
            Scene scene = window.getScene();
            int selection[] = scene.getSelection();
            for(int i = 0; i < selection.length; i++){
                ObjectInfo objectInfo = scene.getObject(selection[i]);
                //System.out.println(" -> " + objectInfo.getName());
                Object obj = (Object)objectInfo.getObject();
                if(obj instanceof armarender.object.Curve){
                    //System.out.println(" selected curve " + objectInfo.getName() );
                    info = objectInfo;
                }
            }
        }
        
        if(info != null){
            ObjectInfo parentArc = info.getParent();
            if(parentArc != null){
                Object3D arcTube = parentArc.getObject();
                // Find closest arcTube segment to get the centre line for rotation.
                if(arcTube instanceof armarender.object.Curve){
                    CoordinateSystem objectCS;
                    objectCS = layout.getCoords(parentArc);
                    
                    // Get centre point of selected notch.
                    BoundingBox notchBounds = info.getTranslatedBounds();
                    Vec3 notchCenter = notchBounds.getCenter();
                    
                    double closestSegmentDistance = Double.MAX_VALUE;
                    Vec3 closestSegmentVecA = null;
                    Vec3 closestSegmentVecB = null;
                    
                    MeshVertex v[] = ((Mesh)arcTube).getVertices();
                    for(int ce = 1; ce < v.length; ce++){
                        Vec3 vecA = new Vec3(v[ce-1].r);
                        Vec3 vecB = new Vec3(v[ce].r);
                        Mat4 mat4 = objectCS.duplicate().fromLocal();
                        mat4.transform(vecA);
                        mat4.transform(vecB);
                        Vec3 midPoint = vecA.midPoint(vecB);
                        double distToNotch = midPoint.distance(notchCenter);
                        if(distToNotch < closestSegmentDistance){
                            closestSegmentDistance = distToNotch;
                            closestSegmentVecA = vecA;
                            closestSegmentVecB = vecB;
                        }
                    }
                    
                    if(closestSegmentVecA != null && closestSegmentVecB != null){
                        Vec3 segmentNormal = new Vec3(closestSegmentVecA);
                        segmentNormal.subtract(closestSegmentVecB);
                        segmentNormal.normalize();
                        //System.out.println("segmentNormal: " + segmentNormal);
                        
                        // closest point along segment
                        Vec3 closestSegmentPoint = getClosestSegmentPoint(closestSegmentVecA, closestSegmentVecB, notchCenter);
                        //System.out.println("closest seg: " + closestSegmentPoint);
                        
                        ObjectInfo rotatedNotch = info.duplicate();
                        
                        CoordinateSystem notchCS;
                        notchCS = layout.getCoords(info);
                        
                        MeshVertex notchVerts[] = ((Mesh)info.getObject()).getVertices();
                        Vec3 rotatedVerts[] = new Vec3[notchVerts.length];
                        
                        for(int nv = 0; nv < notchVerts.length; nv++){
                            Vec3 vecA = new Vec3(notchVerts[nv].r);
                            Mat4 mat4 = notchCS.duplicate().fromLocal();
                            mat4.transform(vecA);
                            Vec3 origionalVecA = new Vec3(vecA);
                            vecA.subtract(closestSegmentPoint);
                            Mat4 rotateMatrix = Mat4.axisRotation(segmentNormal, angleRadians);
                            rotateMatrix.transform(vecA);
                            vecA.add(closestSegmentPoint);
                            rotatedVerts[nv] = vecA;
                        }
                        
                        CoordinateSystem defaultCoords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                        info.setCoords(defaultCoords);
                        ((Mesh)info.getObject()).setVertexPositions(rotatedVerts);
                        info.clearCachedMeshes();
                        window.updateImage();
                        
                        // Update mirrored objects.
                        window.mirrorCurve.update(info);
                    }
                }
            }
        }
    }
    
    
    
    /**
     * getClosestSegmentPoint
     *
     * Description: Find closest point on a line to a reference point.
     *
     */
    public Vec3 getClosestSegmentPoint(Vec3 a, Vec3 b, Vec3 targetPoint){
        Vector<Vec3> segmentPoints = new Vector<Vec3>();
        segmentPoints.addElement(a);
        segmentPoints.addElement(b);
        segmentPoints = subdivideSegment(segmentPoints, a, b, /*depth*/0);
        double closestDistance = Double.MAX_VALUE;
        Vec3 closestVec = new Vec3(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE);
        //System.out.println(" segmentPoints.size() " + segmentPoints.size());
        for(int i = 0; i < segmentPoints.size(); i++){
            Vec3 currVec = (Vec3)segmentPoints.elementAt(i);
            double dist = currVec.distance(targetPoint);
            //System.out.println("                dist: " + dist + " " + i);
            if(dist < closestDistance){
                closestDistance = dist;
                closestVec = currVec;
            }
        }
        return closestVec;
    }
    
    
    // Generate segment points
    public Vector<Vec3> subdivideSegment(Vector<Vec3> points, Vec3 a, Vec3 b, int depth){
        if(depth > 10){ // 7 = 256, 8 = 513  - was 8
            return points;
        }
        Vec3 mid = a.midPoint(b);
        points.addElement(mid);
        points = subdivideSegment(points, a, mid, depth + 1);
        points = subdivideSegment(points, mid, b, depth + 1);
        return points;
    }
    
}
