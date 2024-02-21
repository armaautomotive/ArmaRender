/* Copyright (C) 2021-2023 Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.*;
import armarender.animation.*;
import armarender.animation.distortion.*;
import armarender.material.*;
import armarender.math.*;
import armarender.texture.*;
import armarender.object.*;
import java.lang.ref.*;
import java.util.*;
import javax.swing.*;

public class ConformMesh {
    LayoutWindow window;
    
    public ConformMesh(LayoutWindow window){ // BFrame parent,  , Scene theScene
        this.window = window;
    }
    
    /**
     * conformMeshToCurves
     *
     * Description: Modify a mesh to conform to child or selected curve profiles.
     *
     * @param: Scene - access to scene to update changes made.
     */
    public void conformMeshToCurves(Scene scene){
        // If a curve is selected, merge verts with a larger proximity range
        // If no objects selected, merge verts with curves using a smaller proximity range
        boolean conformSuccess = false;
        boolean foundSnapCurve = false;
        boolean doundChildCurve = false;
        int selection[] = window.getSelectedIndices();
        
        if(selection.length != 1){
            Sound.playSound("deny.wav");
            System.out.println("Error: Select a mesh to conform.");
            
            JOptionPane.showMessageDialog(null, "Select a mesh to conform.",  "Error" , JOptionPane.ERROR_MESSAGE );
            return;
        }
        
        ObjectInfo objInfo = (ObjectInfo) scene.getObject(selection[0]);
        System.out.println("objInfo: " + objInfo.getName());
        CoordinateSystem cs = ((ObjectInfo)objInfo).getCoords();
        
        
        //TriangleMesh tm = null;
        if(objInfo.getObject() instanceof SplineMesh){ //
            //tm = objInfo.getObject().convertToTriangleMesh(0.05);
            SplineMesh splineMesh = (SplineMesh)objInfo.getObject();
            Vec3 meshVerts[] = splineMesh.getVertexPositions();
            Vec3 translatedMeshVerts[] = new Vec3[meshVerts.length];
            for(int i = 0; i < meshVerts.length; i++){
                translatedMeshVerts[i] = new Vec3(meshVerts[i]);
            }
            
            int width = splineMesh.getUSize();
            
            // Translate points
            for(int m = 0; m < translatedMeshVerts.length; m++){
                Vec3 meshPoint = translatedMeshVerts[m];
                // Translate
                cs = ((ObjectInfo)objInfo).getCoords();
                Mat4 mat4 = cs.duplicate().fromLocal();
                mat4.transform(meshPoint);
                translatedMeshVerts[m] = meshPoint;
            }
            
            // what is the maximum distance given the mesh geometry to catch proximity of curve points.
            // we only want to move mesh points if they are close enought to curve geometry that is intended to be an influence.
            double maxProximityDistance = 1;
            double avgProximityDistance = 0;
            for(int m = 1; m < translatedMeshVerts.length; m++){
                Vec3 meshPointA = translatedMeshVerts[m-1];
                Vec3 meshPointB = translatedMeshVerts[m];
                double distance = meshPointA.distance(meshPointB);
                if(distance < maxProximityDistance){
                    maxProximityDistance = distance;
                }
                avgProximityDistance += distance;
            }
            avgProximityDistance = avgProximityDistance / translatedMeshVerts.length;
            //System.out.println("maxProximityDistance " + maxProximityDistance + " avg " + avgProximityDistance);
            
            //Vertex meshVerts[] = (Vertex [])objInfo.getVertices();
            //Edge profEdge[] = profile.getEdges();
            //Face profFace[] = profile.getFaces();
            
            for(int m = 0; m < translatedMeshVerts.length; m++){        // for each mesh vert
                Vec3 meshPoint = translatedMeshVerts[m];
            
                // find closest curve point on any of the child curves
                double closestDistance = 999;               // depricate
                Vec3 closestCurveVert = null;
            
                Vector<ObjectInfo> curves = new Vector<ObjectInfo>();
                
                // if no children found, iterate sibling curves.
                
                // children
                ObjectInfo[] children = objInfo.getChildren();
                for(int j = 0; j < children.length; j++){
                    ObjectInfo child = children[j];
                    if(child.getObject() instanceof Curve){
                        //Curve childCurve = (Curve)child.getObject();
                        curves.addElement(child);
                    }
                }
                if(curves.size() == 0){ // read siblings
                    ObjectInfo parent = objInfo.getParent();
                    if(parent != null){
                        children = parent.getChildren();
                        for(int j = 0; j < children.length; j++){
                            ObjectInfo child = children[j];
                            if(child.getObject() instanceof Curve){
                                //Curve childCurve = (Curve)child.getObject();
                                curves.addElement(child);
                            }
                        }
                    }
                }
                
                //for(int j = 0; j < children.length; j++){
                //    ObjectInfo child = children[j];
                for(int j = 0; j < curves.size(); j++){
                    ObjectInfo child = curves.elementAt(j);
                    
                    if(child.getObject() instanceof Curve &&
                       child.getObject() instanceof MirrorPlaneObject == false)
                    {
                        Curve childCurve = (Curve)child.getObject();
                        
                        // Only process if child is: 1) visible, 2) not disabled, 3) not hidden when children hidden.
                        if(child.isVisible() == true &&
                           child.isChildrenHiddenWhenHidden() == false &&
                           childCurve.isSupportMode() == false){
                        
                            //System.out.println(" child: " + child.getName());
                            
                            Curve subdiv;
                            subdiv = childCurve.subdivideCurve().subdivideCurve().subdivideCurve().subdivideCurve(); // subdivide curve for more points.
                            Vec3 [] subdividedVerts = subdiv.getVertexPositions();
                        
                            //double closestDistance = 999;               // depricate
                            //Vec3 closestCurveVert = null;
                            
                            for(int v = 0; v < subdividedVerts.length; v++){ // itererate subdivided curve
                                Vec3 curvePoint = new Vec3(subdividedVerts[v]);
        
                                // translate
                                cs = ((ObjectInfo)child).getCoords();
                                Mat4 mat4 = cs.duplicate().fromLocal();
                                mat4.transform(curvePoint);
                                
                                //System.out.println("   v: " + curvePoint.x + " " + curvePoint.y + " "+ curvePoint.z + " ");
                                double distance = meshPoint.distance(curvePoint);
                                //System.out.println("" );
                                if(distance < closestDistance){
                                    closestDistance = distance;
                                    closestCurveVert = curvePoint;
                                }
                            }
                            
                        }
                    }
                }
                if(curves.size() == 0){
                    //doundChildCurve = false;
                }
                
                
                // If closest curve point found modify mesh
                if(closestCurveVert != null){
                    
                    // meshVerts[m]
                    
                    //double currAdjacentDist = getPointAvgDist(objInfo, splineMesh, meshVerts, m);
                    double currAdjacentDist =  getPointMinDist(objInfo, splineMesh, meshVerts, m);
                    
                    // TODO:
                    //if(closestDistance < (maxProximityDistance / 2)){ // avgProximityDistance maxProximityDistance
                    //if(closestDistance < (maxProximityDistance )){
                    //if(closestDistance < (avgProximityDistance * 0.5)){
                    if ( closestDistance < (currAdjacentDist * 0.95) ) {
                        
                        
                        // delta vec are absolute scene values.
                        Vec3 delta = new Vec3(meshPoint.x - closestCurveVert.x, meshPoint.y - closestCurveVert.y, meshPoint.z - closestCurveVert.z);
                        //System.out.println(" delta " + m + " " + delta.x + " " + delta.y + " " + delta.z);
                        
                        // translate
                        cs = ((ObjectInfo)objInfo).getCoords().duplicate();
                        cs.setOrigin(new Vec3(0, 0, 0));
                        
                        Vec3 zd = cs.getZDirection();
                        //System.out.println("   zd " + m + " " + zd.x + " " + zd.y + " " + zd.z);
                        Vec3 ud = cs.getUpDirection();
                        //System.out.println("   ud " + m + " " + ud.x + " " + ud.y + " " + ud.z);
                        double rot[] = cs.getRotationAngles();
                        //System.out.println("   rot " + m + "  x " + rot[0] + " " + rot[1] + " " + rot[2]);
                        
                        // We need to reverse/invert the mesh object coordinates so that when
                        // the delta point is applied in reverse to the mesh coords it ends up being in the
                        // global scene coordinates correctly.
                        cs.setOrientation(-rot[0], -rot[1], -rot[2]);
                        
                        Mat4 mat4 = cs.duplicate().fromLocal();
                        mat4.transform(delta);
                        
                        Vec3 untranslatedMeshVec = meshVerts[m];
                        
                        untranslatedMeshVec.subtract(delta);
                        
                        meshVerts[m] = untranslatedMeshVec;
                        
                        foundSnapCurve = true;
                    }
                }
            }
            
            //sMesh.rebuild(newV,
            //              splineMesh.getUSmoothness(), splineMesh.getVSmoothness(),
            //              splineMesh.getSmoothingMethod(), splineMesh.isUClosed(), splineMesh.isVClosed());
            splineMesh.setVertexPositions(meshVerts);
            scene.objectModified(splineMesh);
            
            // If the mesh has a mirror plane, update the mirror copy.
            MirrorMesh mirror = new MirrorMesh();
            //mirror.setScene(scene); // Error
            mirror.update(objInfo);
            
            window.updateImage();
            
            conformSuccess = true;
        } else { // SplineMesh
            Sound.playSound("deny.wav");
            
            System.out.println("Error: Selected object is not a mesh type.");
            JOptionPane.showMessageDialog(null, "The selected object is not a mesh.",  "Error" , JOptionPane.ERROR_MESSAGE);
        }
        
        if(conformSuccess && foundSnapCurve){
            Sound.playSound("success.wav");
        } else {
            Sound.playSound("deny.wav");
        }
    }
    
    
    /**
     * getPointAvgDist
     *
     * Description: Get an average distance between points for a given point.
     * @param: ObjectInfo info.
     * @param: SplineMesh - mesh object with dimension information.
     * @param: Vec3[] mesh - point data.
     * @param: int m - current point index.
     */
    public double getPointAvgDist(ObjectInfo info, SplineMesh splineMesh, Vec3 meshVerts[], int m){
        double result = 0;
        int xWidth = splineMesh.getUSize();
        int yHeight = splineMesh.getVSize();
        int xIndex = (int)( m % splineMesh.getUSize() );
        int yIndex = (int)( m / splineMesh.getVSize() );
        Vec3 currVec = meshVerts[m];
        Vector<Vec3> adjacentPoints = new Vector<Vec3>();
        
        // Left
        if(xIndex > 0){
            Vec3 leftVec = meshVerts[m - 1];
            adjacentPoints.addElement(leftVec);
        }
        // Right
        if(xIndex < xWidth - 1){
            Vec3 rightVec = meshVerts[m + 1];
            adjacentPoints.addElement(rightVec);
        }
        // Upper
        if(yIndex > 0){
            Vec3 upperVec = meshVerts[m - xWidth];
            adjacentPoints.addElement(upperVec);
        }
        // Lower
        if(yIndex < yHeight - 1){
            Vec3 lowerVec = meshVerts[m + xWidth];
            adjacentPoints.addElement(lowerVec);
        }
        
        for(int i  = 0; i < adjacentPoints.size(); i++){
            Vec3 currAdjacentVec = adjacentPoints.elementAt(i);
            result += currVec.distance(currAdjacentVec);
        }
        
        if (adjacentPoints.size() > 0)
            result /= adjacentPoints.size();
        
        return result;
    }
    
    /**
     * getPointMinDist
     *
     * Description: Get the shortest adjacent point distance for a given point.
     * @param: ObjectInfo info.
     * @param: SplineMesh - mesh object with dimension information.
     * @param: Vec3[] mesh - point data.
     * @param: int m - current point index.
     */
    public double getPointMinDist(ObjectInfo info, SplineMesh splineMesh, Vec3 meshVerts[], int m){
        double result = 999999999;
        int xWidth = splineMesh.getUSize();
        int yHeight = splineMesh.getVSize();
        int xIndex = (int)( m % splineMesh.getUSize() );
        int yIndex = (int)( m / splineMesh.getVSize() );
        Vec3 currVec = meshVerts[m];
        Vector<Vec3> adjacentPoints = new Vector<Vec3>();
        
        // Left
        if(xIndex > 0){
            Vec3 leftVec = meshVerts[m - 1];
            adjacentPoints.addElement(leftVec);
        }
        // Right
        if(xIndex < xWidth - 1){
            Vec3 rightVec = meshVerts[m + 1];
            adjacentPoints.addElement(rightVec);
        }
        // Upper
        if(yIndex > 0){
            Vec3 upperVec = meshVerts[m - xWidth];
            adjacentPoints.addElement(upperVec);
        }
        // Lower
        if(yIndex < yHeight - 1){
            Vec3 lowerVec = meshVerts[m + xWidth];
            adjacentPoints.addElement(lowerVec);
        }
        
        for(int i  = 0; i < adjacentPoints.size(); i++){
            Vec3 currAdjacentVec = adjacentPoints.elementAt(i);
            double currDistance = currVec.distance(currAdjacentVec);
            if( currDistance < result ){
                result = currDistance;
            }
        }

        return result;
    }
    
    
    
    /**
     * conformCurveToCurves
     *
     * Description: Modify a selected curve to snap points to close by curve points.
     *
     * Param: Scene - reference to world objects and selection.
     */
    public void conformCurveToCurves(Scene scene){
        boolean conformSuccess = false;
        boolean foundSnapToCurve = false;
        System.out.println("conformCurveToCurves");
        int selection[] = window.getSelectedIndices();
        
        if(selection.length != 1){
            
            Sound.playSound("deny.wav");
            System.out.println("Error: Select a curve to conform.");
            
            JOptionPane.showMessageDialog(null, "Select a curve to conform.",  "Error" , JOptionPane.ERROR_MESSAGE );
            return;
        }
        
        ObjectInfo objInfo = (ObjectInfo) scene.getObject(selection[0]);
        System.out.println("objInfo: " + objInfo.getName());
        CoordinateSystem cs = ((ObjectInfo)objInfo).getCoords();
        
        
        if(objInfo.getObject() instanceof Curve){
            System.out.println("Curve Object selected");
            
            Curve curve = (Curve)objInfo.getObject();
            Vec3 meshVerts[] = curve.getVertexPositions();
            Vec3 translatedMeshVerts[] = new Vec3[meshVerts.length];
            for(int i = 0; i < meshVerts.length; i++){
                translatedMeshVerts[i] = new Vec3(meshVerts[i]);
            }
            // Translate points
            for(int m = 0; m < translatedMeshVerts.length; m++){
                Vec3 meshPoint = translatedMeshVerts[m];
                // Translate
                cs = ((ObjectInfo)objInfo).getCoords();
                Mat4 mat4 = cs.duplicate().fromLocal();
                mat4.transform(meshPoint);
                translatedMeshVerts[m] = meshPoint;
            }
            
            // what is the maximum distance given the mesh geometry to catch proximity of curve points.
            // we only want to move mesh points if they are close enought to curve geometry that is intended to be an influence.
            double maxProximityDistance = 1;
            double avgProximityDistance = 0;
            for(int m = 1; m < translatedMeshVerts.length; m++){
                Vec3 meshPointA = translatedMeshVerts[m-1];
                Vec3 meshPointB = translatedMeshVerts[m];
                double distance = meshPointA.distance(meshPointB);
                if(distance < maxProximityDistance){
                    maxProximityDistance = distance;
                }
                avgProximityDistance += distance;
            }
            avgProximityDistance = avgProximityDistance / translatedMeshVerts.length;
            System.out.println("maxProximityDistance " + maxProximityDistance + " avg " + avgProximityDistance);
            
            
            for(int m = 0; m < translatedMeshVerts.length; m++){        // for each mesh vert
                Vec3 meshPoint = translatedMeshVerts[m];
                
                Vec3 adjacentMeshPointVector = null;
                if(m == 0){
                    Vec3 adjacentMeshPoint = new Vec3(translatedMeshVerts[m + 1]);
                    adjacentMeshPointVector = new Vec3(meshPoint.x - adjacentMeshPoint.x,
                                                       meshPoint.y - adjacentMeshPoint.y,
                                                       meshPoint.z - adjacentMeshPoint.z);
                } else { // if(m = translatedMeshVerts.length - 1)
                    Vec3 adjacentMeshPoint = new Vec3(translatedMeshVerts[m - 1]);
                    adjacentMeshPointVector = new Vec3(meshPoint.x - adjacentMeshPoint.x,
                                                       meshPoint.y - adjacentMeshPoint.y,
                                                       meshPoint.z - adjacentMeshPoint.z);
                }
                
                // find closest curve point on any of the child curves
                double closestDistance = 999;               // depricate
                Vec3 closestCurveVert = null;
                Vec3 closestCurveVector = null; // direction of curve segment.
                double aDot = 0;
                
                Vector<ObjectInfo> curves = new Vector<ObjectInfo>();
                
                // if no children found, iterate sibling curves.
                
                // children
                ObjectInfo[] children = objInfo.getChildren();
                for(int j = 0; j < children.length; j++){
                    ObjectInfo child = children[j];
                    if(child.getObject() instanceof Curve){
                        //Curve childCurve = (Curve)child.getObject();
                        if(((Curve)child.getObject()).isSupportMode() == false){
                            curves.addElement(child);
                        }
                    }
                }
                if(curves.size() == 0){ // read siblings
                    /*
                     ObjectInfo parent = objInfo.getParent();
                     if(parent != null){
                     children = parent.getChildren();
                     for(int j = 0; j < children.length; j++){
                     ObjectInfo child = children[j];
                     if(child.getObject() instanceof Curve){
                     //Curve childCurve = (Curve)child.getObject();
                     curves.addElement(child);
                     }
                     }
                     }
                     */
                    // Read all scene curves
                    for (ObjectInfo obj : scene.getObjects()){
                        if(obj.getObject() instanceof Curve){
                            if(obj.getId() != objInfo.getId() ){ // && ((Curve)obj.getObject()).isSupportMode() == false // (Use angle instead of obj type.)
                                curves.addElement(obj);
                            }
                        }
                    }
                }
                //System.out.println("curves.size() " + curves.size());
                
                //for(int j = 0; j < children.length; j++){
                //    ObjectInfo child = children[j];
                for(int j = 0; j < curves.size(); j++){
                    ObjectInfo child = curves.elementAt(j);
                    
                    if(child.getObject() instanceof Curve){
                        Curve childCurve = (Curve)child.getObject();
                        
                        // Only process if child is: 1) visible, 2) not disabled, 3) not hidden when children hidden.
                        if(child.isVisible() == true &&
                           child.isChildrenHiddenWhenHidden() == false
                           //&& childCurve.isSupportMode() == false // (Use angle now)
                           ){
                            
                            //System.out.println(" child: " + child.getName());
                            
                            Curve subdiv;
                            subdiv = childCurve.subdivideCurve().subdivideCurve().subdivideCurve().subdivideCurve(); // subdivide curve for more points.
                            Vec3 [] subdividedVerts = subdiv.getVertexPositions();
                            
                            //double closestDistance = 999;               // depricate
                            //Vec3 closestCurveVert = null;
                            
                            for(int v = 0; v < subdividedVerts.length; v++){ // itererate subdivided curve
                                Vec3 curvePoint = new Vec3(subdividedVerts[v]);
                                
                                // translate
                                cs = ((ObjectInfo)child).getCoords();
                                Mat4 mat4 = cs.duplicate().fromLocal();
                                mat4.transform(curvePoint);
                                
                                //System.out.println("   v: " + curvePoint.x + " " + curvePoint.y + " "+ curvePoint.z + " ");
                                double distance = meshPoint.distance(curvePoint);
                                //System.out.println("" );
                                if(distance < closestDistance){
                                    
                                    // closestCurveVector
                                    if(v == 0){
                                        Vec3 adjacentCurvePoint = new Vec3(subdividedVerts[v + 1]);
                                        mat4.transform(adjacentCurvePoint);
                                        closestCurveVector = new Vec3( curvePoint.x - adjacentCurvePoint.x,
                                                                      curvePoint.y - adjacentCurvePoint.y,
                                                                      curvePoint.z - adjacentCurvePoint.z );
                                    } else { // if(v == subdividedVerts.length -1)
                                        Vec3 adjacentCurvePoint = new Vec3(subdividedVerts[v - 1]);
                                        mat4.transform(adjacentCurvePoint);
                                        closestCurveVector = new Vec3( curvePoint.x - adjacentCurvePoint.x,
                                                                      curvePoint.y - adjacentCurvePoint.y,
                                                                      curvePoint.z - adjacentCurvePoint.z );
                                        // minus
                                    }
                                    //double angle = adjacentMeshPointVector.getAngle(closestCurveVector);
                                    
                                    adjacentMeshPointVector.normalize();
                                    closestCurveVector.normalize();
                                    aDot = adjacentMeshPointVector.dot(closestCurveVector);
                                    //System.out.println("aDot " +aDot );
                                    double a1 = Math.acos(aDot);
                                    double a2 = Math.cos(aDot);
                                    double degrees = Math.toDegrees(a1);
                                    if(degrees > 90){
                                        degrees = 90 - (degrees - 90);
                                    }
                                    if(degrees > 15){ // 15 degrees is just a magic number.
                                        closestDistance = distance;
                                        closestCurveVert = curvePoint;
                                        
                                        //foundSnapToCurve = true;
                                    } else {
                                        //System.out.println(" Ignoring point because of angle. ");
                                    }
                                }
                            }
                            
                        }
                    }
                }
                
                //System.out.println(" aDot " +aDot );
                //double a1 = Math.acos(aDot);
                //double a2 = Math.cos(aDot);
                //double degrees = Math.toDegrees(a1);
                //if(degrees > 90){
                //    degrees = 90 - (degrees - 90);
                //}
                
                //System.out.println(" curve angle: " + degrees + "  2 " +  Math.toDegrees(a2) );
                
                
                // If closest curve point found modify mesh
                if(closestCurveVert != null){
                    
                    // TODO:
                    //if(closestDistance < (maxProximityDistance / 2)){ // avgProximityDistance maxProximityDistance
                    //if(closestDistance < (maxProximityDistance )){
                    if(closestDistance < (avgProximityDistance * 0.2)){
                        
                        // delta vec are absolute scene values.
                        Vec3 delta = new Vec3(meshPoint.x - closestCurveVert.x, meshPoint.y - closestCurveVert.y, meshPoint.z - closestCurveVert.z);
                        //System.out.println(" delta " + m + " " + delta.x + " " + delta.y + " " + delta.z);
                        
                        // translate
                        cs = ((ObjectInfo)objInfo).getCoords().duplicate();
                        cs.setOrigin(new Vec3(0, 0, 0));
                        
                        Vec3 zd = cs.getZDirection();
                        //System.out.println("   zd " + m + " " + zd.x + " " + zd.y + " " + zd.z);
                        Vec3 ud = cs.getUpDirection();
                        //System.out.println("   ud " + m + " " + ud.x + " " + ud.y + " " + ud.z);
                        double rot[] = cs.getRotationAngles();
                        //System.out.println("   rot " + m + "  x " + rot[0] + " " + rot[1] + " " + rot[2]);
                        
                        // We need to reverse/invert the mesh object coordinates so that when
                        // the delta point is applied in reverse to the mesh coords it ends up being in the
                        // global scene coordinates correctly.
                        cs.setOrientation(-rot[0], -rot[1], -rot[2]);
                        
                        Mat4 mat4 = cs.duplicate().fromLocal();
                        mat4.transform(delta);
                        
                        Vec3 untranslatedMeshVec = meshVerts[m];
                        
                        untranslatedMeshVec.subtract(delta);
                        
                        meshVerts[m] = untranslatedMeshVec;
                        
                        foundSnapToCurve = true;
                    }
                }
            }
            
            //sMesh.rebuild(newV,
            //              splineMesh.getUSmoothness(), splineMesh.getVSmoothness(),
            //              splineMesh.getSmoothingMethod(), splineMesh.isUClosed(), splineMesh.isVClosed());
            curve.setVertexPositions(meshVerts);
            //splineMesh.setVertexPositions(meshVerts);
            scene.objectModified(curve);
            window.updateImage();
            conformSuccess = true;
            
        } else { // type is Curve
            Sound.playSound("deny.wav");
            System.out.println("Error: Selected object is not a curve type.");
            JOptionPane.showMessageDialog(null, "The selected object is not a Curve.",  "Error" , JOptionPane.ERROR_MESSAGE);
        }
        if(conformSuccess && foundSnapToCurve){
            Sound.playSound("success.wav");
        } else {
            Sound.playSound("deny.wav");
        }
    }
    
    
    
    
    /*
     public void conformMeshToCurves(Scene scene){
         // If a curve is selected, merge verts with a larger proximity range
         // If no objects selected, merge verts with curves using a smaller proximity range
         
         int selection[] = window.getSelectedIndices();
         ObjectInfo objInfo = (ObjectInfo) scene.getObject(selection[0]);
         System.out.println("objInfo: " + objInfo.getName());
         CoordinateSystem cs = ((ObjectInfo)objInfo).getCoords();
         
         //TriangleMesh tm = null;
         if(objInfo.getObject() instanceof SplineMesh){ //
             //tm = objInfo.getObject().convertToTriangleMesh(0.05);
             SplineMesh splineMesh = (SplineMesh)objInfo.getObject();
             Vec3 meshVerts[] = splineMesh.getVertexPositions();
             Vec3 translatedMeshVerts[] = new Vec3[meshVerts.length];
             for(int i = 0; i < meshVerts.length; i++){
                 translatedMeshVerts[i] = new Vec3(meshVerts[i]);
             }
             
             int width = splineMesh.getUSize();
             
             // Translate points
             for(int m = 0; m < translatedMeshVerts.length; m++){
                 Vec3 meshPoint = translatedMeshVerts[m];
                 // Translate
                 cs = ((ObjectInfo)objInfo).getCoords();
                 Mat4 mat4 = cs.duplicate().fromLocal();
                 mat4.transform(meshPoint);
                 translatedMeshVerts[m] = meshPoint;
             }
             
             // what is the maximum distance given the mesh geometry to catch proximity of curve points.
             // we only want to move mesh points if they are close enought to curve geometry that is intended to be an influence.
             double maxProximityDistance = 1;
             double avgProximityDistance = 0;
             for(int m = 1; m < translatedMeshVerts.length; m++){
                 Vec3 meshPointA = translatedMeshVerts[m-1];
                 Vec3 meshPointB = translatedMeshVerts[m];
                 double distance = meshPointA.distance(meshPointB);
                 if(distance < maxProximityDistance){
                     maxProximityDistance = distance;
                 }
                 avgProximityDistance += distance;
             }
             avgProximityDistance = avgProximityDistance / translatedMeshVerts.length;
             System.out.println("maxProximityDistance " + maxProximityDistance + " avg " + avgProximityDistance);
             
             //Vertex meshVerts[] = (Vertex [])objInfo.getVertices();
             //Edge profEdge[] = profile.getEdges();
             //Face profFace[] = profile.getFaces();
             
             // find closest curve point on any of the child curves
             double closestDistance = 999;               // depricate
             Vec3 closestCurveVert = null;
             
             
             // children
             ObjectInfo[] children = objInfo.getChildren();
             for(int j = 0; j < children.length; j++){
                 ObjectInfo child = children[j];
                 
                 if(child.getObject() instanceof Curve){
                     Curve childCurve = (Curve)child.getObject();
                     
                     // Only process if child is: 1) visible, 2) not disabled, 3) not hidden when children hidden.
                     if(child.isVisible() == true &&
                        child.isChildrenHiddenWhenHidden() == false &&
                        childCurve.isSupportMode() == false){
                     
                         System.out.println(" child: " + child.getName());
                         
                         Curve subdiv;
                         subdiv = childCurve.subdivideCurve().subdivideCurve().subdivideCurve(); // subdivide curve for more points.
                         Vec3 [] subdividedVerts = subdiv.getVertexPositions();
                         
                         for(int m = 0; m < translatedMeshVerts.length; m++){
                             Vec3 meshPoint = translatedMeshVerts[m];
                             
                             double closestDistance = 999;               // depricate
                             Vec3 closestCurveVert = null;
                             
                             for(int v = 0; v < subdividedVerts.length; v++){ // itererate subdivided curve
                                 Vec3 curvePoint = new Vec3(subdividedVerts[v]);
         
                                 // translate
                                 cs = ((ObjectInfo)child).getCoords();
                                 Mat4 mat4 = cs.duplicate().fromLocal();
                                 mat4.transform(curvePoint);
                                 
                                 //System.out.println("   v: " + curvePoint.x + " " + curvePoint.y + " "+ curvePoint.z + " ");
                                 double distance = meshPoint.distance(curvePoint);
                                 //System.out.println("" );
                                 if(distance < closestDistance){
                                     closestDistance = distance;
                                     closestCurveVert = curvePoint;
                                 }
                             }
                             
                             if(closestCurveVert != null){
                                 
                                 if(closestDistance < (maxProximityDistance / 2)){ // avgProximityDistance maxProximityDistance
                                     
                                     // delta vec are absolute scene values.
                                     Vec3 delta = new Vec3(meshPoint.x - closestCurveVert.x, meshPoint.y - closestCurveVert.y, meshPoint.z - closestCurveVert.z);
                                     //System.out.println(" delta " + m + " " + delta.x + " " + delta.y + " " + delta.z);
                                     
                                     // translate
                                     cs = ((ObjectInfo)objInfo).getCoords().duplicate();
                                     cs.setOrigin(new Vec3(0, 0, 0));
                                     
                                     Vec3 zd = cs.getZDirection();
                                     //System.out.println("   zd " + m + " " + zd.x + " " + zd.y + " " + zd.z);
                                     Vec3 ud = cs.getUpDirection();
                                     //System.out.println("   ud " + m + " " + ud.x + " " + ud.y + " " + ud.z);
                                     double rot[] =  cs.getRotationAngles();
                                     //System.out.println("   rot " + m + "  x " + rot[0] + " " + rot[1] + " " + rot[2]);
                                     
                                     // We need to reverse/invert the mesh object coordinates so that when
                                     // the delta point is applied in reverse to the mesh coords it ends up being in the
                                     // global scene coordinates correctly.
                                     cs.setOrientation(-rot[0], -rot[1], -rot[2]);
                                     
                                     Mat4 mat4 = cs.duplicate().fromLocal();
                                     mat4.transform(delta);
                                     
                                     Vec3 untranslatedMeshVec = meshVerts[m];
                                     
                                     untranslatedMeshVec.subtract(delta);
                                     
                                     meshVerts[m] = untranslatedMeshVec;
                                 }
                             }
                         }
                     }
                 }
             }
             
             // If closest curve point found modify mesh
             
             //sMesh.rebuild(newV,
             //              splineMesh.getUSmoothness(), splineMesh.getVSmoothness(),
             //              splineMesh.getSmoothingMethod(), splineMesh.isUClosed(), splineMesh.isVClosed());
             splineMesh.setVertexPositions(meshVerts);
             scene.objectModified(splineMesh);
             window.updateImage();
         }
     }
     **/
    
    
    /**
     * conformCurveToMesh
     *
     * Description: Modify a selected curve object to conform to a sibling mesh object.
     * TODO: Only snap if points are close otherwise they can't be evenly maped.
     *
     *  @param: Scene - access to scene object.
     */
    public void conformCurveToMesh(Scene scene, ObjectInfo selectedCurve){
        boolean conformSuccess = false;
        boolean foundSnapToCurve = false;
        System.out.println("conformCurveToCurves");
        int selection[] = window.getSelectedIndices();
        
        //if(selection.length != 1){
        //    Sound.playSound("deny.wav");
        //    System.out.println("Error: Select a curve to conform.");
        //    JOptionPane.showMessageDialog(null, "Select a curve to conform.",  "Error" , JOptionPane.ERROR_MESSAGE );
        //    return;
        //}
        
        // Get curve and conform mesh objects.
        //ObjectInfo selectedCurve = null;
        ObjectInfo selectedMesh = null;
        for(int i = 0; i < selection.length; i++){
            ObjectInfo currInfo = (ObjectInfo)scene.getObject(selection[i]);
            if(currInfo.getObject() instanceof Curve){
                selectedCurve = currInfo;
            }
            if(currInfo.getObject() instanceof Mesh &&
               currInfo.getObject() instanceof Curve == false &&
               currInfo.getObject() instanceof ArcObject == false){
                selectedMesh = currInfo;
            }
        }
        
        // If mesh not selected, seach for it in curve children.
        // Depricate this
        /*
        if(selectedCurve != null && selectedMesh == null){
            ObjectInfo parent = selectedCurve.getParent();
            if(parent != null){
                ObjectInfo[] children = parent.getChildren();
                for(int j = 0; j < children.length; j++){
                    ObjectInfo child = children[j];
                    if(child.getObject() instanceof Mesh &&
                       child.getObject() instanceof Curve == false &&
                       child.getObject() instanceof ArcObject == false){
                        selectedMesh = child;
                    }
                }
            }
        }
         */
        
        // if selectedMesh is null, find a suitable mesh from the scene.
        if(selectedCurve != null && selectedMesh == null){
            CoordinateSystem curveCs = ((ObjectInfo)selectedCurve).getCoords();
            BoundingBox curveBound = selectedCurve.getTranslatedBounds();
            Vec3 curveCenter = curveBound.getCenter();
            Vector<ObjectInfo> sceneObjects = scene.getObjects();
            double closestDistance = Double.MAX_VALUE;
            ObjectInfo closestObject = null;
            for(int i = 0; i < sceneObjects.size(); i++){
                ObjectInfo currObject = sceneObjects.elementAt(i);
                CoordinateSystem currCs = ((ObjectInfo)currObject).getCoords();
                Mat4 mat4 = currCs.duplicate().fromLocal();
                if(currObject.getObject() instanceof Mesh &&
                   currObject.getObject() instanceof Curve == false &&
                   currObject.getObject() instanceof ArcObject == false)
                {
                    Mesh currMesh = (Mesh)currObject.getObject();
                    MeshVertex[] verts = currMesh.getVertices();
                    for(int v = 0; v < verts.length; v++){
                        Vec3 currPoint = new Vec3(verts[v].r);
                        mat4.transform(currPoint);
                        double distance = curveCenter.distance(currPoint);
                        if(distance < closestDistance){
                            closestObject = currObject;
                            closestDistance = distance;
                        }
                    }
                }
            }
            if(closestObject != null){
                
                
                selectedMesh = closestObject;
            }
        }
        
        // Alert
        if(selectedCurve == null){
            Sound.playSound("deny.wav");
            System.out.println("Error: Select a curve to conform.");
            JOptionPane.showMessageDialog(null, "Select a curve to conform.",  "Error" , JOptionPane.ERROR_MESSAGE );
            return;
        }
        if(selectedMesh == null){
            Sound.playSound("deny.wav");
            System.out.println("Error: Select a mesh to conform to.");
            JOptionPane.showMessageDialog(null, "Could not find mesh to conform to. Try again by also selecting a mesh object.",  "Error" , JOptionPane.ERROR_MESSAGE );
            return;
        }
        
        //ObjectInfo objInfo = (ObjectInfo) scene.getObject(selection[0]);
        //System.out.println("objInfo: " + objInfo.getName());
        CoordinateSystem cs = ((ObjectInfo)selectedCurve).getCoords();
        
        //if(objInfo.getObject() instanceof Curve){
        if(selectedCurve != null && selectedMesh != null ){
            //System.out.println("Curve Object selected");
            
            Curve curve = (Curve)selectedCurve.getObject();
            Vec3 curveVerts[] = curve.getVertexPositions();
            Vec3 translatedCurveVerts[] = new Vec3[curveVerts.length]; // curve
            for(int i = 0; i < curveVerts.length; i++){
                translatedCurveVerts[i] = new Vec3(curveVerts[i]);
            }
            // Translate points
            for(int m = 0; m < translatedCurveVerts.length; m++){
                Vec3 meshPoint = translatedCurveVerts[m];
                // Translate
                cs = ((ObjectInfo)selectedCurve).getCoords();
                Mat4 mat4 = cs.duplicate().fromLocal();
                mat4.transform(meshPoint);
                translatedCurveVerts[m] = meshPoint;
            }
            
            BoundingBox curveBound = selectedCurve.getTranslatedBounds();
            double conformLimit = 0;
            
            // Find Mesh object to snap to.
            //ObjectInfo parent = objInfo.getParent();
            //if(parent != null){
            //    ObjectInfo[] children = parent.getChildren();
            //    for(int j = 0; j < children.length; j++){
            //        ObjectInfo child = children[j];
            //        if(child.getObject() instanceof Mesh &&
            //           child.getObject() instanceof Curve == false &&
            //           child.getObject() instanceof ArcObject == false){
            
                        //System.out.println("     child: " + child.getName()  );
                        
                        CoordinateSystem meshCs = ((ObjectInfo)selectedMesh).getCoords();
                        
                        Mesh conformMesh = (Mesh)selectedMesh.getObject();
                        Vec3 meshVerts[] = conformMesh.getVertexPositions();
                        Vec3 translatedMeshVerts[] = new Vec3[meshVerts.length]; // mesh
                        for(int i = 0; i < meshVerts.length; i++){
                            translatedMeshVerts[i] = new Vec3(meshVerts[i]);
                            // Translate
                            Mat4 mat4 = meshCs.duplicate().fromLocal();
                            mat4.transform(translatedMeshVerts[i]);
                            // for curve point find closest mesh point
                        }
                        
                        boolean modified = false;
                        for(int c = 1; c < translatedCurveVerts.length - 1; c++){ // For each point in the curve
                            Vec3 curvePoint = translatedCurveVerts[c];
                            double currCurvePointClosestDist = 999999;
                            boolean found = false;
                            Vec3 foundVec = null;
                            for(int m = 0; m < translatedMeshVerts.length; m++){    // For each point in the mesh
                                Vec3 meshPoint = translatedMeshVerts[m];
                                
                                double distance = curvePoint.distance(meshPoint);
                                if(distance < currCurvePointClosestDist){
                                    currCurvePointClosestDist = distance;
                                    found = true;
                                    foundVec = meshPoint;
                                }
                            }
                            // Move
                            if(found && foundVec != null){
                                // Update curve geometry.
                                translatedCurveVerts[c] = foundVec;
                                modified = true;
                            }
                        }
                        
                        if(modified){
                            // Set Curve Coordinate system to zero
                            CoordinateSystem csZero = new CoordinateSystem();
                            selectedCurve.setCoords(csZero);
                            curve.setVertexPositions(translatedCurveVerts);
                            selectedCurve.clearCachedMeshes();
                            window.updateImage();
                            
                            // Success
                            Sound.playSound("success.wav");
                        } else {
                            // Error
                            Sound.playSound("deny.wav");
                        }
                    //}
                //}
            //}
        } else {
            Sound.playSound("deny.wav");
        }
    }
}
