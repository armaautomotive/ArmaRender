/* Copyright (C) 2023 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.animation.*;
import armarender.math.*;
import armarender.object.*;
import armarender.texture.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.Dimension;
import java.util.Vector;
import java.util.ArrayList;

/** This class renders a contoured highlight. */

public class ContouredHighlight
{
    static LayoutWindow window = null;
    static SceneViewer sceneViewer = null;
    boolean enabled = true;
    
    public ContouredHighlight(){
    }
    
    public void setLayoutWindow(LayoutWindow w){
        this.window = w;
    }

    public void setSceneViewer(SceneViewer sv){
        this.sceneViewer = sv;
    }
    
    public void setEnabled(boolean e){
        this.enabled = e;
    }
    
    /**
     * renderHighlight
     *
     * Description: Render a contoured highlight around a selection.
     *
     * Bug - multi object selection doesnt work
     */
    public void renderHighlight(){
        if(enabled == false)
            return;
        
        // Get selection
        ArrayList<ObjectInfo> selectedObjects = (ArrayList<ObjectInfo>)window.getSelectedObjects();
        for(int i = 0; i < selectedObjects.size(); i++){
            Vector<Vec2> selectionVerts = new Vector<Vec2>();
            Vector<Vec2> contractedSelectionVerts = new Vector<Vec2>();
            ObjectInfo obj = selectedObjects.get(i);
            //obj = obj.duplicate();
            Camera theCamera = sceneViewer.getCamera().duplicate();
            theCamera.setObjectTransform(obj.getCoords().duplicate().fromLocal());
            BoundingBox bb = obj.getBounds(); // obj.getTranslatedBounds();
            //Vec3 center = bb.getCenter();
            if(obj.getObject() instanceof Mesh){
                Mesh mesh = (Mesh)obj.getObject().duplicate();
                Vec3 verts [] = mesh.getVertexPositions();
                for(int v = 0; v < verts.length; v++){
                    Vec3 vert = verts[v];
                    Vec2 vecScreen = theCamera.findScreenPos(vert);
                    selectionVerts.addElement(vecScreen);
                }
            }
            Rectangle bounds = theCamera.findScreenBounds(bb);
            if(bounds == null){
                return;
            }
            
            Vector<Vec2> boundaryPoints = new Vector<Vec2>();
            boundaryPoints.addElement( new Vec2(bounds.x - 10, bounds.y + (bounds.height / 2)) );               // Center Left
            boundaryPoints.addElement( new Vec2(bounds.x - 10, bounds.y - 10) );                                // top left
            boundaryPoints.addElement( new Vec2(bounds.x + (bounds.width / 2), bounds.y - 10) );                // Top Center
            boundaryPoints.addElement( new Vec2(bounds.x + bounds.width + 10, bounds.y - 10 ));                 // Top Right
            boundaryPoints.addElement( new Vec2(bounds.x + bounds.width + 10, bounds.y + (bounds.height / 2)) ); // Center Right
            boundaryPoints.addElement( new Vec2(bounds.x + bounds.width + 10, bounds.y + bounds.height + 10) ); // Bottom Right
            boundaryPoints.addElement( new Vec2(bounds.x + (bounds.width / 2), bounds.y + bounds.height + 10) ); // Bottom Centre
            boundaryPoints.addElement( new Vec2(bounds.x - 10, bounds.y + bounds.height + 10) );                // Bottom Left
            
            // Subdivide boundary points
            boundaryPoints = subdividePoints(boundaryPoints, 4);
            
            Vector<Vec2> contractedNormals = new Vector<Vec2>(); // Keep track of normal for contraction to add padding in correct direction.
            
            // Contract points
            for(int p = 0; p < boundaryPoints.size(); p++){
                Vec2 pointA = boundaryPoints.elementAt(p);
                // For this boundary point Find its closesest geometry point.
                Vec2 closestGeometryPoint = null;
                double closestGeometryPointDistance = 99999;
                for(int g = 0; g < selectionVerts.size(); g++){
                    Vec2 currGeometryVert = selectionVerts.elementAt(g);
                    double dist = pointA.distance(currGeometryVert); // Error: currGeometryVert can be null.
                    if(dist < closestGeometryPointDistance){
                        closestGeometryPointDistance = dist;
                        closestGeometryPoint = currGeometryVert;
                    }
                }
                if(closestGeometryPoint != null){
                    contractedSelectionVerts.addElement(closestGeometryPoint);
                    
                    // Save contraction normal
                    Vec2 contractionNormal = pointA.minus(closestGeometryPoint);
                    contractionNormal.normalize();
                    contractionNormal = contractionNormal.times(3);
                    contractedNormals.addElement(contractionNormal);
                }
            }
            
            //
            // Draw contracted selection points
            //
            for(int p = 1; p < contractedSelectionVerts.size(); p++){
                Vec2 pointA = contractedSelectionVerts.elementAt(p - 1);
                Vec2 pointB = contractedSelectionVerts.elementAt(p);
                
                // Expand by contration normal
                Vec2 contractionNormalA = contractedNormals.elementAt(p - 1);
                Vec2 contractionNormalB = contractedNormals.elementAt(p);
                
                sceneViewer.drawLine(new Point((int)(pointA.x + contractionNormalA.x), (int)(pointA.y + contractionNormalA.y)),
                                     new Point((int)(pointB.x + contractionNormalB.x), (int)(pointB.y + contractionNormalB.y)), new Color(32, 64, 245, 80));
            }
            if(contractedSelectionVerts.size() > 1){        // Close ends
                Vec2 pointA = contractedSelectionVerts.elementAt(0);
                Vec2 pointB = contractedSelectionVerts.elementAt(contractedSelectionVerts.size() -1);
                
                // Expand by contration normal
                Vec2 contractionNormalA = contractedNormals.elementAt(0);
                Vec2 contractionNormalB = contractedNormals.elementAt(contractedSelectionVerts.size() -1);
                
                sceneViewer.drawLine(new Point((int)(pointA.x + contractionNormalA.x), (int)(pointA.y + contractionNormalA.y)),
                                     new Point((int)(pointB.x + contractionNormalB.x), (int)(pointB.y + contractionNormalB.y)), new Color(32, 64, 245, 80));
            }
        }
       
        //ConcaveHull ch = new ConcaveHull();
        //ArrayList<ConcaveHull.Point> pointArrayList = new ArrayList<ConcaveHull.Point>();
        //for(int i = 0; i < selectionVerts.size(); i++){
        //    Vec2 a = selectionVerts.elementAt(i);
        //    pointArrayList.add( new ConcaveHull.Point(a.x, a.y) );
        //}
        //ArrayList<ConcaveHull.Point> res = ch.calculateConcaveHull( pointArrayList, 2);
        //System.out.println(" res " + res.size() );
    }
    
