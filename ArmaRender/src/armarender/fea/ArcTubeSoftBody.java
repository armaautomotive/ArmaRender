/* Copyright (C) 2023 by Jon Taylor

 Phase 1
 
 1) update soft body algo to resist compression with a spring force like it resists expansion. The code comes from cloth simulation which allows contraction but we want to model a tube structure which resists both expansion and contraction.
 This should result in a triangulated structure holding its form rather than collapsing with user interaction or gravity forces.
 
 2) Add a modifier that if a compression value for each edge / rod / constraint exeeds a certain percentage value change then permanently reduce the length. This should have the effect of allowing the structure to deform when compressed and permanently stay permanently smaller after large impact. A physical material will have an elastic plastic deformation property where it will spring back from a small force but be modified after a threshold.
 
 3) Add a modifier that if a tension expansion value exceds a percentage value the constraint section will be removed simulating a physical material being stretched beyond its ability and breaking free. Would be nice if a arc tube that is broken would fall down front gravity to the ground plane.
 
 This should be testable by interacting with triangulated structures using a cube object aby dragging it into the tested object. The tested object should spring back to small impacts and be deformed or break by large forces.

 
 
 Phase 2)
 
 1) Durring a simulation calculate the total force value as the accumulation of forces applied to each rod / connection. Print the real time value to the gui. This should represent an energy level transformed from potential into the object structure.
 
 2) Durring a simulation calculate the total deformation distance as the accumulation of distance change for each point from the origional.
 
 I can use this data the force amsorbed by a triangulated object and the deformation distace to measure the performance of a design.

 3) Improve the cube collision code. Right now it makes changes mased on the size of the cube and its too forceful.
 
 
 Phase 3)
 
 1) Instead of simulating impacts with a moving impact plane or object and pinning or constraigning a back wall a new apprach would be to collect a force vector XYZ and apply motion to all of the selected object points causing it to move into a scene object that would act as the impact interaction. This would make testing different scenarios possible. This is an idea i’m open to suggestions.  This would make it so we don’t have to implement seperate mechanisms for a back wall and impact wall.
 
 2) Add a reset function which saves and restores the selected arc tube vectors and restores them to re reun a new simulation.

 

 */

package armarender.fea;

