/* Copyright (C) 2018, 2019 by Jon Taylor
  
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 
 
 1 for each force object get force vector direction and vertex it affects. Length could be force value.

 2 propagate force input vector to connected verticies (not repeating on verts). Each verticy
 will accumulate a set of force vectors from all scene force objects.
 Angle of force and angle of connected edge will affect the amount of force propagated.
 Angles same max, 90 deg off is none, 180 off is negative.
 each vertex point only needs one force vector that can be adjusted for each input force propagation.
 EAch propagation keeps
 
 track of which points it’s updated as to only do each one once.
 Thread per input force vector ???

 3 move verticies based on force vectors. Take average of all force vectors to apply.
 Use transformation to apply correct deformation.
 Threaded by groups of points.

 4 calculate deformation sum and repeat process until forces transferred into deformation.
 Possibly show edges in colour by deformation.

 
 */

package armarender;

import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;
import java.awt.Graphics;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import armarender.object.*;
import armarender.math.*;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import armarender.fea.*;

//import fea.*;   // Gennadiy FEA
//import elem.*;
//import gener.*;
//import model.*;
//import util.*;

//import static edu.uta.futureye.function.FMath.C0;

/** This class implements the dialog box which is used for the "Object Layout" and
    "Transform Object" commands.  It allows the user enter values for the position,
    orientation, and size of an object.  The initial values are passed to the constructor
    in values[].  If this argument is omitted, all of the fields will initially be blank.
    If transformLabels is true, the rows will be labelled "Move", "Rotate", and "Scale".
    If it is false, they will be labelled "Position", "Orientation", and "Size".
 
 
  Scene.runCrashSimulation()
 
 */

public class CrashSimulation extends BDialog
{
    boolean running = true;
    private double initialValues[], finalValues[];
    private ValueField fields[];
    private RadioButtonGroup centerGroup;
    private BCheckBox childrenBox;
    private BRadioButton objectCenterBox, selectionCenterBox;
    private boolean ok = true;

    private static boolean children = true;
    private static boolean selectionCenter = true;

    private double inerta = 0.5;
    
    LayoutWindow window;
    Vector<ObjectInfo> objects;
    
    private Fea fea;

    public CrashSimulation(BFrame parent) // LayoutWindow
    {
        super(parent, "FEA Crash Simulation", true);

        fields = new ValueField[9];
        layoutDialog();
        pack();
        setResizable(false);
        UIUtilities.centerDialog(this, parent);
        //fields[0].requestFocus();
        //if (show)
        setVisible(true);
        
        window = ((LayoutWindow)parent);
        
        fea = new Fea();
    }
    
    public void setObjects(Vector<ObjectInfo> objects){
        this.objects = objects;
        fea.setObjects(objects);
    }

    public boolean clickedOk()
    {
        return ok;
    }

    void layoutDialog()
    {
        BorderContainer content = new BorderContainer();
        setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
        FormContainer center = new FormContainer(3, 6);
        content.add(center, BorderContainer.CENTER);
        LayoutInfo eastLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
        LayoutInfo westLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);