    /**
     * subdividePoints
     *
     * Description: For a given list of points subdivide all
     *
     * @param: Vector<Vec2> Points
     * @param: int subdivisions. Not implemented.
     * @return: Subdivided Vector.
     */
    public Vector<Vec2> subdividePoints(Vector<Vec2> boundaryPoints, int subdivisions){
        for(int i = 1; i < boundaryPoints.size(); i++){
            Vec2 pointA = boundaryPoints.elementAt(i - 1);
            Vec2 pointB = boundaryPoints.elementAt(i);
            
            Vec2 midPoint = pointA.midPoint(pointB);
            boundaryPoints.add(i, midPoint);
            i++; // shift index to account for inserted element.
        }
        if(subdivisions > 1){
            boundaryPoints = subdividePoints(boundaryPoints, subdivisions - 1);
        }
        return boundaryPoints;
    }
    
    // Get largest angle
    public boolean pointAngles(Vec2 point, Vector<Vec2> points){
        Vector angles = new Vector();
        // Normals
        
        for(int i = 0; i < points.size(); i++){
            Vec2 curr = points.elementAt(i);
            if(curr != point){
                
                double angle = point.getAngle(curr);
                double degrees = Math.toDegrees(angle);
                //System.out.println(" angle: " + degrees);
                angles.addElement(degrees);
                
            }
        }
        // Find shortest difference in angle.
        return true;
    }
    
}
