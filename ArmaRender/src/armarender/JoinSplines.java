/* Copyright (C) 2019 - 2021 by Jon Taylor

This program is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;
import armarender.view.CanvasDrawer;
import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import buoy.widget.*;
import armarender.ui.*;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JFrame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import armarender.texture.*;

public class JoinSplines {
    
    public JoinSplines(){
    }
    
    /**
     * joinSplines
     *
     * Description: given a selection of spline curves. join them into one continous new curve object
     */
    public void joinSplines(Scene scene, LayoutWindow layoutWindow, Vector<ObjectInfo> objects){
        Vector<Vec3[]> splines = new <Vec3[]>Vector();
        Vector<ObjectInfo> selectionParents = new Vector<ObjectInfo>();
        Vector sourceCurves = new Vector();
        LayoutModeling layout = new LayoutModeling();
        int joinedSplineLength = 0;
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                //System.out.println("Object Info: ");
                Object co = (Object)obj.getObject();
                if((co instanceof Curve) == true){
                    //System.out.println("Curve");

                    Mesh mesh = (Mesh) obj.getObject(); // Object3D
                    Vec3 [] verts = mesh.getVertexPositions();

                    // translate local coords with obj location.
                    CoordinateSystem c;
                    c = layout.getCoords(obj);
                    Vec3 objOrigin = c.getOrigin();
                    for (Vec3 vert : verts){
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vert);
                        //System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                    }
                    splines.addElement(verts);
                    selectionParents.addElement( obj.getParent() );
                    joinedSplineLength += verts.length;
                    
                    sourceCurves.addElement(obj);
                }
            }
        }
        
        ObjectInfo parent = null;
        if(selectionParents.size() == 2){
            ObjectInfo a = (ObjectInfo)selectionParents.elementAt(0);
            ObjectInfo b = (ObjectInfo)selectionParents.elementAt(1);
            if(a != null && b != null && a.getId() == b.getId()){
                parent = a;
            }
        }
        
        // Require only two selected curves.
        if(splines.size() != 2){
            // Error: Only two curves are supported.
            System.out.println("Error: Only two selected curves are supported." );
            
            return;
        }
        
        
        // Whech ends connect.
        // The order of the curves does not mean they are in the order they are to be connected
        //Double threshold = 0; // distance
        
        Vector<Vec3> joinedSpline = new Vector<Vec3>(); // new curve (joinedSplineLength)
        
        // Which ends connect
        Vector pairEndDistances = new Vector();
        for(int i = 1; i < splines.size(); i++){
            Vec3[] splineA = splines.elementAt(i - 1);
            Vec3[] splineB = splines.elementAt(i);
            double ae_bs = splineA[splineA.length - 1].distance(splineB[0]);
            double be_as = splineB[splineB.length - 1].distance(splineA[0]);
            double as_bs = splineA[0].distance(splineB[0]);
            double ae_be = splineA[splineA.length - 1].distance(splineB[splineB.length - 1]);
            
            if(ae_bs < be_as && ae_bs < as_bs && ae_bs < ae_be){ // ae_bs
                for(int s = 0; s < splines.size(); s++){
                    Vec3[] spline = (Vec3[])splines.elementAt(s);
                    if(s == 0){
                        for(int v = 0; v < spline.length; v++){
                            Vec3 vec = (Vec3)spline[v];
                            joinedSpline.addElement(vec);
                        }
                    } else if(s == 1){
                        for(int v = 0; v < spline.length; v++){
                            Vec3 vec = (Vec3)spline[v];
                            joinedSpline.addElement(vec);
                        }
                    }
                }
            } else if(be_as < ae_bs && be_as < as_bs && be_as < ae_be){ // be_as
                for(int s = 0; s < splines.size(); s++){
                    Vec3[] spline = (Vec3[])splines.elementAt(s);
                    if(s == 0){
                        for(int v = spline.length - 1; v >= 0; v--){
                            Vec3 vec = (Vec3)spline[v];
                            joinedSpline.addElement(vec);
                        }
                    } else if(s == 1){
                        for(int v = spline.length - 1; v >= 0; v--){
                            Vec3 vec = (Vec3)spline[v];
                            joinedSpline.addElement(vec);
                        }
                    }
                }
            } else if(as_bs < ae_bs && as_bs < be_as && as_bs < ae_be){ // as_bs
                for(int s = 0; s < splines.size(); s++){
                    Vec3[] spline = (Vec3[])splines.elementAt(s);
                    if(s == 0){
                        for(int v = spline.length - 1; v >= 0; v--){
                            Vec3 vec = (Vec3)spline[v];
                            joinedSpline.addElement(vec); // (s * (splines.size() - 0)) + v
                        }
                    } else if(s == 1){
                        for(int v = 0; v < spline.length; v++){
                            Vec3 vec = (Vec3)spline[v];
                            joinedSpline.addElement(vec); // (s * (splines.size() - 0)) + v
                        }
                    }
                }
            } else if(ae_be < ae_bs && ae_be < be_as && ae_be < as_bs){ // ae_be
                for(int s = 0; s < splines.size(); s++){
                    Vec3[] spline = (Vec3[])splines.elementAt(s);
                    if(s == 0){
                        for(int v = 0; v < spline.length; v++){
                            Vec3 vec = (Vec3)spline[v];
                            joinedSpline.addElement(vec); // (s * (splines.size() - 0)) + v
                        }
                    } else if(s == 1){
                        for(int v = spline.length - 1; v >= 0; v--){
                            Vec3 vec = (Vec3)spline[v];
                            joinedSpline.addElement(vec); // (s * (splines.size() - 0)) + v
                        }
                    }
                }
            }
        }
    
        //System.out.println("Joined curve: "  );
       
        Vec3[] joinedPoints = new Vec3[joinedSpline.size()];
        for(int i = 0; i < joinedSpline.size(); i++){
            joinedPoints[i] = joinedSpline.elementAt(i);
        }
        Curve joinedCurve = getCurve(joinedPoints);
        
        CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
        
        ObjectInfo joinedCurveOI = new ObjectInfo(joinedCurve, coords, "Joined");
        
        
        UndoRecord undo = new UndoRecord(layoutWindow, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(layoutWindow.getScene().getNumObjects()-1)});
        //layoutWindow.addObject(joinedCurve, coords, "joined ", null);
        layoutWindow.addObject(joinedCurveOI, undo);
        
        if(parent != null){
            joinedCurveOI.setParent(parent);
            parent.addChild(joinedCurveOI, 0); // parent.getChildren().length - 1
        }
        
        // Add existing curves to new joined curve as child.
        // Hide existing curves
        for(int i = 0; i < sourceCurves.size(); i++){
            ObjectInfo oi = (ObjectInfo)sourceCurves.elementAt(i);
            //oi.setParent(joinedCurveOI);
            //joinedCurveOI.addChild(oi, i);
            oi.setVisible(false);
        }
        
        layoutWindow.setSelection(layoutWindow.getScene().getNumObjects()-1);
        layoutWindow.setUndoRecord(  undo  );
        layoutWindow.updateImage();
        layoutWindow.updateTree();
        
    }
    
    /**
     * getCurve
     *
     */
    public Curve getCurve(Vec3[] points){
        float smooths[] = new float[points.length];
        for(int i = 0; i < points.length; i++){
            smooths[i] = 1.0f;
        }
        Curve curve = new Curve(points, smooths, Mesh.APPROXIMATING, false);
        return curve;
    }
    
    
}