        LayoutInfo centerLayout = new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, null, null);
        center.add(new BLabel("X"), 1, 0, centerLayout);
        //center.add(new BLabel("Y"), 2, 0, centerLayout);
        //center.add(new BLabel("Z"), 3, 0, centerLayout);
        Object listener = new Object() {
          void processEvent()
          {
            for (int i = 0; i < finalValues.length; i++){
              finalValues[i] = fields[i].getValue();
            }
            //if (childrenBox != null){
            //  children = childrenBox.getState();
            //}
            if (centerGroup != null){
              selectionCenter = (centerGroup.getSelection() == selectionCenterBox);
            }
            dispatchEvent(new ValueChangedEvent(CrashSimulation.this));
          }
        };

        center.add(Translate.label("Mass"), 0, 1, eastLayout);
        center.add(fields[0] = new ValueField(3550, ValueField.NONE), 1, 1);
        center.add(Translate.label("kgs"), 2, 1, westLayout);

        center.add(Translate.label("Velocity"), 0, 2, eastLayout);
        center.add(fields[1] = new ValueField(56, ValueField.NONE), 1, 2);
        center.add(Translate.label("kph"), 2, 2, westLayout);

        center.add(Translate.label("Force"), 0, 3, eastLayout);
        center.add(fields[2] = new ValueField(-1, ValueField.NONE), 1, 3);
        center.add(Translate.label("kph"), 2, 3, westLayout);

        center.add(Translate.label("Material"), 0, 4, eastLayout);
        String[] materialStrings = { "Steel", "Aluminum" };
        JComboBox materialList = new JComboBox(materialStrings);
        materialList.setSelectedIndex(0);
        //materialList.addActionListener(this);
        //center.add(materialList, 1, 4);

        center.add(Translate.label("Material Strength"), 0, 5, eastLayout);
        center.add(fields[3] = new ValueField(-1, ValueField.NONE), 1, 5);
        center.add(Translate.label("psi"), 2, 5, westLayout);


        /*
        for (int i = 0; i < 9; i++)
        {
          center.add(
        fields[i] =
            new ValueField(
                0, ValueField.NONE), (i%3)+1, (i/3)+1);
          fields[i].addEventLink(ValueChangedEvent.class, listener);
        }
        */
        /*
        if (extraOptions)
        {
          center.add(childrenBox = new BCheckBox(Translate.text("applyToUnselectedChildren"), children), 0, 4, 4, 1);
          childrenBox.addEventLink(ValueChangedEvent.class, listener);
          FormContainer extra = new FormContainer(2, 2);
          center.add(extra, 0, 5, 4, 1);
          extra.add(new BLabel(Translate.text("rotateScaleAround")), 0, 0, 1, 2, eastLayout);
          centerGroup = new RadioButtonGroup();
          centerGroup.addEventLink(SelectionChangedEvent.class, listener);
          LayoutInfo westLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
          extra.add(objectCenterBox = new BRadioButton(Translate.text("individualObjectCenters"), !selectionCenter, centerGroup), 1, 0, westLayout);
          extra.add(selectionCenterBox = new BRadioButton(Translate.text("centerOfSelection"), selectionCenter, centerGroup), 1, 1, westLayout);
        }
        */

        FormContainer extra = new FormContainer(1, 1);
        //RowContainer graphContainer = new RowContainer();
        JPanel graphPanel = new JPanel(){
            protected void paintComponent(Graphics g) {
              super.paintComponent(g);
              g.drawLine(0,0, 20, 35);
            };
        };

        //extra.add( graphPanel );
        //content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());


        RowContainer buttons = new RowContainer();
        content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());
        buttons.add(Translate.button("ok", this, "doOk"));
        buttons.add(Translate.button("cancel", this, "dispose"));
        addEventLink(WindowClosingEvent.class, this, "dispose");
        addAsListener(this);
    }

    private void doOk()
    {
        ValueField velocityField = fields[1];
        //velocityField.get
        
        ok = true;
        dispose();
    }

    private void keyPressed(KeyPressedEvent ev)
    {
        int code = ev.getKeyCode();

        if (code == KeyPressedEvent.VK_ENTER)
          doOk();
        if (code == KeyPressedEvent.VK_ESCAPE)
          dispose();
    }

    private void addAsListener(Widget w)
    {
        w.addEventLink(KeyPressedEvent.class, this, "keyPressed");
        if (w instanceof WidgetContainer)
        {
          Iterator iter = ((WidgetContainer) w).getChildren().iterator();
          while (iter.hasNext())
            addAsListener((Widget) iter.next());
        }
    }

    public ImpactThread impactThread = new ImpactThread();
    public class ImpactThread extends Thread {
        ObjectInfo obj;
        public ImpactThread(){}
        public void setObject(ObjectInfo obj){
            this.obj = obj;
        }
        public void run() {
            
            solver(); // New FEA Solver.
            fea.initalize(obj);
            
            
            // Test
            //Jfem jfem = new Jfem();
            
            
            while(obj != null && inerta > 0.001){
                impact(obj);
                
                fea.impact(obj);
                try {
                    Thread.sleep(50);
                } catch (Exception e){
                    
                }
            }
            
            //progressDialog.setVisible(false);
        }
    }
    
    /**
     * process: DEPRICATE
    */
    public void runImpact(ObjectInfo obj){
        while(inerta > 0.001){
            impact(obj);
            try {
                Thread.sleep(50);
            } catch (Exception e){
                
            }
        }
    }
    
    /**
     * solver
     * Description:
     */
    public void solver(){
        // 1
        Vector forceVectors = new Vector();
        for (ObjectInfo info : objects){
            Object co = (Object)info.getObject();
            if((co instanceof ForceObject) == true){
                
                System.out.println(" Force Vector ");
                Mesh mesh = (Mesh) info.getObject(); // Object3D
                Vec3 [] verts = mesh.getVertexPositions();
                for (Vec3 vert : verts){
                    System.out.println("   - " + vert.x + " " + vert.y + " " + vert.z);
                }
                
            }
        }
    }
    
    //public SolverThread solverThread = new SolverThread();
    public class SolverThread extends Thread {
        ObjectInfo obj;
        public SolverThread(){}
        public void setObject(ObjectInfo obj){
            this.obj = obj;
        }
        public void run() {
        
            
        }
    }
    
    public void applyForceVectors(){
        
        
    }
    
    /**
     * impact
     *
     * Description:
     *      This is a one dimensional static simulation that is only a proof of concept to access scene mesh points and edges used as a
     *      Platform for a correct fea solver.
     *
     * TODO: Break inerta into descrete values for each vertex as it transfers through the object.
     * TODO: Trasform coordinate points
     */
    public void impact(ObjectInfo meshObj){
        System.out.println("Run Simulation." + "  mesh: " + meshObj);
        
        LayoutModeling layout = new LayoutModeling();

        // We may need meshObj in TriangleMesh
        //
        Object3D triangleMesh = null; // && triangleMesh != null
        if(meshObj.getObject().canConvertToTriangleMesh() == Object3D.CANT_CONVERT){
            //System.out.println(" can't ");
        } else {
            //System.out.println(" can ");
            triangleMesh = meshObj.getObject().convertToTriangleMesh(0.05);
            //System.out.println(" tmesh " + triangleMesh.getClass().getName() );
            if( triangleMesh instanceof TriangleMesh ){     // selected object to impact is TriangleMesh
                TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                MeshVertex[] verts = ((TriangleMesh)triangleMesh).getVertices();
                
                Vec3 vecPoints[] = new Vec3[verts.length];
                for(int i = 0; i < verts.length; i++){
                    vecPoints[i] = verts[i].r;
                }
                
                // Pre collide edge distances
                HashMap<TriangleMesh.Edge, Double> preEdgeDistances = new HashMap<TriangleMesh.Edge, Double>();
                for(int i = 0; i < edges.length; i++){
                    TriangleMesh.Edge edge = edges[i];
                    double distance = verts[edge.v1].r.distance(verts[edge.v2].r);
                    preEdgeDistances.put(edge, distance);
                    //System.out.println("distance: " + distance);
                }
                
                
                // Force Vectors for each point
                //int vertId = ((Integer)orderedVec.get(i)).intValue();
                //Vector pointForceVectors = new Vector(); // vertId -> Vector( Vec3[] )
                HashMap<Vec3, Vector> pointForceVectors = new HashMap<Vec3, Vector>(); // Vec3 point, Vector normal forces list
                
                CoordinateSystem c;
                c = layout.getCoords(meshObj);
                Vec3 origin = c.getOrigin();
                
                // Find Left most vertex to start the impact calculation process.
                int firstVecId = -1;
                double firstVecX = 99999;
                double lastVecX = -99999;
                for(int i = 0; i < verts.length; i++){
                    Vec3 vertex1 = verts[i].r;
                    
                    Mat4 mat4 = c.duplicate().fromLocal();
                    //mat4.transform(vertex1);
                    
                    if(vertex1.x < firstVecX){
                        firstVecX = vertex1.x;
                        firstVecId = i;
                    }
                    if(vertex1.x > lastVecX){
                        lastVecX = vertex1.x;
                    }
                }
                System.out.println("firstVecId: " + firstVecId);
                
                // Traverse mesh, left to right. (Impact is from Left axis.)
                HashMap traveredPoint = new HashMap(); // Vec
                HashMap traveredEdge = new HashMap();
                Vector orderedVec = new Vector();
                
                Vector processQueueVecs = new Vector();
                
                boolean done = false;
                int currVecId = -1;
                int counter = 0;
                
                //
                // scan object, ordering points
                //
                while(!done){ // && currVecId >= 0
                    if(currVecId == -1){
                        currVecId = firstVecId;
                        orderedVec.addElement( firstVecId );
                        traveredPoint.put(firstVecId, 1);
                    } else if(processQueueVecs.size() > 0) {
                        currVecId = ((Integer)processQueueVecs.remove(0)).intValue();
                    } else {
                        currVecId = -1;
                    }
                    
                    Vec3 vec = verts[currVecId].r;
                    
                    // Get list of all verts connected to the currVec by edges.
                    for(int i = 0; i < edges.length; i++){
                        TriangleMesh.Edge edge = edges[i];
                        
                        if(currVecId == edge.v1){
                            if(traveredPoint.containsKey(edge.v2) == false){
                                orderedVec.addElement(edge.v2);
                                processQueueVecs.addElement(edge.v2);
                            }
                            traveredPoint.put(edge.v2, 1);
                            traveredEdge.put(i, 1);
                        }
                        if(currVecId == edge.v2){
                            if(traveredPoint.containsKey(edge.v1) == false){
                                orderedVec.addElement(edge.v1);
                                processQueueVecs.addElement(edge.v1);
                            }
                            traveredPoint.put(edge.v1, 1);
                            traveredEdge.put(i, 1);
                        }
                    }
                    
                    //counter++;
                    if( processQueueVecs.size() == 0 ){
                        done = true;
                    }
                }
                
                
                // TEST
                
                // Add connected point normals to list.
                for(int k = 0; k < edges.length; k++){
                    TriangleMesh.Edge edge = edges[k];
                    Vec3 a = verts[edge.v1].r;
                    Vec3 b = verts[edge.v2].r;
                    
                    Vec3 normalForce = new Vec3(a.x - b.x, a.y - b.y, a.z - b.z);
                    // add normalForce to both points a and b in pointForceVectors.
                    Vector va = pointForceVectors.get(a);
                    if( va == null ){
                        va = new Vector();
                    }
                    va.addElement(normalForce);
                    pointForceVectors.put(a, va);
                    
                    Vector vb = pointForceVectors.get(b);
                    if( vb == null ){
                        vb = new Vector();
                    }
                    vb.addElement(normalForce);
                    pointForceVectors.put(b, vb);
                }
                
                
                // print 
                for(int i = 0; i < orderedVec.size(); i++){
                    int vertId = ((Integer)orderedVec.get(i)).intValue();
                    Vec3 vec = verts[vertId].r;
                    //System.out.println("Point:  x: " + vec.x + " y: " + vec.y + " z: " + vec.z);
                    
                    
                    Vec3 overallForceVector = new Vec3();
                    Vector forceNormals = pointForceVectors.get(vec);
                    if(forceNormals != null){
                        for(int j = 0; j < forceNormals.size(); j++){
                            Vec3 forceNormal = (Vec3)forceNormals.elementAt(j);
                            //System.out.println("   force normal " + forceNormal.x + " " + forceNormal.y + " " + forceNormal.z);
                        }
                    } else {
                        System.out.println("Err.");
                    }
                }
                
                
                //
                // iterate ordered object points moving left to right based on inerta and geometry.
                //
                HashMap<Integer, Vector> forceTransferVectors = new HashMap<Integer, Vector>(); // force vectors transfered from prev
                HashMap movedPoint = new HashMap();
                for(int i = 0; i < orderedVec.size(); i++){
                    int vertId = ((Integer)orderedVec.get(i)).intValue();
                    Vec3 vec = verts[vertId].r;
                    
                    
                    // Calculate direction of connected forward edges (compression)
                    // Calculate direction of connected rearward edges (tension)
                    Vec3 connectionsForward = getPointConnectionsForward(vertId, verts, edges);
                    Vec3 anglesForward = getPointAnglesForward(vertId, verts, edges);
                    Vec3 anglesBack = getPointAnglesBack(vertId, verts, edges);
                    //double distanceBack = distanceFromPointsBehind(vertId, verts, edges);
                    
                    Mat4 mat4 = c.duplicate().fromLocal();
                    //mat4.transform(verts[i]);
                    //mat4.transform(vec);
                    
                    //System.out.println(" vec " + i + " " + orderedVec.get(i) + " x: " + vec.x + " " + vec.y + " " + vec.z );
                    
                    // Calculate vert (and edge connected verts) angle delta from horizontal impact.
                    // TODO.
                    
                    // for each edge connected to this vec, process movement deformation, momentum dissipation.
                    double avgAngle = 0;
                    int avgAngleCount = 0;
                    double movement = 0;
                    double yMovement = 0;
                    double zMovement = 0;
                    /*
                    for(int k = 0; k < edges.length; k++){
                        TriangleMesh.Edge edge = edges[k];
                        Vec3 vecCompare = null;
                        if(vertId == edge.v1){
                            vecCompare = verts[edge.v2].r;
                        }
                        if(vertId == edge.v2){
                            vecCompare = verts[edge.v1].r;
                        }
                        if(vecCompare != null
                           //&& vecCompare.x >= vec.x
                           ) { // ****
                            double angle = getAngle3(vec, vecCompare); // vec (current point) - compare (connected point)
                            //System.out.println("     comp- x: " + vecCompare.x + " " + vecCompare.y + " " + vecCompare.z + "  a: " + angle);
                            
                            avgAngleCount++;
                            avgAngle += angle;
                        }
                    }
                    if(avgAngleCount > 0){
                        avgAngle = avgAngle / avgAngleCount;
                    }
                    */
                    
                    // Math.abs(anglesForward.y)  Math.abs(anglesForward.z)
                    //System.out.println(" XXX  y: " + Math.abs(anglesForward.y)   );
                    
                    // Move point
                    //movement = 0.3 * (avgAngle / 2) * inerta; // (avgAngle * avgAngle)  include momentum--
                    movement = 0.2 * (Math.abs(anglesForward.y) + Math.abs(anglesForward.z) + 0.01) * inerta; // move more if angle is farther from 0.
                    
                    // abs(angle) = angle
                    // hypotinuse = constant (1)
                    // movement = Opposite
                    double forceFromAngleY = Math.sin( Math.abs(anglesForward.y) ) * 1; //
                    double forceFromAngleZ = Math.sin( Math.abs(anglesForward.z) ) * 1;
                    
                    double tensionFromAnglesY = Math.sin( Math.abs(anglesBack.y) ) * 1;
                    double tensionFromAnglesZ = Math.sin( Math.abs(anglesBack.z) ) * 1;
                    
                    double retard = (tensionFromAnglesY+tensionFromAnglesZ) / 1.7;
                    
                    movement = 0.2 * ((forceFromAngleY + forceFromAngleZ ) ) * inerta;
                    
                    
                    //System.out.println("  vec.x " + vec.x + "  angle: " + (forceFromAngleY+forceFromAngleZ) + "  R: " +  (tensionFromAnglesY+tensionFromAnglesZ)  + " m " + movement);
                    
                    // anglesBack.y anglesBack.z
                    
                    // Limit movement so that lengths in X axis from previous points are not exceeded.
                    //if( movement > distanceBack ){
                        
                    //}
                    
                    //System.out.println("movement: " + movement + " y " + anglesForward.y + " z " + anglesForward.z);
                    
                    yMovement = ( (connectionsForward.y * forceFromAngleY) / 60); // experiment
                    zMovement = ( (connectionsForward.z * forceFromAngleZ) / 60);
                    
                    inerta = inerta - (movement * 0.128); // absorb inerta from deformation of structure.
                    if(inerta < 0){
                        inerta = 0;
                        done = true;
                    }
                    
                    // ObjectInfo meshObj
                    // triangleMesh = meshObj.getObject().convertToTriangleMesh(0.0);  object3D
                    // MeshVertex[] verts = ((TriangleMesh)triangleMesh).getVertices();
                    
                    // object3D set vec3 data
                    
                    //meshObj.getObject().
                    // Update vertex location
                    Vec3 moved = vecPoints[ vertId ];
                    moved.x = moved.x + movement;
                    moved.y = moved.y + yMovement;
                    moved.z = moved.z + zMovement;
                    vecPoints[ vertId ] = moved;
                    
                    // forceTransferVectors
                    // Vec3 connectionsForward = getPointConnectionsForward(vertId, verts, edges);
                    // verts[vertId].r;
                    Vec3 forceTransfer = new Vec3(movement, yMovement, zMovement);
                    addForceVector(verts, edges, vertId, forceTransferVectors, forceTransfer);
                    
                    
                    //System.out.println(" x " + moved.x );
                    
                    if(moved.x > 5){
                        done = true;
                    }
                    
                    // o3d.setVertexPositions(vr); // clears cache mesh
                    // obj.setObject((Object3D)o3d);
                    // obj.clearCachedMeshes();
                    // dispatchSceneChangedEvent()
                    
                    //System.out.println("         Move: " + movement  + "   a: " + avgAngle + " inerta: " + inerta  );
                    
                } // for orderedVec
                
                
                // Post collide edge distances
                // tension and compression should be averaged among connected edges.
                /*
                DecimalFormat df = new DecimalFormat("###.######");
                HashMap<TriangleMesh.Edge, Double> postEdgeDistances = new HashMap<TriangleMesh.Edge, Double>();
                for(int i = 0; i < edges.length; i++){
                    TriangleMesh.Edge edge = edges[i];
                    double distance = verts[edge.v1].r.distance(verts[edge.v2].r);
                    postEdgeDistances.put(edge, distance);
                    //System.out.println("distance: " + distance);
                }
                for(int i = 0; i < verts.length; i++){
                    Vec3 vertex1 = verts[i].r;
                    System.out.println("  ");
                    double avg = 0;
                    for(int j = 0; j < edges.length; j++){
                        TriangleMesh.Edge edge = edges[j];
                        if(verts[edge.v1].r == vertex1 || verts[edge.v2].r == vertex1){
                            double pre = preEdgeDistances.get(edge);
                            double post = postEdgeDistances.get(edge);
                            double change = post - pre;
                            avg += change;
                            System.out.println("   pre " + pre + " - " + post + "    " + df.format(change) );
                        }
                    }
                    avg /= edges.length;
                    System.out.println("   avg " + df.format(avg));
                    for(int j = 0; j < edges.length; j++){
                        TriangleMesh.Edge edge = edges[j];
                        double pre = preEdgeDistances.get(edge);
                        double post = postEdgeDistances.get(edge);
                        double change = post - pre;
                        if( change >  avg  ){
                            // calculate direction point needs to move to even out tension/compression.
                        }
                    
                    }
                    
                }
                */
                
                // Shift object if moved.
                //for(int i = 0; i < orderedVec.size(); i++){
                //    int vertId = ((Integer)orderedVec.get(i)).intValue();
                //    Vec3 vec = verts[vertId].r;
                
                //}
                
                // Shift whole object to stay in frame
                //
                double lastVecX2 = -99999;
                for(int i = 0; i < verts.length; i++){
                    Vec3 vertex1 = verts[i].r;
                    //Mat4 mat4 = c.duplicate().fromLocal();
                    //mat4.transform(vertex1);
                    if(vertex1.x > lastVecX2){
                        lastVecX2 = vertex1.x;
                    }
                }
                double shifted = (lastVecX2 - lastVecX);
                System.out.println("1: " + lastVecX + " 2: " + lastVecX2 + "   shift: " + shifted);
                /*
                    Vec3 moved = vecPoints[ vertId ];
                    moved.x = moved.x + movement;
                    moved.y = moved.y + yMovement;
                    moved.z = moved.z + zMovement;
                    vecPoints[ vertId ] = moved;
                }
                */
                
                
                ((Mesh)meshObj.getObject()).setVertexPositions(vecPoints); // todo: check object is instance of type.
                meshObj.clearCachedMeshes();
                ((LayoutWindow)window).setModified();
                ((LayoutWindow)window).updateImage();
                
                /*
                 for(int i = 0; i < edges.length; i++){
                 //System.out.println(" edge " + i );
                 Edge edge = edges[i];
                 
                 Vec3 vertex1 = verts[edge.v1].r;
                 Vec3 vertex2 = verts[edge.v2].r;
                 System.out.println(" edge v1: " + edge.v1 + " v2: " + edge.v2 +
                 " : " + vertex1.x + " " + vertex1.y + " " + vertex1.z  + " - " +
                 vertex2.x + " " + vertex2.y + " " + vertex2.z); // Vec3
                 
                 
                 }
                 */
            
        }
    }


    /*
    // calculate mesh boundary.
    // ...
    BoundingBox meshBound = meshObj.getBounds();
    Vec3 meshCenter = meshBound.getCenter();
    System.out.println(" mesh centre:  " + meshCenter.x + " " + meshCenter.y + " " + meshCenter.z);
    Vec3 meshSize = meshBound.getSize();
    System.out.println(" mesh size:  " + meshSize.x + " " + meshSize.y + " " + meshSize.z);


    //BoundingBox wallBound = wall.getBounds();
    //Vec3 wallCenter = wallBound.getCenter();
    //System.out.println(" wall:  " + wallCenter.x + " " + wallCenter.y + " " + wallCenter.z);
    //Vec3 wallSize = wallBound.getSize();
    //System.out.println(" wall size:  " + wallSize.x + " " + wallSize.y + " " + wallSize.z);

    //
    for( int i = 0; i < 1; i++ ){
        //Camera cam = view.getCamera();
        Mat4 transform;
        //transform = Mat4.translation(0, 0, -0.12);
        //meshObj.getCoords().transformCoordinates(transform);
        
        // triangleMesh
        
        // Get geometry
        //ObjectInfo meshClone = meshObj.duplicate();
        Object co = (Object)meshObj.getObject();
        if(co instanceof Mesh && meshObj.isVisible() == true){
            Mesh meshObj3d = (Mesh)meshObj.getObject();
            Vec3 [] verts = meshObj3d.getVertexPositions();
            Vec3 [] normals = meshObj3d.getNormals();
            
            //System.out.println(" verts: " + verts.length + " normals " + normals.length);
            
            //for (Vec3 vert : verts){
            //  System.out.println(" point " + vert.x + " " + vert.y + " " + vert.z);
            //  if( vert.z < 0 ){
            //    vert.z = 0;
            //  }
            //}
            
            CoordinateSystem c;
            c = layout.getCoords(meshObj);
            Vec3 origin = c.getOrigin();
            
            
            for(int j = 0; j < verts.length; j++){
                Vec3 vert = verts[j];
                
                Mat4 mat4 = c.duplicate().fromLocal();
                mat4.transform(verts[j]);
                
                mat4.transform(vert);
                
                //System.out.print("  x: " + vert.x + "   y: " + vert.y + "   z: " + vert.z + "    " +
                //                 " " +  origin.x + "   " +
                //               " n " + normals[j].x + " " + normals[j].y + " " + normals[j].z);
                
                //verts[j].z -= 0.12; // Move Object (if Z > 0)
                double z = verts[j].z;
                if( z < 0.0 ){
                    //System.out.print(" < ");
                    //verts[j].z = 0;
                } else {
                    //System.out.print(" > ");
                }
                
                //System.out.println("");
            }
            // update model
            //meshObj3d.setVertexPositions( verts ); // affects on its own
            //meshObj.setObject( (Object3D) meshObj3d);
            
        }
        // ViewerCanvas view
        ((LayoutWindow)frame).updateImage();
        
        //
        //try { Thread.sleep(250); } catch (Exception e) {}
        */
    }
    
    
    /**
     * getPointConnectionsForward
     *
     * Description: calculate directional information of forward connecting points.
     */
    public Vec3 getPointConnectionsForward(int vertId, MeshVertex[] verts, TriangleMesh.Edge[] edges){
        Vec3 dir = new Vec3();
        Vec3 vec = verts[vertId].r;
        int count = 0;
        for(int k = 0; k < edges.length; k++){
            TriangleMesh.Edge edge = edges[k];
            Vec3 vecCompare = null;
            if(vertId == edge.v1){
                vecCompare = verts[edge.v2].r;
            }
            if(vertId == edge.v2){
                vecCompare = verts[edge.v1].r;
            }
            if(vecCompare != null && vecCompare.x >= vec.x){
                count++;
                dir.y += (vec.y - vecCompare.y);
                dir.z += (vec.z - vecCompare.z);
            }
        }
        if(count > 0){
            dir.y = dir.y / count;
            dir.z = dir.z / count;
        }
        //System.out.println("getPointConnectionsForward: y "+ dir.y + " z:  " +  dir.z);
        return dir;
    }
    
    public Vector getForwardPoints(int vertId, MeshVertex[] verts, TriangleMesh.Edge[] edges){
        Vector forwardPoints = new Vector();
        Vec3 vec = verts[vertId].r;
        for(int k = 0; k < edges.length; k++){
            TriangleMesh.Edge edge = edges[k];
            Vec3 vecCompare = null;
            if(vertId == edge.v1){
                vecCompare = verts[edge.v2].r;
            }
            if(vertId == edge.v2){
                vecCompare = verts[edge.v1].r;
            }
            if(vecCompare != null && vecCompare.x >= vec.x){
                forwardPoints.addElement(vecCompare);
            }
        }
        return forwardPoints;
    }
    
    public Vector getForwardPointIds(int vertId, MeshVertex[] verts, TriangleMesh.Edge[] edges){
        Vector forwardPoints = new Vector();
        Vec3 vec = verts[vertId].r;
        for(int k = 0; k < edges.length; k++){
            TriangleMesh.Edge edge = edges[k];
            Vec3 vecCompare = null;
            int forwardPointId = -1;
            if(vertId == edge.v1){
                //vecCompare = verts[edge.v2].r;
                forwardPointId = edge.v2;
            }
            if(vertId == edge.v2){
                //vecCompare = verts[edge.v1].r;
            }
            if(vecCompare != null && vecCompare.x >= vec.x){
                //forwardPoints.addElement(vecCompare);
            }
        }
        return forwardPoints;
    }
      
    /**
     * getAngle3
     *
     * Description:
     */
    public double getAngle3(Vec3 a, Vec3 b){
      double result = 0;
      
      // Temp not accurate
      result += Math.max(a.y, b.y) - Math.min(a.y, b.y);
      result += Math.max(a.z, b.z) - Math.min(a.z, b.z);
      
      double distance = a.distance(b);
      
      result = result / distance;
      
      return result;
    }
    
    /**
     * getPointAnglesForward
     *
     * Description: canculate angles to points connected in front of a given point.
     */
    public Vec3 getPointAnglesForward(int vertId, MeshVertex[] verts, TriangleMesh.Edge[] edges){
        Vec3 angles = new Vec3(0, 0, 0);
        Vec3 vec = verts[vertId].r;
        int count = 0;
        for(int k = 0; k < edges.length; k++){
            TriangleMesh.Edge edge = edges[k];
            Vec3 vecCompare = null;
            if(vertId == edge.v1){
                vecCompare = verts[edge.v2].r;
            }
            if(vertId == edge.v2){
                vecCompare = verts[edge.v1].r;
            }
            if(vecCompare != null && vecCompare.x > vec.x ){
                count++;
                //double distance = vec.distance(vecCompare); // Probably used as the hypotinuce
                double xOffset = Math.max(vec.x, vecCompare.x) - Math.min(vec.x, vecCompare.x);
                double yOffset = Math.max(vec.y, vecCompare.y) - Math.min(vec.y, vecCompare.y);
                double zOffset = Math.max(vec.z, vecCompare.z) - Math.min(vec.z, vecCompare.z);
                double yAngle = 0;
                //System.out.println("xOffset: " + xOffset);
                if(xOffset != 0){
                    yAngle = Math.atan(yOffset / xOffset); // yOffset = opposite, xOffet = adjacent
                }
                double zAngle = 0;
                if(xOffset != 0){
                    zAngle = Math.atan(zOffset / xOffset);
                }
                //if(vec.y > vecCompare.y){   // This doesn't work when symmetrical sides cancel out their angles
                //    yAngle = -yAngle;
                //}
                //if(vec.z > vecCompare.z){
                //    zAngle = -zAngle;
                //}
                angles.y += yAngle;
                angles.z += zAngle;
                //System.out.println("                yAngle " + yAngle + "  zAngle " + zAngle );
            }
        }
        if(count > 0){
            angles.y = angles.y / count;
            angles.z = angles.z / count;
        }
        //System.out.println("getPointAnglesForward: y "+ angles.y + " z:  " +  angles.z + "  count: " + count);
        return angles;
    }
    
    
    public Vec3 getPointAnglesBack(int vertId, MeshVertex[] verts, TriangleMesh.Edge[] edges){
        Vec3 angles = new Vec3(0, 0, 0);
        Vec3 vec = verts[vertId].r;
        int count = 0;
        for(int k = 0; k < edges.length; k++){
            TriangleMesh.Edge edge = edges[k];
            Vec3 vecCompare = null;
            if(vertId == edge.v1){
                vecCompare = verts[edge.v2].r;
            }
            if(vertId == edge.v2){
                vecCompare = verts[edge.v1].r;
            }
            if(vecCompare != null && vecCompare.x < vec.x ){
                count++;
                //double distance = vec.distance(vecCompare); // Probably used as the hypotinuce
                double xOffset = Math.max(vec.x, vecCompare.x) - Math.min(vec.x, vecCompare.x);
                double yOffset = Math.max(vec.y, vecCompare.y) - Math.min(vec.y, vecCompare.y);
                double zOffset = Math.max(vec.z, vecCompare.z) - Math.min(vec.z, vecCompare.z);
                double yAngle = 0;
                //System.out.println("xOffset: " + xOffset);
                if(xOffset != 0){
                    yAngle = Math.atan(yOffset / xOffset); // yOffset = opposite, xOffet = adjacent
                }
                double zAngle = 0;
                if(xOffset != 0){
                    zAngle = Math.atan(zOffset / xOffset);
                }
                //if(vec.y > vecCompare.y){   // This doesn't work when symmetrical sides cancel out their angles
                //    yAngle = -yAngle;
                //}
                //if(vec.z > vecCompare.z){
                //    zAngle = -zAngle;
                //}
                angles.y += yAngle;
                angles.z += zAngle;
                //System.out.println("                yAngle " + yAngle + "  zAngle " + zAngle );
            }
        }
        if(count > 0){
            angles.y = angles.y / count;
            angles.z = angles.z / count;
        }
        //System.out.println("getPointAnglesForward: y "+ angles.y + " z:  " +  angles.z + "  count: " + count);
        return angles;
    }
    
    
    /**
     * DEPRICATE
     *
     */
    public double distanceFromPointsBehind(int vertId, MeshVertex[] verts, TriangleMesh.Edge[] edges){
        double dist = -1;
        Vec3 vec = verts[vertId].r;
        int count = 0;
        for(int k = 0; k < edges.length; k++){
            TriangleMesh.Edge edge = edges[k];
            Vec3 vecCompare = null;
            if(vertId == edge.v1){
                vecCompare = verts[edge.v2].r;
            }
            if(vertId == edge.v2){
                vecCompare = verts[edge.v1].r;
            }
            if(vecCompare != null && vecCompare.x < vec.x){
                count++;
                dist = dist + (vec.x - vecCompare.x);
            }
        }
        if(count > 0){
            dist = dist / count;
        }
        return dist;
    }
    
    /**
     * addForceVector
     *
     * Description:
     */
    public void addForceVector(MeshVertex[] verts,
                               TriangleMesh.Edge[] edges,
                               int vecId,
                               HashMap<Integer, Vector> forceTransferVectors,
                               Vec3 forceTransferVec){
        Vec3 vec = verts[vecId].r;
        Vector forwardPoints = getForwardPoints(vecId, verts, edges);
        for(int i = 0; i < forwardPoints.size(); i++){
            Vec3 forwardVec = (Vec3)forwardPoints.elementAt(i);
            if(
               (forwardVec.y > 0 && forceTransferVec.y > 0 && forwardVec.z > 0 && forceTransferVec.z > 0) ||
               (forwardVec.y > 0 && forceTransferVec.y > 0 && forwardVec.z < 0 && forceTransferVec.z < 0) ||
               (forwardVec.y < 0 && forceTransferVec.y < 0 && forwardVec.z > 0 && forceTransferVec.z > 0) ||
               (forwardVec.y < 0 && forceTransferVec.y < 0 && forwardVec.z < 0 && forceTransferVec.z < 0)
               ){
                
                
                //forceTransferVectors.put(  , );
            }
        }
    }
    
    public Vec3 diminish(Vec3 force){
        
        return force;
    }
    
    /**
     * getForceTransferVec3
     *
     * Description:
     */
    public Vec3 getForceTransferVec3(int id){
        return new Vec3();
    }
}

