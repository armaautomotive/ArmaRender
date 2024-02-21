/* Copyright (C) 2023 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.fea;

import armarender.*;
import armarender.object.*;
import armarender.math.*;
import armarender.view.CanvasDrawer;
import java.util.*;
import javax.swing.JOptionPane;

public class Cloth {
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
    
    Vector<Point> points = new Vector<Point>();
    public ClothThread clothThread = null;
    
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
    
    /**
     * Cloth
     *
     */
    public Cloth(LayoutWindow window){
        this.window = window;
        init();
        running = true;
        clothThread = new ClothThread();
        clothThread.start();
    }
    
    public void init(){
        // Get selected SplineMesh
        
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
                if(currInfo.getId() != splineMeshInfo.getId()){         // dont check against self
                    BoundingBox bounds = currInfo.getTranslatedBounds();
                    Vec3 currCenter = bounds.getCenter();
                    Vec3 translatedP = new Vec3(p);
                    CoordinateSystem c;
                    c = layout.getCoords(splineMeshInfo);
                    Mat4 mat4 = c.duplicate().fromLocal();
                    mat4.transform(translatedP);
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
        if(splineMesh == null){
            return;
        }
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
        
        // Update mesh points
        splineMesh.setVertexPositions(updatedMeshPoints);
        splineMeshInfo.clearCachedMeshes();
        
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
}
