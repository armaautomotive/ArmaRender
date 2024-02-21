/* Copyright (C) 2020-2021 by Jon Taylor

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

public class Perferate {
    private LayoutWindow window = null;

    Perferate(Scene scene, LayoutWindow window){
        this.window = window;
    }
    
    /**
     * isCloseToPoints
     *
     * Description: check if a point is within a given distance to a list of points.
     */
    public boolean isCloseToPoints(Vec3 point, Vector <Vec3>points, double distance){
        for(int i = 0; i < points.size(); i++){
            Vec3 comparePoint = points.elementAt(i);
            if(point.distance(comparePoint) < distance){
                return true;
            }
        }
        return false;
    }
    
    /**
     * removeExistingPerferations
     *
     *
     */
    public void removeExistingPerferations(ObjectInfo info){
        
    }
    
    /**
     * perferateTriangles
     *
     * Description: Given an object selection, generate curve geometry representing cut lines to perferate the part with holes.
     * @param - Scene : object containing world objects.
     * @param - double percentage of material to remove.
     */
    public void perferateTriangles(Scene scene){
        LayoutModeling layout = new LayoutModeling();
        int selection[] = scene.getSelection();
        if(selection.length > 0){
            ObjectInfo info = scene.getObject(selection[0]);
            
            System.out.println("obj " + info);
            Object co = (Object)info.getObject();
            if((co instanceof Mesh) == true){
            
                // Find object orientation
                
                BoundingBox bounds = getTranslatedBounds(info);
                System.out.println("bounds " + bounds.minx + " " + bounds.maxx + " " +
                                   bounds.miny + " " + bounds.maxy + " " +
                                   bounds.minz + " " + bounds.maxz + " " );
                
                // What if part is not aligned to primary axis???
                // todo sort out
                
                // Collect child verticies and avoid pererating near them.
                Vector childVerts = new Vector();
                ObjectInfo[] children = info.getChildren();
                for(int i = 0; i < children.length; i++){
                    ObjectInfo child = children[i];
                    Object childCo = (Object)child.getObject();
                    if((childCo instanceof Curve) == true){
                        CoordinateSystem c;
                        c = layout.getCoords(child);
                        //Vec3 objOrigin = c.getOrigin();
                        
                        //System.out.println("Curve");
                        Mesh mesh = (Mesh) child.getObject(); // Object3D
                        Vec3 [] verts = mesh.getVertexPositions();
                        for (Vec3 vert : verts){
                            
                            Mat4 mat4 = c.duplicate().fromLocal();
                            mat4.transform(vert);
                            
                            //System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                            childVerts.addElement(vert);
                        }
                    }
                }
                
                
                // X Axis
                if( (bounds.maxx - bounds.minx) > (bounds.maxy - bounds.miny) &&
                     (bounds.maxx - bounds.minx) > (bounds.maxz - bounds.minz)   ){ // X axis
                    System.out.println(" is on x ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int x_index = 0; // bounds.minx;
                    double unit_distance = (bounds.maxz - bounds.minz) / 5.5; // testing for now
                    //System.out.println("unit_distance " + unit_distance); // 0.36363636363636365
                    double partDiameter = (bounds.maxy - bounds.miny);
                    double partRadius = (partDiameter / 2);
                    double partRadiusExpand = partRadius * 1.01; // expand for visibility.
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                    
                    //for(int x_i = 0; x_i < ((bounds.maxx - bounds.minx) / unit_distance); x_i ++){ // x unit_distance
                    for(int x_i = 0; x_i < ((bounds.maxx - bounds.minx) / unit_distance); x_i++){
                        double xRow =  ( x_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        
                        double rShift = 0; // rotate to evenly pack
                        if(x_i % 4 == 2 || x_i % 4 == 3){ //
                            rShift = Math.toRadians( (360.0 / ((double)rUnits)) / 2 );
                        }
                        double xShift = 0;  // Shift to evenly pack
                        if(x_i % 4 == 1){
                            xShift = (unit_distance / 7.0); // move right
                        } else if (x_i % 4 == 2){
                            xShift = -(unit_distance / 7.0); // move left
                        }
                        if(x_i % 4 == 1){
                            xShift += (unit_distance / 24.0);
                        }
                        if(x_i % 4 == 2){
                            xShift += ((unit_distance / 24.0) * 2);
                            xShift += ((unit_distance / 24.0) );
                        }
                        if(x_i % 4 == 3){
                            xShift += ((unit_distance / 24.0) * 3);
                            xShift += ((unit_distance / 24.0) );
                        }
                        
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            
                            // Entry
                            double xPos = bounds.minx + xRow + (unit_distance / 4) + xShift;
                            double yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.15)) + yCentre;
                            double zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.15) + zCentre;
                            Vec3 entryPoint = new Vec3(xPos, yPos, zPos);
                            
                            // 1
                            xPos = bounds.minx + xRow + xShift;
                            yPos = bounds.miny + (partRadiusExpand * Math.cos(angle)) + yCentre;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle) + zCentre;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.minx + xRow + xShift;
                            yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.25)) + yCentre;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.25) + zCentre;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.minx + xRow + (unit_distance / 1.65) + xShift;
                            yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.125)) + yCentre;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.125) + zCentre;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            // Close Point
                            xPos = bounds.minx + xRow + xShift;
                            yPos = bounds.miny + (partRadiusExpand * Math.cos(angle)) + yCentre;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle) + zCentre;
                            Vec3 closePoint = new Vec3(xPos, yPos, zPos);
                            
                            // Exit point
                            xPos = bounds.minx + xRow + (unit_distance / 3) + xShift;
                            yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.10)) + yCentre;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.10) + zCentre;
                            Vec3 exitPoint = new Vec3(xPos, yPos, zPos);
                            
                            if(x_i % 2 == 0){
                                xPos = bounds.minx + xRow + (unit_distance / 1.65) + xShift;
                                yPos = bounds.miny + (partRadiusExpand * Math.cos(angle)) + yCentre;
                                zPos = bounds.minz + partRadiusExpand * Math.sin(angle) + zCentre;
                                point1 = new Vec3(xPos, yPos, zPos);
                                
                                xPos = bounds.minx + xRow + (unit_distance / 1.65) + xShift;
                                yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.25)) + yCentre;
                                zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.25) + zCentre;
                                point2 = new Vec3(xPos, yPos, zPos);
                                
                                xPos = bounds.minx + xRow + xShift;
                                yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.125)) + yCentre;
                                zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.125) + zCentre;
                                point3 = new Vec3(xPos, yPos, zPos);
                                
                                xPos = bounds.minx + xRow + (unit_distance / 1.65) + xShift;
                                yPos = bounds.miny + (partRadiusExpand * Math.cos(angle)) + yCentre;
                                zPos = bounds.minz + partRadiusExpand * Math.sin(angle) + zCentre;
                                closePoint = new Vec3(xPos, yPos, zPos);
                            }
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                            
                                float[] s_ = new float[6]; s_[0] = 0; s_[1] = 0; s_[2] = 0;
                                Vec3[] vertex = new Vec3[6];
                                vertex[0] = entryPoint;
                                vertex[1] = point1;
                                vertex[2] = point2;
                                vertex[3] = point3;
                                vertex[4] = closePoint;
                                vertex[5] = exitPoint;
                                
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                                //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                            }
                        }
                        if(x_i % 6 == 0){   // Show intermittant progress
                            window.updateImage();
                        }
                    }
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                } // end of X axis
                
                
                // Y axis
                if( (bounds.maxx - bounds.minx) < (bounds.maxy - bounds.miny) &&
                     (bounds.maxy - bounds.miny) > (bounds.maxz - bounds.minz)   ){ // Y axis
                    System.out.println(" is on y ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int x_index = 0; // bounds.minx;
                    double unit_distance = (bounds.maxz - bounds.minz) / 5.5; // testing for now
                    //System.out.println("unit_distance " + unit_distance); // 0.36363636363636365
                    double partDiameter = (bounds.maxx - bounds.minx);
                    double partRadius = (partDiameter / 2);
                    double partRadiusExpand = partRadius * 1.01; // expand for visibility.
                    double xCentre = ((bounds.maxx - bounds.minx) / 2);
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                    
                    for(int y_i = 0; y_i < ((bounds.maxy - bounds.miny) / unit_distance); y_i++){
                        double yRow =  (y_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        
                        double rShift = 0; // rotate to evenly pack
                        if(y_i % 4 == 2 || y_i % 4 == 3){ //
                            rShift = Math.toRadians( (360.0 / ((double)rUnits)) / 2 );
                        }
                        double yShift = 0;  // Shift to evenly pack
                        if(y_i % 4 == 1){
                            yShift = (unit_distance / 7.0); // move right
                        } else if (y_i % 4 == 2){
                            yShift = -(unit_distance / 7.0); // move left
                        }
                        if(y_i % 4 == 1){
                            yShift += (unit_distance / 24.0);
                        }
                        if(y_i % 4 == 2){
                            yShift += ((unit_distance / 24.0) * 2);
                            yShift += ((unit_distance / 24.0) );
                        }
                        if(y_i % 4 == 3){
                            yShift += ((unit_distance / 24.0) * 3);
                            yShift += ((unit_distance / 24.0) );
                        }
                        
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            
                            // 1
                            double xPos = bounds.minx + (partRadiusExpand * Math.cos(angle)) + xCentre;
                            double yPos = bounds.miny + yRow + yShift;
                            double zPos = bounds.minz + partRadiusExpand * Math.sin(angle) + zCentre;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.minx + (partRadiusExpand * Math.cos(angle + 0.25)) + xCentre;
                            yPos = bounds.miny + yRow + yShift;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.25) + zCentre;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.minx + (partRadiusExpand * Math.cos(angle + 0.125)) + xCentre;
                            yPos = bounds.miny + yRow + (unit_distance / 1.65) + yShift;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.125) + zCentre;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            if(y_i % 2 == 0){
                                xPos = bounds.minx + (partRadiusExpand * Math.cos(angle)) + xCentre;
                                yPos = bounds.miny + yRow + (unit_distance / 1.65) + yShift;
                                zPos = bounds.minz + partRadiusExpand * Math.sin(angle) + zCentre;
                                point1 = new Vec3(xPos, yPos, zPos);
                                
                                xPos = bounds.minx + (partRadiusExpand * Math.cos(angle + 0.25)) + xCentre;
                                yPos = bounds.miny + yRow + (unit_distance / 1.65) + yShift;
                                zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.25) + zCentre;
                                point2 = new Vec3(xPos, yPos, zPos);
                                
                                xPos = bounds.minx + (partRadiusExpand * Math.cos(angle + 0.125)) + xCentre;
                                yPos = bounds.miny + yRow + yShift;
                                zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.125) + zCentre;
                                point3 = new Vec3(xPos, yPos, zPos);
                            }
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                                float[] s_ = new float[3]; s_[0] = 0; s_[1] = 0; s_[2] = 0;
                                Vec3[] vertex = new Vec3[3];
                                vertex[0] = point1;
                                vertex[1] = point2;
                                vertex[2] = point3;
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                                //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                            }
                        }
                        if(y_i % 6 == 0){   // Show intermittant progress
                            window.updateImage();
                        }
                    }
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                } // end of Y axis
                
                
                // Z axis
                if( (bounds.maxz - bounds.minz) > (bounds.maxy - bounds.miny) &&
                 (bounds.maxx - bounds.minx) < (bounds.maxz - bounds.minz)   ){ // X axis
                    System.out.println(" is on z ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int z_index = 0;
                    double unit_distance = (bounds.maxx - bounds.minx) / 5.5; // testing for now
                    //System.out.println("unit_distance " + unit_distance); // 0.36363636363636365
                    double partDiameter = (bounds.maxy - bounds.miny);
                    double partRadius = (partDiameter / 2);
                    double partRadiusExpand = partRadius * 1.01; // expand for visibility.
                    double xCentre = ((bounds.maxx - bounds.minx) / 2);
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                    for(int z_i = 0; z_i < ((bounds.maxz - bounds.minz) / unit_distance); z_i++){
                        double zRow =  ( z_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        
                        double rShift = 0; // rotate to evenly pack
                        if(z_i % 4 == 2 || z_i % 4 == 3){ //
                            rShift = Math.toRadians( (360.0 / ((double)rUnits)) / 2 );
                        }
                        double zShift = 0;  // Shift to evenly pack
                        if(z_i % 4 == 1){
                            zShift = (unit_distance / 7.0); // move right
                        } else if (z_i % 4 == 2){
                            zShift = -(unit_distance / 7.0); // move left
                        }
                        if(z_i % 4 == 1){
                            zShift += (unit_distance / 24.0);
                        }
                        if(z_i % 4 == 2){
                            zShift += ((unit_distance / 24.0) * 2);
                            zShift += ((unit_distance / 24.0) );
                        }
                        if(z_i % 4 == 3){
                            zShift += ((unit_distance / 24.0) * 3);
                            zShift += ((unit_distance / 24.0) );
                        }
                        
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            
                            // 1
                            double xPos = bounds.minx + partRadiusExpand * Math.sin(angle) + xCentre;
                            double yPos = bounds.miny + (partRadiusExpand * Math.cos(angle)) + yCentre;
                            double zPos = bounds.minz + zRow + zShift;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.minx + partRadiusExpand * Math.sin(angle + 0.25) + xCentre;
                            yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.25)) + yCentre;
                            zPos = bounds.minz + zRow + zShift;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.minx + partRadius * Math.sin(angle + 0.125) + xCentre;
                            yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.125)) + yCentre;
                            zPos = bounds.minz + zRow + (unit_distance / 1.65) + zShift;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            if(z_i % 2 == 0){
                                xPos = bounds.minx + partRadiusExpand * Math.sin(angle) + xCentre;
                                yPos = bounds.miny + (partRadiusExpand * Math.cos(angle)) + yCentre;
                                zPos = bounds.minz + zRow + (unit_distance / 1.65) + zShift;
                                point1 = new Vec3(xPos, yPos, zPos);
                                
                                xPos = bounds.minx + partRadiusExpand * Math.sin(angle + 0.25) + xCentre;
                                yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.25)) + yCentre;
                                zPos = bounds.minz + zRow + (unit_distance / 1.65) + zShift;
                                point2 = new Vec3(xPos, yPos, zPos);
                                
                                xPos = bounds.minx + partRadiusExpand * Math.sin(angle + 0.125) + xCentre;
                                yPos = bounds.miny + (partRadiusExpand * Math.cos(angle + 0.125)) + yCentre;
                                zPos = bounds.minz + zRow + zShift;
                                point3 = new Vec3(xPos, yPos, zPos);
                            }
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                                float[] s_ = new float[3]; s_[0] = 0; s_[1] = 0; s_[2] = 0;
                                Vec3[] vertex = new Vec3[3];
                                vertex[0] = point1;
                                vertex[1] = point2;
                                vertex[2] = point3;
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                                //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                            }
                        }
                        if(z_i % 6 == 0){   // Show intermittant progress
                            window.updateImage();
                        }
                    }
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                } // end Z axis
                
            } else {
                System.out.println("Error: Object is not a mesh. ");
            }
        }
    }
    
    
    /**
     * perferateSquares
     *
     * Description:
     *
     */
    public void perferateSquares(Scene scene){
        LayoutModeling layout = new LayoutModeling();
        int selection[] = scene.getSelection();
        if(selection.length > 0){
            ObjectInfo info = scene.getObject(selection[0]);
            //System.out.println("obj " + info);
            Object co = (Object)info.getObject();
            if((co instanceof Mesh) == true){
        
                // Find object orientation
                
                BoundingBox bounds = getTranslatedBounds(info);
                System.out.println("bounds " + bounds.minx + " " + bounds.maxx + " " +
                                   bounds.miny + " " + bounds.maxy + " " +
                                   bounds.minz + " " + bounds.maxz + " " );
                
                // What if part is not aligned to primary axis???
                // todo sort out
                
                // Collect child verticies and avoid pererating near them.
                Vector childVerts = new Vector();
                ObjectInfo[] children = info.getChildren();
                for(int i = 0; i < children.length; i++){
                    ObjectInfo child = children[i];
                    Object childCo = (Object)child.getObject();
                    if((childCo instanceof Curve) == true){
                        CoordinateSystem c;
                        c = layout.getCoords(child);
                        //Vec3 objOrigin = c.getOrigin();
                        
                        //System.out.println("Curve");
                        Mesh mesh = (Mesh) child.getObject(); // Object3D
                        Vec3 [] verts = mesh.getVertexPositions();
                        for (Vec3 vert : verts){
                            
                            Mat4 mat4 = c.duplicate().fromLocal();
                            mat4.transform(vert);
                            
                            //System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                            childVerts.addElement(vert);
                        }
                    }
                }
                
                if( (bounds.maxx - bounds.minx) > (bounds.maxy - bounds.miny) &&
                     (bounds.maxx - bounds.minx) > (bounds.maxz - bounds.minz)   ){ // X axis
                    System.out.println(" is on x ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int x_index = 0; // bounds.minx;
                    double unit_distance = (bounds.maxz - bounds.minz) / (5.5 - 1); // testing for now
                    //System.out.println("unit_distance " + unit_distance); // 0.36363636363636365
                    double partDiameter = (bounds.maxy - bounds.miny);
                    double partRadius = (partDiameter / 2);
                    partRadius = partRadius * 1.02; // expand for visibility.
                    //System.out.println(" xi : " + ((bounds.maxx - bounds.minx) / (unit_distance)));
                    //System.out.println(" r_i : " + (360 / (unit_distance * 4 )));
                    double xCentre = ((bounds.maxx - bounds.minx) / 2);
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                    
                    //for(int x_i = 0; x_i < ((bounds.maxx - bounds.minx) / unit_distance); x_i ++){ // x unit_distance
                    for(int x_i = 0; x_i < ((bounds.maxx - bounds.minx) / unit_distance); x_i++){
                        double xRow =  ( x_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        
                        double rShift = 0; // rotate to evenly pack
                        //if(x_i % 2 == 1){ //
                        //    rShift = Math.toRadians( (360.0 / ((double)rUnits)) / 2 );
                        //}
                        double xShift = 0;  // Shift to evenly pack
                        //if(x_i % 4 == 1){
                            //xShift = (unit_distance / 7.0); // move right
                        //} else if (x_i % 4 == 2){
                            //xShift = -(unit_distance / 7.0); // move left
                        //}
                        
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            //System.out.println("  angle " + angle);
                            
                            
                            // entry point
                            double xPos = bounds.minx + xRow + (unit_distance / 7) + xShift;
                            double yPos = bounds.miny + (partRadius * Math.cos(angle + 0.05)) + yCentre;
                            double zPos = bounds.minz + partRadius * Math.sin(angle + 0.05) + zCentre;
                            Vec3 entryPoint = new Vec3(xPos, yPos, zPos);
                            
                            // 1
                            xPos = bounds.minx + xRow + xShift;
                            yPos = bounds.miny + (partRadius * Math.cos(angle)) + yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle) + zCentre;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.minx + xRow + xShift;
                            yPos = bounds.maxy + (partRadius * Math.cos(angle + 0.22)) - yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle + 0.22) + zCentre;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.minx + xRow + (unit_distance / 2) + xShift;
                            yPos = bounds.maxy + (partRadius * Math.cos(angle + 0.22)) - yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle+ 0.22) + zCentre;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            // 4
                            xPos = bounds.minx + xRow + (unit_distance / 2) + xShift;
                            yPos = bounds.maxy + (partRadius * Math.cos(angle)) - yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle) + zCentre;
                            Vec3 point4 = new Vec3(xPos, yPos, zPos);
                            
                            // close
                            xPos = bounds.minx + xRow + xShift;
                            yPos = bounds.miny + (partRadius * Math.cos(angle)) + yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle) + zCentre;
                            Vec3 closePoint = new Vec3(xPos, yPos, zPos);
                            
                            // exit
                             xPos = bounds.minx + xRow + (unit_distance / 6) + xShift;
                             yPos = bounds.miny + (partRadius * Math.cos(angle + 0.04)) + yCentre;
                             zPos = bounds.minz + partRadius * Math.sin(angle + 0.04) + zCentre;
                            Vec3 exitPoint = new Vec3(xPos, yPos, zPos);
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                                float[] s_ = new float[7]; s_[0] = 0; s_[1] = 0; s_[2] = 0; s_[3] = 0; s_[4] = 0; s_[5] = 0; s_[6] = 0;
                                Vec3[] vertex = new Vec3[7];
                                vertex[0] = entryPoint;
                                vertex[1] = point1;
                                vertex[2] = point2;
                                vertex[3] = point3;
                                vertex[4] = point4;
                                vertex[5] = closePoint;
                                vertex[6] = exitPoint;
                                
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                //window.addObject(perferationCurve, coords, "Perferation " + ++p, null);
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                            }
                            //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                            //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                        }
                    }
                    
                    // end of X axis
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                }
                
                
                // Y axis
                if( (bounds.maxx - bounds.minx) < (bounds.maxy - bounds.miny) &&
                     (bounds.maxy - bounds.miny) > (bounds.maxz - bounds.minz)   ){ // Y axis
                    System.out.println(" is on y ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int x_index = 0; // bounds.minx;
                    double unit_distance = (bounds.maxz - bounds.minz) / 5.5; // testing for now
                    //System.out.println("unit_distance " + unit_distance); // 0.36363636363636365
                    double partDiameter = (bounds.maxx - bounds.minx);
                    double partRadius = (partDiameter / 2);
                    double partRadiusExpand = partRadius * 1.01; // expand for visibility.
                    double xCentre = ((bounds.maxx - bounds.minx) / 2);
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                    
                    for(int y_i = 0; y_i < ((bounds.maxy - bounds.miny) / unit_distance); y_i++){
                        double yRow =  (y_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        double rShift = 0; // rotate to evenly pack
                        double yShift = 0;  // Shift to evenly pack
                       
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            
                            // 1
                            double xPos = bounds.minx + (partRadiusExpand * Math.cos(angle)) + xCentre;
                            double yPos = bounds.miny + yRow + yShift;
                            double zPos = bounds.minz + partRadiusExpand * Math.sin(angle) + zCentre;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.maxx + (partRadiusExpand * Math.cos(angle + 0.22)) - xCentre;
                            yPos = bounds.miny + yRow + yShift;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle + 0.22) + zCentre;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.maxx + (partRadiusExpand * Math.cos(angle + 0.22)) - xCentre;
                            yPos = bounds.miny + yRow + (unit_distance / 2) + yShift;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle+ 0.22) + zCentre;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            // 4
                            xPos = bounds.maxx + (partRadiusExpand * Math.cos(angle)) - xCentre;
                            yPos = bounds.miny + yRow + (unit_distance / 2) + yShift;
                            zPos = bounds.minz + partRadiusExpand * Math.sin(angle) + zCentre;
                            Vec3 point4 = new Vec3(xPos, yPos, zPos);
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                                float[] s_ = new float[4]; s_[0] = 0; s_[1] = 0; s_[2] = 0;  s_[3] = 0;
                                Vec3[] vertex = new Vec3[4];
                                vertex[0] = point1;
                                vertex[1] = point2;
                                vertex[2] = point3;
                                vertex[3] = point4;
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                                //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                            }
                        }
                    }
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                } // end of Y axis
                
                
                // Z
                if( (bounds.maxx - bounds.minx) < (bounds.maxz - bounds.minz) &&
                     (bounds.maxy - bounds.miny) < (bounds.maxz - bounds.minz)   ){ // Y axis
                    System.out.println(" is on z ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int x_index = 0; // bounds.minx;
                    double unit_distance = (bounds.maxx - bounds.minx) / 5.5; // testing for now
                    //System.out.println("unit_distance " + unit_distance); // 0.36363636363636365
                    double partDiameter = (bounds.maxx - bounds.minx);
                    double partRadius = (partDiameter / 2);
                    double partRadiusExpand = partRadius * 1.01; // expand for visibility.
                    double xCentre = ((bounds.maxx - bounds.minx) / 2);
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                    
                    for(int z_i = 0; z_i < ((bounds.maxz - bounds.minz) / unit_distance); z_i++){
                        double zRow =  (z_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        double rShift = 0; // rotate to evenly pack
                        double zShift = 0;  // Shift to evenly pack
                       
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            
                            // 1
                            double xPos = bounds.minx + (partRadiusExpand * Math.cos(angle)) + xCentre;
                            double yPos = bounds.miny + partRadiusExpand * Math.sin(angle) + yCentre;
                            double zPos = bounds.minz + zRow + zShift;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.maxx + (partRadiusExpand * Math.cos(angle + 0.22)) - xCentre;
                            yPos = bounds.miny + partRadiusExpand * Math.sin(angle + 0.22) + yCentre;
                            zPos = bounds.minz + zRow + zShift;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.maxx + (partRadiusExpand * Math.cos(angle + 0.22)) - xCentre;
                            yPos = bounds.miny + partRadiusExpand * Math.sin(angle+ 0.22) + yCentre;
                            zPos = bounds.minz + zRow + (unit_distance / 2) + zShift;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            // 4
                            xPos = bounds.maxx + (partRadiusExpand * Math.cos(angle)) - xCentre;
                            yPos = bounds.miny + partRadiusExpand * Math.sin(angle) + yCentre;
                            zPos = bounds.minz + zRow + (unit_distance / 2) + zShift;
                            Vec3 point4 = new Vec3(xPos, yPos, zPos);
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                                float[] s_ = new float[4]; s_[0] = 0; s_[1] = 0; s_[2] = 0;  s_[3] = 0;
                                Vec3[] vertex = new Vec3[4];
                                vertex[0] = point1;
                                vertex[1] = point2;
                                vertex[2] = point3;
                                vertex[3] = point4;
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                                //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                            }
                        }
                    }
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                } // end of Y axis
                
            } else {
                System.out.println("Error: Object is not a mesh. ");
            }
        }
    }
    
    /**
     * perferateDiamonds
     *
     * Description:
     */
    public void perferateDiamonds(Scene scene){
        LayoutModeling layout = new LayoutModeling();
        int selection[] = scene.getSelection();
        if(selection.length > 0){
            ObjectInfo info = scene.getObject(selection[0]);
            //System.out.println("obj " + info);
            Object co = (Object)info.getObject();
            if((co instanceof Mesh) == true){
        
                // Find object orientation
                
                BoundingBox bounds = getTranslatedBounds(info);
                System.out.println("bounds " + bounds.minx + " " + bounds.maxx + " " +
                                   bounds.miny + " " + bounds.maxy + " " +
                                   bounds.minz + " " + bounds.maxz + " " );
                
                // What if part is not aligned to primary axis???
                // todo sort out
                
                // Collect child verticies and avoid pererating near them.
                Vector childVerts = new Vector();
                ObjectInfo[] children = info.getChildren();
                for(int i = 0; i < children.length; i++){
                    ObjectInfo child = children[i];
                    Object childCo = (Object)child.getObject();
                    if((childCo instanceof Curve) == true){
                        CoordinateSystem c;
                        c = layout.getCoords(child);
                        //Vec3 objOrigin = c.getOrigin();
                        
                        //System.out.println("Curve");
                        Mesh mesh = (Mesh) child.getObject(); // Object3D
                        Vec3 [] verts = mesh.getVertexPositions();
                        for (Vec3 vert : verts){
                            
                            Mat4 mat4 = c.duplicate().fromLocal();
                            mat4.transform(vert);
                            
                            //System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                            childVerts.addElement(vert);
                        }
                    }
                }
                
                if( (bounds.maxx - bounds.minx) > (bounds.maxy - bounds.miny) &&
                     (bounds.maxx - bounds.minx) > (bounds.maxz - bounds.minz)   ){ // X axis
                    System.out.println(" is on x ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int x_index = 0; // bounds.minx;
                    double unit_distance = (bounds.maxz - bounds.minz) / 5.5; // testing for now
                    double partDiameter = (bounds.maxy - bounds.miny);
                    double partRadius = (partDiameter / 2);
                    partRadius = partRadius * 1.02; // expand for visibility.
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                    
                    for(int x_i = 0; x_i < ((bounds.maxx - bounds.minx) / unit_distance); x_i++){
                        double xRow =  ( x_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        
                        double rShift = 0; // rotate to evenly pack
                        if(x_i % 2 == 1){ //
                            rShift = Math.toRadians( (360.0 / ((double)rUnits)) / 2 );
                        }
                        double xShift = 0;  // Shift to evenly pack
                        
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            //System.out.println("  angle " + angle);
                            
                            // Entry point. First point within the shape to prevent cutting edge.
                            double xPos = bounds.minx + xRow + ((unit_distance / 1.68)/1.0) + xShift;
                            double yPos = bounds.miny + (partRadius * Math.cos(angle - 0.045)) + yCentre;
                            double zPos = bounds.minz + partRadius * Math.sin(angle - 0.045) + zCentre;
                            Vec3 entryPoint = new Vec3(xPos, yPos, zPos);
                            
                            // 1
                            xPos = bounds.minx + xRow + xShift;
                            yPos = bounds.miny + (partRadius * Math.cos(angle)) + yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle) + zCentre;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.minx + xRow + ((unit_distance / 1.68)/1.1) + xShift;
                            yPos = bounds.miny + (partRadius * Math.cos(angle + 0.125)) + yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle + 0.125) + zCentre;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.minx + xRow + ((unit_distance / 1.68) * 1.8 ) + xShift;
                            yPos = bounds.miny + (partRadius * Math.cos(angle )) + yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle) + zCentre;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            // 4
                            xPos = bounds.minx + xRow + ((unit_distance / 1.68)/1.1) + xShift;
                            yPos = bounds.miny + (partRadius * Math.cos(angle - 0.125)) + yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle - 0.125) + zCentre;
                            Vec3 point4 = new Vec3(xPos, yPos, zPos);
                            
                            // Close
                            xPos = bounds.minx + xRow + xShift;
                            yPos = bounds.miny + (partRadius * Math.cos(angle)) + yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle) + zCentre;
                            Vec3 closePoint = new Vec3(xPos, yPos, zPos);
                            
                            // Exit point
                            xPos = bounds.minx + xRow + ((unit_distance / 1.68)/1.2) + xShift;
                            yPos = bounds.miny + (partRadius * Math.cos(angle + 0.045)) + yCentre;
                            zPos = bounds.minz + partRadius * Math.sin(angle + 0.045) + zCentre;
                            Vec3 exitPoint = new Vec3(xPos, yPos, zPos);
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                                float[] s_ = new float[7]; s_[0] = 0; s_[1] = 0; s_[2] = 0; s_[3] = 0; s_[4] = 0; s_[5] = 0; s_[6] = 0;
                                Vec3[] vertex = new Vec3[7];
                                vertex[0] = entryPoint;
                                vertex[1] = point1;
                                vertex[2] = point2;
                                vertex[3] = point3;
                                vertex[4] = point4;
                                vertex[5] = closePoint;
                                vertex[6] = exitPoint;
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                                //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                            }
                        }
                    }
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                } // end of X axis
                
                
                // Y
                if( (bounds.maxy - bounds.miny) > (bounds.maxx - bounds.minx) &&
                 (bounds.maxy - bounds.miny) > (bounds.maxz - bounds.minz)   ){ // X axis
                    System.out.println(" is on y ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int x_index = 0; // bounds.minx;
                    double unit_distance = (bounds.maxz - bounds.minz) / 5.5; // testing for now
                    //System.out.println("unit_distance " + unit_distance); // 0.36363636363636365
                    double partDiameter = (bounds.maxx - bounds.minx);
                    double partRadius = (partDiameter / 2);
                    partRadius = partRadius * 1.02; // expand for visibility.
                    //System.out.println(" xi : " + ((bounds.maxx - bounds.minx) / (unit_distance)));
                    //System.out.println(" r_i : " + (360 / (unit_distance * 4 )));
                    double xCentre = ((bounds.maxx - bounds.minx) / 2);
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                
                    for(int y_i = 0; y_i < ((bounds.maxy - bounds.miny) / unit_distance); y_i++){
                        double yRow =  ( y_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        
                        double rShift = 0; // rotate to evenly pack
                        if(y_i % 2 == 1){ //
                            rShift = Math.toRadians( (360.0 / ((double)rUnits)) / 2 );
                        }
                        double yShift = 0;  // Shift to evenly pack
                        
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            //System.out.println("  angle " + angle);
                            
                            // 1
                            double xPos = bounds.minx + (partRadius * Math.cos(angle)) + xCentre;
                            double yPos = bounds.miny + yRow + yShift;
                            double zPos = bounds.minz + partRadius * Math.sin(angle) + zCentre;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.minx + (partRadius * Math.cos(angle + 0.125)) + xCentre;
                            yPos = bounds.miny + yRow + ((unit_distance / 1.68)/1.1) + yShift;
                            zPos = bounds.minz + partRadius * Math.sin(angle + 0.125) + zCentre;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.minx + (partRadius * Math.cos(angle )) + xCentre;
                            yPos = bounds.miny + yRow + ((unit_distance / 1.68) * 1.8 ) + yShift;
                            zPos = bounds.minz + partRadius * Math.sin(angle) + zCentre;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            // 4
                            xPos = bounds.minx + (partRadius * Math.cos(angle - 0.125)) + xCentre;
                            yPos = bounds.miny + yRow + ((unit_distance / 1.68)/1.1) + yShift;
                            zPos = bounds.minz + partRadius * Math.sin(angle - 0.125) + zCentre;
                            Vec3 point4 = new Vec3(xPos, yPos, zPos);
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                                float[] s_ = new float[4]; s_[0] = 0; s_[1] = 0; s_[2] = 0; s_[3] = 0;
                                Vec3[] vertex = new Vec3[4];
                                vertex[0] = point1;
                                vertex[1] = point2;
                                vertex[2] = point3;
                                vertex[3] = point4;
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                                //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                            }
                        }
                    }
                
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                } // end of Y axis
                
                
                // Z
                if( (bounds.maxz - bounds.minz) > (bounds.maxx - bounds.minx) &&
                 (bounds.maxz - bounds.minz) > (bounds.maxy - bounds.miny)   ){ // X axis
                    System.out.println(" is on z ");
                    
                    // H
                    double rotate_angle_index = 0; // 360 degrees total
                    int x_index = 0; // bounds.minx;
                    double unit_distance = (bounds.maxx - bounds.minx) / 5.5; // testing for now
                    //System.out.println("unit_distance " + unit_distance); // 0.36363636363636365
                    double partDiameter = (bounds.maxx - bounds.minx);
                    double partRadius = (partDiameter / 2);
                    partRadius = partRadius * 1.02; // expand for visibility.
                    //System.out.println(" xi : " + ((bounds.maxx - bounds.minx) / (unit_distance)));
                    //System.out.println(" r_i : " + (360 / (unit_distance * 4 )));
                    double xCentre = ((bounds.maxx - bounds.minx) / 2);
                    double yCentre = ((bounds.maxy - bounds.miny) / 2);
                    double zCentre = ((bounds.maxz - bounds.minz) / 2);
                    int p = 0;
                
                    for(int z_i = 0; z_i < ((bounds.maxz - bounds.minz) / unit_distance); z_i++){
                        double zRow =  ( z_i * unit_distance); // (bounds.minx) +
                        int rUnits = 14; // (int)(unit_distance * 38.5); // 0.36363636363636365 = 14
                        
                        double rShift = 0; // rotate to evenly pack
                        if(z_i % 2 == 1){ //
                            rShift = Math.toRadians( (360.0 / ((double)rUnits)) / 2 );
                        }
                        double zShift = 0;  // Shift to evenly pack
                        
                        for(int r_i = 0; r_i < rUnits; r_i++){ // rotate    // unit_distance
                            double angle = (360.0 / ((double)rUnits)) * (double)r_i;
                            angle = Math.toRadians(angle);
                            angle = angle + rShift; // shift every double pair
                            //System.out.println("  angle " + angle);
                            
                            // 1
                            double xPos = bounds.minx + (partRadius * Math.cos(angle)) + xCentre;
                            double yPos = bounds.miny + partRadius * Math.sin(angle) + yCentre;
                            double zPos = bounds.minz + zRow + zShift;
                            Vec3 point1 = new Vec3(xPos, yPos, zPos);
                            
                            // 2
                            xPos = bounds.minx + (partRadius * Math.cos(angle + 0.125)) + xCentre;
                            yPos = bounds.miny + partRadius * Math.sin(angle + 0.125) + yCentre;
                            zPos = bounds.minz + zRow + ((unit_distance / 1.68)/1.1) + zShift;
                            Vec3 point2 = new Vec3(xPos, yPos, zPos);
                            
                            // 3
                            xPos = bounds.minx + (partRadius * Math.cos(angle )) + xCentre;
                            yPos = bounds.miny + partRadius * Math.sin(angle) + yCentre;
                            zPos = bounds.minz + zRow + ((unit_distance / 1.68) * 1.8 ) + zShift;
                            Vec3 point3 = new Vec3(xPos, yPos, zPos);
                            
                            // 4
                            xPos = bounds.minx + (partRadius * Math.cos(angle - 0.125)) + xCentre;
                            yPos = bounds.miny + partRadius * Math.sin(angle - 0.125) + yCentre;
                            zPos = bounds.minz + zRow + ((unit_distance / 1.68)/1.1) + zShift;
                            Vec3 point4 = new Vec3(xPos, yPos, zPos);
                            
                            if(isCloseToPoints(point1, childVerts, unit_distance * 2) == false){
                                float[] s_ = new float[4]; s_[0] = 0; s_[1] = 0; s_[2] = 0; s_[3] = 0;
                                Vec3[] vertex = new Vec3[4];
                                vertex[0] = point1;
                                vertex[1] = point2;
                                vertex[2] = point3;
                                vertex[3] = point4;
                                Curve perferationCurve = new Curve(vertex, s_, 0, true); // false
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, "Perferation " + ++p);
                                perferationInfo.setParent(info); // Add perferation object to selection.
                                info.addChild(perferationInfo, info.getChildren().length); // info.getChildren().length+1
                                window.addObject(perferationInfo, null); // Add ObjectInfo
                                //window.setSelection(window.getScene().getNumObjects()-1); // Add to selected object as child
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
                            }
                        }
                    }
                
                    window.updateImage();
                    window.updateTree(); // Tell the tree it has changed.
                } // end of Z axis
                    
                
            } else {
                System.out.println("Error: Object is not a mesh. ");
            }
        }
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