import armarender.*;
import armarender.object.*;
import armarender.math.*;
import armarender.view.CanvasDrawer;
import java.util.*;
import javax.swing.JOptionPane;
import armarender.texture.*;
import javax.swing.JFrame;
import java.awt.Rectangle;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Toolkit;
import java.awt.Font;
import java.awt.Color;
import java.awt.*;
import javax.swing.*;
import java.util.Random;
import java.awt.event.*;
import java.util.concurrent.*;
import java.text.DecimalFormat;
import java.awt.geom.Arc2D;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ArcTubeSoftBody {
    LayoutWindow window;
    SplineMesh splineMesh = null;
    ObjectInfo splineMeshInfo = null;
    boolean running = false;
    
    double accuracy = 5;
    double gravity = 0.1; // 400
    int clothY = 28;
    int clothX = 54;
    //double spacing = 0.8; // based on scene obvject
    double tearDist = 6.0;
    double friction = 0.99;
    double bounce = 0.5;
    
    int progressStart = 0;
    int progressEnd = 180;
    boolean progressCycle = true;
    
    Vector<Point> points = new Vector<Point>();
    public ClothThread clothThread = null;
    
    Vector<PointToArcReference> reference = new Vector<PointToArcReference>();
    
    private int acceptedChanges = 0;
    private int rejectedChanges = 0;
    private JLabel acceptedChangesLabel;
    private JLabel rejectedChangesLabel;
    private JLabel forceAbsorbedValueLabel;
    private JLabel deformationDistanceValueLabel;
    private JLabel timeValueLabel;
    private JCheckBox gravityCheck;
    private JCheckBox pinFirstCheck;
    
    private Rectangle prevBounds = null;
    
    
    public class mouse {
        double cut = 8;
        double influence = 26;
        boolean down = false;
        int button = 1;
        int x = 0;
        int y = 0;
        int px = 0;
        int py = 0;
    }
    
    public class TubeContainer {
        public ObjectInfo info;
        public Vec3[] points;
        public Vec3[] previousPoints;
        public BoundingBox bounds;
        public Vector<BoundingBox> segmentBounds = new Vector<BoundingBox>();
    }
    
    
    public class PointToArcReference {
        public Point point;
        public ObjectInfo info;
        public int vertIndex = 0;
    }
    
    /**
     * Cloth
     *
     */
    public ArcTubeSoftBody(LayoutWindow window){
        this.window = window;
        init();
        running = true;
        clothThread = new ClothThread();
        //clothThread.start(); // this worker is now being done in the displayUI thread.
        
        displayUI();
    }
    
    public void setPrevBounds(Rectangle bounds){
        prevBounds = bounds;
    }
    
    /**
     *
     *
     */
    public void init(){
        // Get selected SplineMesh
        
        Vector<TubeContainer> tubeContainers = new Vector<TubeContainer>();
        
        //
        // Get list of selected tubes to route.
        //
        tubeContainers = getTubeContainers();
        
        loadTubeStructure(tubeContainers);
        
        
        /*
        // Get selected scene object to assign the material to.
        int sel[] = window.getScene().getSelection();
        Vector<ObjectInfo> sceneObjects = window.getScene().getObjects();
        for(int i = 0; i < sel.length; i++){
            int selectionIndex = sel[i];
            //System.out.println("selectionIndex: " +  selectionIndex);
            ObjectInfo selectedObject = sceneObjects.elementAt(selectionIndex);
            //System.out.println("Selected Object: " + selectedObject.getName());
            if(selectedObject.getObject() instanceof SplineMesh){
                splineMesh = (SplineMesh)selectedObject.getObject();
                splineMeshInfo = selectedObject;
            }
        }
        if(splineMesh == null){
            System.out.println("No SplineMesh Selected.");
            JOptionPane.showMessageDialog(null, "Select a SplineMesh object.",  "Alert" , JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        MeshVertex[] verts = splineMesh.getVertices(); // splineMeshInfo.getObject().getVertices();
        
        // Translate verts to world coords.
        // TODO
        
        int meshU = splineMesh.getUSize();
        int meshV = splineMesh.getVSize();
        // MeshVertex getVertex(int u, int v)
        this.clothX = meshU;
        this.clothY = meshV;
        //System.out.println(" meshU: " + meshU + " meshV " + meshV );
        
        double startX = 0;
        for(int y = 0; y < clothY; y++){
            for(int x = 0; x < clothX; x++){
                MeshVertex vert = splineMesh.getVertex(x, y);
                Point point = new Point( vert.r.x, vert.r.y, vert.r.z  ); // set inital location
                //Point point = new Point( startX + x * spacing, 20 + y * spacing, 0);
                if(y == 0){                                                     // Pin first row (X)
                    point.pin = new Vec3(point.p.x, point.p.y, point.p.z);
                }
                if(x != 0){
                    //point.attach(this.points[this.points.length - 1]) ;
                    point.attach( points.elementAt(points.size() -1)  );        // attach the prev point
                }
                if(y != 0){                                                     // attach upper point
                    // vertex[u+usize*v]; // u=x, y=v   x + meshU * y
                    int pIndex = x + clothX * (y-1);
                    Point p = points.elementAt(pIndex);
                    point.attach(p);
                    //point.attach( points.elementAt( x + (y-1) * (clothX + 1) )  ); // ???
                }
                points.addElement(point);
            }
        }
         */
    }
    
    
    /**
     * getTubeContainers
     *
     */
    public Vector<TubeContainer> getTubeContainers(){
        LayoutModeling layout = new LayoutModeling();
        Vector<TubeContainer> tubeContainers = new Vector<TubeContainer>();
        Scene scene = window.getScene();
        int selection[] = scene.getSelection();
        for(int i = 0; i < selection.length; i++){
            ObjectInfo objectInfo = scene.getObject(selection[i]);
            Object obj = (Object)objectInfo.getObject();
            //System.out.println(" -> " + objectInfo.getName() + " " + obj.getClass().getName() );
            if(obj instanceof ArcObject){
                ArcObject arc = (ArcObject)obj;
                //System.out.println(" YES " );
                CoordinateSystem objectCS;
                objectCS = layout.getCoords(objectInfo);
                Mat4 mat4 = objectCS.duplicate().fromLocal();
                Vec3 verts[] = arc.getVertexPositions();
                Vec3 translatedVerts[] = new Vec3[verts.length];
                Vec3 previousTranslatedVerts[] = new Vec3[verts.length];
                for(int j = 0; j < verts.length; j++){
                    Vec3 vec = new Vec3(verts[j]);
                    mat4.transform(vec);
                    translatedVerts[j] = new Vec3(vec);
                    previousTranslatedVerts[j] = new Vec3(vec);
                }
                TubeContainer container = new TubeContainer();
                container.info = objectInfo;
                container.points = translatedVerts;
                container.previousPoints = previousTranslatedVerts;
                container.bounds = objectInfo.getTranslatedBounds();
                for(int j = 1; j < verts.length; j++){
                    Vec3 vecA = new Vec3(verts[j - 1]);
                    Vec3 vecB = new Vec3(verts[j - 0]);
                    BoundingBox segBounds = new BoundingBox(vecA, vecB);
                    container.segmentBounds.addElement(segBounds);
                }
                tubeContainers.addElement(container);
            }
        }
        return tubeContainers;
    }
    
    
    
    public double getObjectMass(){
        double mass = 0;
        LayoutModeling layout = new LayoutModeling();
        PhysicalMaterial pm = new PhysicalMaterial(window, window, window.getScene());
        pm.loadMaterials();
        Vector<TubeContainer> tubeContainers = new Vector<TubeContainer>();
        Scene scene = window.getScene();
        int selection[] = scene.getSelection();
        for(int i = 0; i < selection.length; i++){
            ObjectInfo objectInfo = scene.getObject(selection[i]);
            Object obj = (Object)objectInfo.getObject();
            if(obj instanceof ArcObject){
                ArcObject arc = (ArcObject)obj;
                double length = arc.getLength();
                //int materialId = objectInfo.getPhysicalMaterialId();
                PhysicalMaterial.MaterialContainer material = pm.getMaterial(objectInfo);
                if(material != null){
                    mass += length * material.weight;
                }
            }
        }
        return mass;
    }
    
    /**
     * getObjectTriangulationScore
     *
     * Description: score measures relatively how triangulated a structure / mesh is. A equal length triangular pyramid is optional: 1, and an equal length edge cube is the worst: 0.
     *  This function is used as a heuristic in determining the structural integrity of a design. Actual FEA would be more usefull.
     */
    public double loadTubeStructure(Vector<TubeContainer> tubes){
        
        // Collect all points to consider.
        Vector<Vec3> arcPoints = new Vector<Vec3>();
        
        double nodeSnapDist = 0;    // distance allowed to still connect multiple tube ends to a shared vert node.
        for(int i = 0; i < tubes.size(); i++){
            TubeContainer tube = tubes.elementAt(i);
            ArcObject arc = (ArcObject)tube.info.getObject();
            if( arc.getTubeDiameter() > nodeSnapDist ){
                nodeSnapDist = arc.getTubeDiameter();
            }
        }
        
        for(int i = 0; i < tubes.size(); i++){
            TubeContainer tube = tubes.elementAt(i);
            //ArcObject arc = (ArcObject)tube.info.getObject();
            //points.addAll( tube.points ); // todo don't add duplicates.
            for(int j = 0; j < tube.points.length; j++){
                
                boolean exists = false;
                
                for(int k = 0; k < points.size(); k++){
                    Vec3 existingPoint = points.elementAt(k).p;
                    double dist = existingPoint.distance(tube.points[j]);
                    if(dist < nodeSnapDist){
                        exists = true;
                        //existingPoint =
                    }
                }
            
                // does a point allready exist in this location?
                // If not then add to the list.
                if(exists == false){
                    Vec3 vec = new Vec3(tube.points[j]);
                    arcPoints.addElement( vec );
                    //System.out.println("  add " + tube.points[j]);
                    
                    Point point = new Point( vec.x, vec.y, vec.z  );
                    
                    // && (pinFirstCheck != null && pinFirstCheck.isSelected() == true) // doesn't work because the pin check box hasn't been initalized.
                    if(points.size() == 0){ // 0 // just for demo (later look for the desired point based on its location.)
                        point.pin = new Vec3(point.p.x, point.p.y, point.p.z);
                    }
                    
                    points.addElement(point);
                    
                    PointToArcReference ref = new PointToArcReference();
                    ref.point = point;
                    ref.info = tube.info;
                    ref.vertIndex = j;
                    reference.addElement(ref); // allow soft body solver Point/Constraignt calculations to be back ported to the display geometry (ObjectInfo/ArcObject)
                    
                    //System.out.println("Adding reference " + point.p + "  " +  tube.info.getName() + " : " + j);
                    
                } else {    // dont add point because it allready exists
                    //System.out.println("  dup " + tube.points[j]);
                    Vec3 vec = new Vec3(tube.points[j]);
                    
                    // Find Point
                    Point existingPoint = null;
                    for(int x = 0; x < points.size(); x++ ){
                        Point currPoint = points.elementAt(x);
                        if(currPoint.p.distance( vec ) < nodeSnapDist ){
                            existingPoint = currPoint;
                        }
                    }
                    
                    if(existingPoint != null){
                        PointToArcReference ref = new PointToArcReference();
                        ref.point = existingPoint;
                        ref.info = tube.info;
                        ref.vertIndex = j;
                        reference.addElement(ref);
                    }
                }
            }
        }
        /*
        // Add arc tube points to soft body list
        for(int i = 0; i < arcPoints.size(); i++){
            Vec3 vec = arcPoints.elementAt(i);
            System.out.println(" point " + i + " " + arcPoints.elementAt(i));
            Point point = new Point( vec.x, vec.y, vec.z  );
            
            if(i == 0){ // just for demo (later look for the desired point based on its location.)
                point.pin = new Vec3(point.p.x, point.p.y, point.p.z);
            }
            points.addElement(point);
        }
         */
        
        //
        // Get edges (Constraints)
        //
        for(int i = 0; i < arcPoints.size(); i++){
            Vec3 vec = arcPoints.elementAt(i);
            
            //System.out.println(".");
            
            // 1) Find all segments that connect with this point
            for(int a = 0; a < tubes.size(); a++){
                TubeContainer tube = tubes.elementAt(a);
                //ArcObject arc = (ArcObject)tube.info.getObject();
                //points.addAll( tube.points ); // todo don't add duplicates.
                for(int aj = 0; aj < tube.points.length; aj++){
                    
                    double dist = tube.points[aj].distance( vec );
                    if(dist < nodeSnapDist){
                        // Point A on the edge / rod / constraignt is (tube.points[aj])
                        Point pointA = null;
                        for(int x = 0; x < points.size(); x++){
                            if( points.elementAt(x).p.distance( vec ) < nodeSnapDist ){
                                pointA = points.elementAt(x);
                            }
                        }
                        
                        // Now find connecting point the constraignt will be attached to
                        if(aj > 0){
                            Vec3 prevVec = null;
                            prevVec = tube.points[aj - 1];
                            Point pointB = null;
                            
                            for(int x = 0; x < points.size(); x++){
                                if( points.elementAt( x ).p.distance( prevVec ) < nodeSnapDist ){
                                    pointB = points.elementAt(x);
                                }
                            }
                            if(pointA != null && pointB != null){
                                pointA.attach(pointB);
                                //System.out.println("ATTACH Prev");
                                
                                //addDebugCurve( pointA.p , pointB.p );
                            }
                        }
                        
                        if(aj < tube.points.length - 1){
                            Vec3 nextVec = null;
                            nextVec = tube.points[aj + 1];
                            Point pointB = null;
                            for(int x = 0; x < points.size(); x++){
                                if( points.elementAt( x ).p.distance( nextVec ) < nodeSnapDist ){
                                    pointB = points.elementAt(x);
                                }
                            }
                            if(pointA != null && pointB != null){
                                pointA.attach(pointB);
                                //System.out.println("ATTACH Next");
                                
                                //addDebugCurve( pointA.p , pointB.p );
                            }
                        }
                        
                    }
                }
            }
        }
        
        // Line.shortestDistance
        // Point p = points.elementAt(pIndex);
        // point.attach(p);
        
        return 1;
    }
    
    
    /**
     * setUpdatedGeometry
     *
     * Description: Set the mesh geometry. Form of interactivity as objects can be modied with Splinemesh editor.
     */
    public void setUpdatedGeometry(ObjectInfo info){
        if(info.getObject() instanceof SplineMesh){
            SplineMesh splineMesh = (SplineMesh)info.getObject();
            int meshU = splineMesh.getUSize();
            int meshV = splineMesh.getVSize();
            this.clothX = meshU;
            this.clothY = meshV;
            //System.out.println("UPDATE clothX " + clothX + " clothY " + clothY);
            for(int y = 1; y < clothY; y++){
                for(int x = 0; x < clothX; x++){
                    int pIndex = x + clothX * (y-1);
                    //System.out.println(" pIndex " + pIndex + " " + x + " " + y);
                    Point p = points.elementAt(pIndex);
                    MeshVertex vert = splineMesh.getVertex(x, y);
                    p.p.x = vert.r.x;
                    p.p.y = vert.r.y;
                    p.p.z = vert.r.z;
                }
            }
        }
    }
    
    
    /**
     * Point
     *
     */
    public class Point {
        Vec3 p = new Vec3();
        Vec3 p2 = new Vec3();
        Vec3 v = new Vec3();
        Vec3 pin = null;
        Vector<Constraint> constraints = new Vector<Constraint> ();
        
        ArcObject arcObject = null;
        int arcObjectIndex = 0;
        
        public Point (double x, double y, double z) {
            this.p.x = x;               // Location
            this.p.y = y;
            this.p.z = z;

            this.p2.x = x; // px->p2.x  // Previous location
            this.p2.y = y;
            this.p2.z = z;

            this.v.x = 0;               // Velovity
            this.v.y = 0;
            this.v.z = 0;

            this.pin = null;            // pin fix in place.
        }

        
        
        public void update(double delta) {
            LayoutModeling layout = new LayoutModeling();
            //if (this.pinX && this.pinY) return; //  this
            if(this.pin != null){
                //System.out.println("Point.update point is pined. " );
                return;
            }
           
            /*
             // Mouse click drag
            if (mouse.down) {
                let dx = this.x - mouse.x; //  this.p.x = mouse.x
                let dy = this.y - mouse.y
                let dist = Math.sqrt(dx * dx + dy * dy)

                if (mouse.button === 1 && dist < mouse.influence) {
                    this.px = this.x - (mouse.x - mouse.px)
                    this.py = this.y - (mouse.y - mouse.py)
                } else if (dist < mouse.cut) {
                    this.constraints = []
                }
             }
             */
            
            // Collide with world
            boolean collided = false;
            
            Vector<ObjectInfo> worldObjects = window.getScene().getObjects();
            for(int i = 0; i < worldObjects.size(); i++){
                ObjectInfo currInfo = (ObjectInfo)worldObjects.elementAt(i);
                //if(splineMeshInfo != null && currInfo.getId() != splineMeshInfo.getId()){         // dont check against self
                if( currInfo.getObject() instanceof Cube ){
                    BoundingBox bounds = currInfo.getTranslatedBounds();
                    Vec3 currCenter = bounds.getCenter();
                    Vec3 translatedP = new Vec3(p);
                    CoordinateSystem c;
                    //c = layout.getCoords(splineMeshInfo);
                    //Mat4 mat4 = c.duplicate().fromLocal();
                    //mat4.transform(translatedP);
                    if(bounds.contains(translatedP)){
                        //System.out.println("colide with: " + currInfo.getName() );
                        collided = true;
                        
                        // Y Height
                        double collideY = translatedP.minus(currCenter).y;
                        this.p2.y = this.p.y - (0.2 * collideY); //  (mouse.y - mouse.py)
                        //this.p2.y = this.p.y - (0.2); // Old
                        
                        double collideX = translatedP.minus(currCenter).x;      // X
                        this.p2.x = this.p.x - (collideX * 0.2);
                        
                        double collideZ = translatedP.minus(currCenter).z;
                        this.p2.z = this.p.z - (collideZ * 0.2);
                    }
                }
            }

            // if collided reduce gravity?
            double currGravity = gravity;
            if(collided){
                //currGravity = currGravity * 0.25;
                currGravity = 0;
            }
            
            if(gravityCheck.isSelected() == false){
                currGravity = 0;
            }
            
            this.addForce(0, -currGravity, 0);

            double nx = this.p.x + (this.p.x - this.p2.x) * friction + this.v.x * delta;
            double ny = this.p.y + (this.p.y - this.p2.y) * friction + this.v.y * delta;
            double nz = this.p.z + (this.p.z - this.p2.z) * friction + this.v.z * delta;
            
            this.p2.x = this.p.x;
            this.p2.y = this.p.y;
            this.p2.z = this.p.z;

            this.p.x = nx;
            this.p.y = ny;
            this.p.z = nz;

            this.v.y = this.v.x = 0;
            this.v.z = 0;

            /*
            // Bounds
            if (this.x >= canvas.width) {
                this.px = canvas.width + (canvas.width - this.px) * bounce
                this.x = canvas.width
            } else if (this.x <= 0) {
                this.px *= -1 * bounce
                this.x = 0
            }

            if (this.y >= canvas.height) {
                this.py = canvas.height + (canvas.height - this.py) * bounce
                this.y = canvas.height
            } else if (this.y <= 0) {
                this.py *= -1 * bounce
                this.y = 0
            }
            */
            //return this
        } // end update

        public void draw () {
            //let i = this.constraints.length
            //while (i--) this.constraints[i].draw()
        }

        public void resolve () {
            //if (this.pin  && this.pinY) {
            if (this.pin != null) {
                this.p.x = this.pin.x;
                this.p.y = this.pin.y;
                this.p.z = this.pin.z;
                return;
            }

            //this.constraints.forEach((constraint) => constraint.resolve())
            for(int i = 0; i < constraints.size(); i++){
                Constraint c = (Constraint)constraints.elementAt(i);
                c.resolve();
            }
        }

        public void attach (Point point) {
            //this.constraints.push(new Constraint(this, point));
            this.constraints.addElement(new Constraint(this, point));
        }

        //free (constraint) {
        //  this.constraints.splice(this.constraints.indexOf(constraint), 1)
        //}

        public void addForce (double x, double y, double z) {
          this.v.x += x;
          this.v.y += y;
          this.v.z += z;
        }

        public void pin (double pinx, double piny, double pinz) {
          if(pin == null){
              pin = new Vec3();
          }
          this.pin.x = pinx;
          this.pin.y = piny;
          this.pin.z = pinz;
        }
    }
    

    public class Constraint {
        Point p1;
        Point p2;
        double length = 0;

        public Constraint (Point p1, Point p2) {
          this.p1 = p1;
          this.p2 = p2;
          //this.length = spacing; // Save origional spacing
          this.length = p1.p.distance( p2.p );
        }

        /**
         * Constraint.resolve()
         */
        public void resolve () {
            double dx = this.p1.p.x - this.p2.p.x;
            double dy = this.p1.p.y - this.p2.p.y;
            double dz = this.p1.p.z - this.p2.p.z;
            //double dist = Math.sqrt(dx * dx + dy * dy);
            // x = this.p1.p.x
            // v.x = this.p2.p.x
            // y = this.p1.p.y
            // v.y = this.p2.p.y
            // z = this.p1.p.z
            // v.z = this.p2.p.z
            double dist = Math.sqrt((this.p2.p.x - this.p1.p.x) * (this.p2.p.x - this.p1.p.x) +
                             (this.p2.p.y - this.p1.p.y) * (this.p2.p.y - this.p1.p.y) +
                             (this.p2.p.z - this.p1.p.z) * (this.p2.p.z - this.p1.p.z));

            //System.out.println(" Constraint resolve length " + length + " dist " + dist);
            
            if (dist < this.length)        // fabric is allowed to be closer than length
            {
                // Expansion calculation.
                /*
                double diff = (this.length - dist) / dist;

                //if (dist > tearDist) this.p1.free(this)

                double mul = diff * 0.1 * (1 - this.length / dist);

                double px = dx * mul;
                double py = dy * mul;
                double pz = dz * mul;
                //py = -py;
                
                //!this.p1.pinX && (this.p1.x += px)
                //!this.p1.pinY && (this.p1.y += py)
                //!this.p2.pinX && (this.p2.x -= px)
                //!this.p2.pinY && (this.p2.y -= py)
                if(p1.pin == null){                 // If not pinned, then move
                    this.p1.p.x += px;
                    this.p1.p.y += py;
                    this.p1.p.z += pz;
                }
                if(p2.pin == null){
                    this.p2.p.x -= px;
                    this.p2.p.y -= py;
                    this.p2.p.z -= pz;
                }
                */
                return;
            }
            

            double diff = (this.length - dist) / dist;

            //if (dist > tearDist) this.p1.free(this)

            double mul = diff * 0.5 * (1 - this.length / dist);

            double px = dx * mul;
            double py = dy * mul;
            double pz = dz * mul;
            //py = -py;
            
            //!this.p1.pinX && (this.p1.x += px)
            //!this.p1.pinY && (this.p1.y += py)
            //!this.p2.pinX && (this.p2.x -= px)
            //!this.p2.pinY && (this.p2.y -= py)
            if(p1.pin == null){                 // If not pinned, then move
                this.p1.p.x += px;
                this.p1.p.y += py;
                this.p1.p.z += pz;
            }
            if(p2.pin == null){
                this.p2.p.x -= px;
                this.p2.p.y -= py;
                this.p2.p.z -= pz;
            }
        }

        public void draw () {
          //ctx.moveTo(this.p1.x, this.p1.y)
          //ctx.lineTo(this.p2.x, this.p2.y)
        }
    }
    
    
    
    /**
     * update
     *
     */
    public void update(double delta){
        //System.out.println("update");
        //if(splineMesh == null){
        //    return;
        //}
        for(int a = 0; a < accuracy; a++){
            for(int i = 0; i < points.size(); i++){
                Point point = (Point)points.elementAt(i);
                point.resolve();
            }
        }
        
        Vec3 [] updatedMeshPoints = new Vec3[points.size()];
        for(int i = 0; i < points.size(); i++){
            Point point = (Point)points.elementAt(i);
            point.update(delta * delta);
            
            updatedMeshPoints[i] = new Vec3( point.p.x, point.p.y, point.p.z );
        }
        
        // Take updated points data and update the arcTubes
        
        for(int i = 0; i < points.size(); i++){
            Point point = (Point)points.elementAt(i);
            
            for(int r = 0; r < reference.size(); r++){
                PointToArcReference ref = reference.elementAt(r);
                if(ref.point == point){
                    
                    ObjectInfo info = ref.info;
                    ArcObject arc = (ArcObject)info.getObject();
                    Vec3 [] verts = arc.getVertexPositions();
                    verts[ref.vertIndex] = new Vec3(point.p.x, point.p.y, point.p.z);
                    arc.setVertexPositions(verts);
                    
                    // zero CoordSystem
                    info.setCoords ( new CoordinateSystem() ); // This is because the solver moves points and we can't yet calculate where the verts would be after translation.
                    
                    info.clearCachedMeshes();
                }
            }
        }
        
        // Update mesh points
        /*
        splineMesh.setVertexPositions(updatedMeshPoints);
        splineMeshInfo.clearCachedMeshes();
         */
        
        window.updateImage();
        
        /*
          double i = accuracy;
          while (i--) {
            this.points.forEach((point) => {
              point.resolve();
            })
          }
          //ctx.beginPath();
          this.points.forEach((point) => {
            point.update(delta * delta).draw()
          })
          //ctx.stroke()
      */
    }
    
    
    /**
     * ClothThread
     *
     */
    public class ClothThread extends Thread {
        ObjectInfo obj;
        public ClothThread(){}
        public void setObject(ObjectInfo obj){
            this.obj = obj;
        }
        public void run() {
            while( true ){
                if(running){
                    update(1);
                }
                try {
                    Thread.sleep(50);
                } catch (Exception e){
                }
            }
            //progressDialog.setVisible(false);
        }
    }
    
    
    public boolean isRunning(){
        return running;
    }
    public void stop(){
        running = false;
        points.clear();
        splineMesh = null;
    }
    public void start(){
        init();
        running = true;
    }
    
    
    
    
    
    public void addDebugCurve( Vec3 a , Vec3 b ){
        
        Vec3[] points = new Vec3[2];
        points[0] = new Vec3(a);
        points[1] = new Vec3(b);
        float[] smoothness = new float[2];
        smoothness[0] = 0;
        smoothness[1] = 0;
        
        Curve curve = new Curve(points, smoothness, 0, false ); // Vec3 v[], float smoothness[], int smoothingMethod, boolean isClosed
        ObjectInfo info = new ObjectInfo(curve, new CoordinateSystem(), "ROD");
        Scene scene = window.getScene();
        
        scene.addObject(info, null);
        
        window.updateImage();
        window.updateTree();
        // window
    }
    
    
    
    /**
     * routeEqualLength
     *
     * Description:
     */
    public void displayUI(){
        LayoutModeling layout = new LayoutModeling();
        int fontSize = 16;
        int secondColumn = 165;
        Thread thread = new Thread(){
            boolean running = true;
            public void run(){
                
                long start = System.currentTimeMillis();
                
                // Dialog
                Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                JFrame dialog = new JFrame();
                dialog.setTitle("Arc Tube Soft Body");
                JPanel leftPanel = new JPanel();
                leftPanel.setSize(300, dim.height);
                leftPanel.setMinimumSize(new Dimension(300, dim.height));
                leftPanel.setPreferredSize(new Dimension(300, dim.height));
                leftPanel.setBackground(new Color(255,255,255));
                leftPanel.setLayout(null);
                
                
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        running = false;
                        
                        // restore main window size.
                        if(prevBounds != null && window != null){
                            window.setBounds(prevBounds);
                        }
                    }
                });
                
                int y = 12;
                
                //y+= (ySeperation/2);
                
                JLabel titleLabel = new JLabel("Soft Body");
                Font font = titleLabel.getFont();
                titleLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                titleLabel.setBounds(20 + 3, y, dim.width, 33);
                leftPanel.add(titleLabel);
                 
                
                y += 24;
                y += 24;
                
                JLabel forceAbsorbedLabel = new JLabel("Force Absorbed: " );
                forceAbsorbedLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                forceAbsorbedLabel.setBounds(20 + 3, y, dim.width, 33);
                leftPanel.add(forceAbsorbedLabel);
                
                forceAbsorbedValueLabel = new JLabel("0");
                forceAbsorbedValueLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                forceAbsorbedValueLabel.setBounds(160 + 35, y, dim.width, 33);
                leftPanel.add(forceAbsorbedValueLabel);
                
                y += 24;
                
                JLabel deformationLabel = new JLabel("Deformation Length: " );
                deformationLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                deformationLabel.setBounds(20 + 3, y, dim.width, 33);
                leftPanel.add(deformationLabel);
                
                deformationDistanceValueLabel = new JLabel("0");
                deformationDistanceValueLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                deformationDistanceValueLabel.setBounds(160 + 35, y, dim.width, 33);
                leftPanel.add(deformationDistanceValueLabel);
                
                y += 24;
                
                
                
                y += 24;
                // Time
                JLabel timeLabel = new JLabel("Time: " );
                timeLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                timeLabel.setBounds(20 + 3, y, dim.width, 33);
                leftPanel.add(timeLabel);
                
                timeValueLabel = new JLabel("0");
                timeValueLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                timeValueLabel.setBounds(160 + 35, y, dim.width, 33);
                leftPanel.add(timeValueLabel);
                
                
                
                y += 24;
                y += 24;
                
                JLabel gravityLabel = new JLabel("Gravity: " );
                gravityLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                gravityLabel.setBounds(20 + 3, y, dim.width, 33);
                leftPanel.add(gravityLabel);
                gravityCheck = new JCheckBox("Gravity");
                gravityCheck.setSelected(false);
                gravityCheck.setBounds(160 + 35, y, 60, 30);
                leftPanel.add(gravityCheck);
                gravityCheck.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if(e.getStateChange() == ItemEvent.SELECTED) { //checkbox has been selected
                            // ...
                        } else { //checkbox has been deselected
                        };
                    }
                });
                
                
                y += 24;
                y += 24;
                
                JLabel pinFirstLabel = new JLabel("Pin First Point: " );
                pinFirstLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                pinFirstLabel.setBounds(20 + 3, y, dim.width, 33);
                leftPanel.add(pinFirstLabel);
                pinFirstCheck = new JCheckBox("Pin First Point");
                pinFirstCheck.setSelected(false);
                pinFirstCheck.setBounds(160 + 35, y, 60, 30);
                leftPanel.add(pinFirstCheck);
                pinFirstCheck.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if(e.getStateChange() == ItemEvent.SELECTED) { //checkbox has been selected
                            // add pin
            
                            if(points.size() > 0){
                                Point p = points.elementAt(0);
                                p.pin = new Vec3( p.p );
                            }
                            
                        } else { //checkbox has been deselected
                            // remove pin
                            
                            for( int i = 0; i < points.size(); i++){
                                Point p = points.elementAt(i);
                                p.pin = null;
                            }
                            
                        };
                    }
                });
                
                
                y += 24;
                y += 24;
                
                
                JLabel forceVectorLabel = new JLabel("Object Motion Force Vector: " );
                forceVectorLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                forceVectorLabel.setBounds(20 + 3, y, dim.width, 33);
                leftPanel.add(forceVectorLabel);
                y += 24;
                
                JTextField forceVectorXField = new JTextField("-1");
                forceVectorXField.setBounds(20 + 3, y, 80, 33);
                leftPanel.add(forceVectorXField);
                
                JTextField forceVectorYField = new JTextField("0");
                forceVectorYField.setBounds(20 + 3 + 85, y, 80, 33);
                leftPanel.add(forceVectorYField);
                
                JTextField forceVectorZField = new JTextField("0");
                forceVectorZField.setBounds(160 + 35, y, 80, 33);
                leftPanel.add(forceVectorZField);
                
                
                y += 24;
                y += 24;
                
                JLabel forceNewtonsLabel = new JLabel("Force Newtons: " );
                forceNewtonsLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                forceNewtonsLabel.setBounds(20 + 3, y, 150, 33);
                leftPanel.add(forceNewtonsLabel);
                
                JTextField forceNewtonsValueField = new JTextField("1000");
                forceNewtonsValueField.setBounds(160 + 35, y, 80, 33);
                leftPanel.add(forceNewtonsValueField);
                
                y += 24;
                y += 24;
                
                JLabel massLabel = new JLabel("Mass (kgs): " );
                massLabel.setFont(new Font(font.getName(), font.getStyle(), fontSize));
                massLabel.setBounds(20 + 3, y, 150, 33);
                leftPanel.add(massLabel);
                
                double objectMass = getObjectMass();
                JTextField massValueField = new JTextField("" + roundOne(objectMass));
                massValueField.setBounds(160 + 35, y, 80, 33);
                leftPanel.add(massValueField);
                
                
                
                y += 24;
                y += 24;
                
                JButton stopButton = new JButton("Stop");
                stopButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        running = false;
                    }
                });
                stopButton.setBounds(20 + 3, y + 50, 80 , 33);
                leftPanel.add(stopButton);
                
                
                JButton resetButton = new JButton("Reset");
                resetButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        running = false;
                        // TODO: load inital vert data into objects.
                        
                    }
                });
                resetButton.setBounds(20 + 3 + 85, y + 50, 80 , 33);
                leftPanel.add(resetButton);
                
                
                JButton startButton = new JButton("Start");
                startButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        running = true;
                    }
                });
                startButton.setBounds(160 + 35, y + 50, 80, 33);
                leftPanel.add(startButton);
                
                startButton.setEnabled(false); // temporary disable because start isn't coded.
                
                
                dialog.add(leftPanel, BorderLayout.CENTER);
                
                Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                Dimension windowDim = new Dimension((screenBounds.width), (screenBounds.height));
                //setBounds(new Rectangle((screenBounds.width-windowDim.width), (screenBounds.height-windowDim.height), windowDim.width, windowDim.height));
                dialog.setLocation(windowDim.width - 300,0); // dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
                dialog.setPreferredSize(new Dimension(300, windowDim.height)); // windowDim.width
                dialog.pack();

                dialog.setAlwaysOnTop(true);
                
                ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
                dialog.setIconImage(iconImage.getImage());
                
                //5. Show it.
                dialog.setVisible(true);
                
        
                Scene scene = window.getScene();
                int selection[] = scene.getSelection();
                
                //tubeContainers = new Vector<TubeContainer>();
                
                //
                // Get list of selected tubes to route.
                //
                //tubeContainers = getTubeContainers(); // only needs to be called once?
                
                //
                // Get the scene faces in structure list
                //
                //getSceneFaces();
                
                // Get inital fitnes value before any changes.
                //fitnessValueBest = fitnessValue(tubeContainers);
                //origionalFitnessValueLabel.setText(""+ roundThree(fitnessValueBest));// origional fitness
                //double fitnessValuePrev = fitnessValueBest;
                //globalFitnessValue = fitnessValueBest;
                //globalBestFitnessValue = fitnessValueBest;
                //fitnessValueLabel.setText("" + roundThree(globalFitnessValue) );
                //bestFitnessValueLabel.setText( "" + roundThree(globalBestFitnessValue) );
                 
                //System.out.println("  fitnessValue: " + fitnessValueBest);
                
                int acceptedCount = 0;
                int rejectedCount = 0;
                int i = 0;
                while (running) {
                    
                    
                    update(1); // Update Soft Body Simulation
                    
                    
                    
                    //double debugFitnes = fitnessValue(tubeContainers); // keep track of fitness before mutation.
                    
                    // Modify Random point.
                    // Choose random tube.
                    //mutate(tubeContainers);
                    
                    // Regenerate tube data
                    //tubeContainers = getTubeContainers(); // tubes have been changed.
                    
                    double fitnessValueNew = 0; // fitnessValue(tubeContainers);
                    //globalFitnessValue = fitnessValueNew;
                    //fitnessValueLabel.setText("" + roundOne(globalFitnessValue)); // Update Label
                    //collisionsLabel.setText("" + roundOne(globalCollisions));
                    //equalityLabel.setText("" + roundThree(globalEqual));
                    //minSpanErrorLabel.setText("" + roundOne(minSpanErrors) );
                    //sumLengthLabel.setText("" + roundThree(globalSumLength) );
                    
                    //double parallelValue = (double)parallelAngleSum * (double)100;
                    //System.out.println(" parallelValue " +parallelValue );
                    //parallelAlignmentLabel.setText("" + roundThree( parallelValue ) );
                    
                    
                    long currTime = System.currentTimeMillis();
                    long timeElapsed = (currTime - start) / 1000;
                    
                    int hours = (int) timeElapsed / 3600;
                        int remainder = (int) timeElapsed - hours * 3600;
                        int mins = remainder / 60;
                        remainder = remainder - mins * 60;
                        int secs = remainder;
                    
                    timeValueLabel.setText("" + hours + ":" +mins + ":" +secs   );
                    
                    //System.out.println("  fitnessValue NEW: " + fitnessValueNew + "  old: " + fitnessValueBest );
                    //if(minSpanErrors > 0){ // OH OH
                    //    System.out.println("  fitnessValue NEW: " + fitnessValueNew + "  old: " + fitnessValueBest  + "   ****** " );
                    //}
                    
                    //if(fitnessValueNew < fitnessValueBest){
                        //if(minSpanErrors > 0){ // OH OH
                        //    System.out.println(i + " Accept Change. " );
                        //    Sound.playSound("success.wav");
                        //}
                            
                        //acceptTubeChanges(tubeContainers);
                        //revertTubeChanges(tubeContainers); // debug
                        
                        //fitnessValueBest = fitnessValueNew; // old assign to new
                        //acceptedCount++;
                        
                        //globalBestFitnessValue = fitnessValueNew;
                        //bestFitnessValueLabel.setText( "" + roundOne(globalBestFitnessValue) );
                        
                    //} else {    // old one was better, revert.
                        //if(minSpanErrors > 0){ // OH OH
                        //    System.out.println(i + " Reject Change. Reverting. " );
                        //    Sound.playSound("deny.wav");
                        //}
                        
                        //revertTubeChanges(tubeContainers);
                        
                        //System.out
                        //fitnessValueNew = fitnessValue(tubeContainers); // recalculate fitnessvalue from old points
                        //tubeContainers = getTubeContainers(); // tubes have been changed.
                        
                        //if(minSpanErrors > 0){ // OH OH
                        //    System.out.println("    ---  recalculated  fitnessValueNew " + fitnessValueNew + " debugFitnes: " + debugFitnes); // !!! should be the same
                        //}
                        //rejectedCount++;
                    //}
                    
                    
                    //acceptedChangesLabel.setText("" + roundOne(acceptedCount));
                    //rejectedChangesLabel.setText("" + roundOne(rejectedCount));
                    
                    //tubeContainers = getTubeContainers(); // tubes have been changed. (erases prev point data)
                    
                    // Draw progress indicator
                    Graphics2D g2d = (Graphics2D)dialog.getGraphics();
                    if(g2d!=null){
                     
                        //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        //if(i % 750 == 0){
                            g2d.setColor(Color.WHITE);
                            //g2d.fillRect(0, dim.height - 400, 300, 300);
                            
                            g2d.setColor(Color.LIGHT_GRAY);
                            g2d.setStroke(new BasicStroke(1));
                            g2d.drawOval(150 - 100, dim.height - 325, 200, 200);    // outline
                            
                            
                            // CLEAR draw white space (rather than clear rect.)
                            g2d.setColor(Color.WHITE);
                            g2d.setStroke(new BasicStroke(5));
                            g2d.draw(new Arc2D.Double(55, dim.height - 325 + 5,
                                                     190,
                                                     190,
                                                      (progressStart + progressEnd + 2),
                                                      (360 - progressEnd  - 1 ) ,
                                                     Arc2D.OPEN));
                            
                            /*
                            // CLEAR draw white space (rather than clear rect.)
                            g2d.setColor(Color.WHITE);
                            g2d.setStroke(new BasicStroke(6));
                            g2d.draw(new Arc2D.Double(70, dim.height - 325 + 20,
                                                     160,
                                                     160,
                                                      (progressStart + progressEnd + 2),
                                                      (360 - progressEnd  - 1 ) ,
                                                     Arc2D.OPEN));
                            */
                            
                            // Draw progress arc
                            g2d.setColor(Color.GRAY);
                            g2d.setStroke(new BasicStroke(4));
                            g2d.draw(new Arc2D.Double(55, dim.height - 325 + 5,
                                                     190,
                                                     190,
                                                    progressStart,
                                                    progressEnd,
                                                     Arc2D.OPEN));
                            
                            
                            
                            progressStart--;
                            if(progressCycle == false){
                                progressStart--;
                            }
                            if(progressStart > 360){ progressStart = 0; }
                            if(progressStart < 0){ progressStart = 360; }
                            if(progressEnd > 360){
                                progressEnd--;
                                progressCycle = !progressCycle;
                            }
                            else if(progressEnd < 0){
                                progressEnd++;
                                progressCycle = !progressCycle;
                            }
                            if(progressCycle){
                                progressEnd--;
                            } else {
                                progressEnd++;
                            }
                            
                            //System.out.println(".");
                            //System.out.println("progressStart: " + progressStart + " progressEnd: " + progressEnd + " progressCycle: " + progressCycle);
                            
                            //if(tubeContainers.size() == 0){
                            //    try{Thread.sleep(25); } catch (Exception e){}
                            //}
                        
                            //dialog.repaint();
                            
                        //}
                    }
                    
                    // Show change
                    //window.updateImage();
                    
                    //try{Thread.sleep(5000); } catch (Exception e){} // debug
                    try {
                        Thread.sleep(50);
                    } catch (Exception e){
                    }
                    
                    
                    i++;
                } // loop
                
            } // thread run
        }; // thread
        thread.start();
    }
    
    
    String roundOne(double x){
        //double rounded = ((double)Math.round(x * 100000) / 100000);
        
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        return df.format(x);
    }
    
    
    String roundThree(double x){
        //double rounded = ((double)Math.round(x * 100000) / 100000);
        
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(3);

        return df.format(x);
    }
    
}
