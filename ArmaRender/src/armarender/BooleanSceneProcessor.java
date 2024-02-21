/* Copyright (C) 2022-2023 by Jon Taylor
   
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.*;
import armarender.animation.*;
import armarender.object.*;
import armarender.math.*;
import javax.swing.*;
import java.util.*;

/** This class contains functions for generating boolean geometry. */

public class BooleanSceneProcessor {
    LayoutWindow window;
    ThreadedProcessor thread = null;
    private Vector<ThreadedProcessor> threadPool = new Vector<ThreadedProcessor>();
    
    public BooleanSceneProcessor(LayoutWindow window){
        this.window = window;
    }
    
    // Need a thread to manage process boolean operations such that they can be aborted on new user input
    // and render progress outlines in the scene viewer.
    // If process() is called while allready running, cancel existing thread operation, and start a new.
    class ThreadedProcessor extends Thread {
        Boolean running = false;
        ObjectInfo info = null;
        
        public ThreadedProcessor(ObjectInfo info){
            this.info = info;
        }
        
        public void run(){
            running = Boolean.TRUE;
            System.out.println("\nTHREAD START");
            
            process(info, info, null, window, running);
            
            
            if(running.equals(Boolean.TRUE)){   // only redraw scene if it completed without being interrupted.
                // redraw scene.
                window.updateImage();
                window.updateMenus();
                window.rebuildItemList();
            }
            
            running = Boolean.FALSE;
            System.out.println("\nTHREAD END");
        }
        
        public void abort(){
            this.running = Boolean.FALSE;
        }
        // thread.interrupt();
    }
    
    /**
     * processObjectInThread
     *
     * Description:
     */
    public void processObjectInThread(ObjectInfo info){
        //process(obj, obj, null, window);
        
        //
        // remove completed threads from the pool.
        //
        for(int i = threadPool.size() - 1; i >= 0; i--){
            ThreadedProcessor thread = threadPool.elementAt(i);
            if(thread.isAlive() == false){
                threadPool.removeElementAt(i);
                System.out.println("\n REMOVING THREAD *** ");
            }
        }
        
        //
        // Stop existing thread.
        //
        if(thread != null){
            if(thread.isAlive()){
                System.out.println("\n EXISTING THREAD IS ALIVE, STOPPING is *******");
                thread.abort();
            }
            //thread.interrupt();
            //thread = null;
            //System.out.println("THREAD TERMINATED");
        }
        
        //
        // Start a new thread
        //
        ThreadedProcessor currThread = new ThreadedProcessor(info);
        threadPool.addElement(currThread);
        currThread.start();
        thread = currThread;
        System.out.println("\n THREADS " + threadPool.size() + " *" );
    }
    
    
    
    public void clearObject(LayoutWindow window, ObjectInfo info){
        System.out.println("BooleanSceneProcessor.clearObject() ");
    }
    
    
    /**
     *
     */
    public void processObjectTree(LayoutWindow window, ObjectInfo info){
        ObjectInfo upMost = info;
        while(upMost.getParent() != null){
            upMost = upMost.getParent();
        }
    }
    
    public void clearObjectTree(LayoutWindow window, ObjectInfo info){
        ObjectInfo upMost = info;
        while(upMost.getParent() != null){
            upMost = upMost.getParent();
        }
        
    }
    
    
    
    /**
     * go
     *
     * Decription: regenerate entire scene.
     */
    public void go(LayoutWindow window){
        System.out.println("BooleanSceneProcessor.go()  ");
        Scene theScene = window.getScene();
        
        //clear(window); // no longer nessisary as each object3D is now replaced when processed.
        
        //int selection [] = theScene.getSelection();
        //if(selection.length == 2){
        //    ObjectInfo a = theScene.getObject(selection[0]);
        Vector<ObjectInfo> sceneObjects = theScene.getObjects();
        //for (ObjectInfo obj : theScene.getObjects()){
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo obj = sceneObjects.elementAt(i);
            
            Object co = (Object)obj.getObject();
            //System.out.println(" Obj " + obj.getName() + " - " + co.getClass().getName() );
            
            // If object has no parents
            if(obj.getParent() == null ){ // && ((Object3D)co).canConvertToTriangleMesh() != Object3D.CANT_CONVERT
                //System.out.println("    parent: " + obj.getName() );
                process(obj, obj, null, window, true);
            }
        }
        
        // redraw scene.
        window.updateImage();
        window.updateMenus();
        window.rebuildItemList();
    }
    
    /**
     * process()
     *
     * Description: Process recursive hiarchy of objects and boolean folders to generate new geometry.
     *
     * TODO: Delete old boolean objects after creating new ones.
     *
     * TODO: Draw yellow object highlight while processing.
     *
     * @param: ObjectInfo:
     * @param: ObjectInfo:
     */
    public void process(ObjectInfo obj, ObjectInfo resultTargetObj, ObjectInfo boolObj, LayoutWindow window, Boolean running){
        Scene scene = window.getScene();
        ObjectInfo[] children = obj.getChildren();
        for(int i = 0; i < children.length && running.equals(Boolean.TRUE); i++){
            ObjectInfo child = children[i];
            Object3D child3D = child.getObject();
            
            if(child3D instanceof Folder){
                Folder folder = (Folder)child3D;
                
                ObjectInfo[] subChildren = child.getChildren();
                for(int j = 0; j < subChildren.length && running.equals(Boolean.TRUE); j++){ // Iterate child objects used to intersect/union/join with root object.
                    ObjectInfo subChild = subChildren[j];
                    Object3D subChild3D = subChild.getObject();
                    
                    if(subChild3D.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                        //System.out.println("     - child " + child.getName() );
                        //System.out.println("     - subChild " + subChild.getName() );
                        //System.out.println("     - action " + folder.getAction() );
                        
                        // Boolean
                        if(folder.getAction() == Folder.UNION){
                            if(boolObj == null){
                                CSGObject newobj = new CSGObject(obj, subChild, CSGObject.UNION);
                                TriangleMesh tm3D = newobj.convertToTriangleMesh(0.01);
                                Vec3 center = newobj.centerObjects();
                                center = new Vec3(0,0,0);
                                
                                // Does this object exist?
                                boolObj = getExistingBoolObject( resultTargetObj );
                                if(boolObj == null){
                                    boolObj = new ObjectInfo(tm3D, new CoordinateSystem(center, Vec3.vz(), Vec3.vy()), "Boolean " + obj.getName());
                                    boolObj.setGroupName("ads_auto_generated");
                                    boolObj.addTrack(new PositionTrack(boolObj), 0);
                                    boolObj.addTrack(new RotationTrack(boolObj), 1);
                                    // set parent to resultTargetObj
                                    boolObj.setParent(resultTargetObj);
                                    resultTargetObj.addChild(boolObj, 0);
                                    window.addObject(boolObj, null);
                                    window.rebuildItemList();
                                } else {
                                    tm3D.copyTextureAndMaterial(subChild3D);
                                    boolObj.setCoords( new CoordinateSystem(center, Vec3.vz(), Vec3.vy()) );
                                    boolObj.clearCachedMeshes();
                                    boolObj.setObject(tm3D);
                                }
                                    
                                //window.setSelection(scene.getNumObjects()-1);
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(scene.getNumObjects()-1)}));
                                //window.updateImage();
                                //window.updateMenus();
                                //window.rebuildItemList();
                            } else {
                                CSGObject newobj = new CSGObject(boolObj, subChild, CSGObject.UNION);
                                Vec3 center = newobj.centerObjects();
                                TriangleMesh tm3D = newobj.convertToTriangleMesh(0.01);
                                tm3D.copyTextureAndMaterial(subChild3D);
                                boolObj.setObject(tm3D.duplicate());
                                boolObj.setCoords( new CoordinateSystem(center, Vec3.vz(), Vec3.vy()) );
                                boolObj.clearCachedMeshes();
                            }
                            
                            // Set input objects to transparent.
                            obj.setDisplayModeOverride( ViewerCanvas.RENDER_TRANSPARENT + 1);
                            subChild.setDisplayModeOverride( ViewerCanvas.RENDER_TRANSPARENT + 1);
                            obj.clearCachedMeshes();
                            subChild.clearCachedMeshes();
                        }
                        if(folder.getAction() == Folder.SUBTRACT){
                            if(boolObj == null){
                                CSGObject newobj = new CSGObject(obj, subChild, CSGObject.DIFFERENCE12);
                                TriangleMesh tm3D = newobj.convertToTriangleMesh(0.01);
                                Vec3 center = newobj.centerObjects();
                                center = new Vec3(0,0,0);
                                
                                // Does this object exist?
                                boolObj = getExistingBoolObject( resultTargetObj );
                                if(boolObj == null){
                                    boolObj = new ObjectInfo(tm3D, new CoordinateSystem(center, Vec3.vz(), Vec3.vy()), "Boolean " + obj.getName());
                                    boolObj.setGroupName("ads_auto_generated");
                                    boolObj.addTrack(new PositionTrack(boolObj), 0);
                                    boolObj.addTrack(new RotationTrack(boolObj), 1);
                                    boolObj.setParent(resultTargetObj);
                                    resultTargetObj.addChild(boolObj, 0);
                                    window.addObject(boolObj, null);
                                    //window.setSelection(scene.getNumObjects()-1);
                                    //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(scene.getNumObjects()-1)}));
                                    window.rebuildItemList();
                                } else {
                                    tm3D.copyTextureAndMaterial(subChild3D);
                                    boolObj.setCoords( new CoordinateSystem(center, Vec3.vz(), Vec3.vy()) );
                                    boolObj.clearCachedMeshes();
                                    boolObj.setObject(tm3D);
                                }
                                
                                //window.updateImage();
                                //window.updateMenus();
                                //window.rebuildItemList();
                            } else {
                                CSGObject newobj = new CSGObject(boolObj.duplicate(), subChild.duplicate(), CSGObject.DIFFERENCE12);
                                Vec3 center = newobj.centerObjects();
                                TriangleMesh tm3D = newobj.convertToTriangleMesh(0.01);
                                tm3D.copyTextureAndMaterial(subChild3D);
                                boolObj.setObject(tm3D.duplicate()); //
                                boolObj.setCoords( new CoordinateSystem(center, Vec3.vz(), Vec3.vy() )  );
                                boolObj.clearCachedMeshes();
                            }
                            
                            // Set input objects to transparent.
                            obj.setDisplayModeOverride( ViewerCanvas.RENDER_TRANSPARENT + 1);
                            subChild.setDisplayModeOverride( ViewerCanvas.RENDER_TRANSPARENT + 1);
                            obj.clearCachedMeshes();
                            subChild.clearCachedMeshes();
                        }
                        if(folder.getAction() == Folder.INTERSECTION){
                            if(boolObj == null){
                                CSGObject newobj = new CSGObject(obj, subChild, CSGObject.INTERSECTION);
                                TriangleMesh tm3D = newobj.convertToTriangleMesh(0.01);
                                Vec3 center = newobj.centerObjects();
                                center = new Vec3(0,0,0);
                                
                                // Does this object exist?
                                boolObj = getExistingBoolObject( resultTargetObj );
                                if(boolObj == null){
                                    boolObj = new ObjectInfo(tm3D, new CoordinateSystem(center, Vec3.vz(), Vec3.vy()), "Boolean " + obj.getName());
                                    boolObj.setGroupName("ads_auto_generated");
                                    //boolObj = new ObjectInfo(om3D, new CoordinateSystem(), "Boolean");
                                    boolObj.addTrack(new PositionTrack(boolObj), 0);
                                    boolObj.addTrack(new RotationTrack(boolObj), 1);
                                    boolObj.setParent(resultTargetObj);
                                    resultTargetObj.addChild(boolObj, 0);
                                    window.addObject(boolObj, null);
                                    window.rebuildItemList();
                                } else {
                                    tm3D.copyTextureAndMaterial(subChild3D);
                                    boolObj.setCoords( new CoordinateSystem(center, Vec3.vz(), Vec3.vy() )  );
                                    boolObj.clearCachedMeshes();
                                    boolObj.setObject(tm3D);
                                }
                                    
                                //window.setSelection(scene.getNumObjects()-1);
                                //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(scene.getNumObjects()-1)}));
                                //window.updateImage();
                                //window.updateMenus();
                                //window.rebuildItemList();
                            } else {
                                CSGObject newobj = new CSGObject(boolObj.duplicate(), subChild.duplicate(), CSGObject.INTERSECTION); // DIFFERENCE21
                                Vec3 center = newobj.centerObjects();
                                TriangleMesh tm3D = newobj.convertToTriangleMesh(0.01);
                                tm3D.copyTextureAndMaterial(subChild3D);
                                boolObj.setObject(tm3D.duplicate());
                                boolObj.setCoords(new CoordinateSystem(center, Vec3.vz(), Vec3.vy()));
                            }
                            
                            // Set input objects to transparent.
                            obj.setDisplayModeOverride( ViewerCanvas.RENDER_TRANSPARENT + 1);
                            subChild.setDisplayModeOverride( ViewerCanvas.RENDER_TRANSPARENT + 1);
                            obj.clearCachedMeshes();
                            subChild.clearCachedMeshes();
                        } // End Action Type Intersection
                        
                        if(folder.getAction() == Folder.FOLDER){
                            // Recursivly descend
                            
                        }
                        
                    } // subchild can convert to triangle mesh
                    
                } // end sub children
                
                if(folder.getAction() == Folder.FOLDER){
                    // Recursivly descend
                    
                    //System.out.println(" *** FOLDER *** ");
                    //process(child, obj, boolObj, window);
                }
                //System.out.println(" ---FOLDER :" + folder.getAction());
                
                
            } else { // child is folder
                //System.out.println(" NOT FOLDER :"  );
            }
            
            // If folder and action == 0, then recursivly call with this obj as ref.
            //if( child3D instanceof Folder  && folder.getAction() == Folder.FOLDER ){
                
            //}
            if(child3D.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                //System.out.println(" *** BLA *** " + child.getName() );
                
                if(running.equals(Boolean.TRUE)){
                    process(child, obj, boolObj, window, running);
                }
            }
            
        } // end children loop
        
    }
    
    
    /**
     * getExistingBoolObject
     *
     * Description: get an existing boolean object so that it can be modified rather than adding duplicates.
     */
    public ObjectInfo getExistingBoolObject(ObjectInfo parent){
        ObjectInfo existing = null;
        ObjectInfo[] children = parent.getChildren();
        for(int i = 0; i < children.length; i++){
            ObjectInfo child = children[i];
            if(child.getGroupName().equals("ads_auto_generated")){
                existing = child;
                return existing;
            }
        }
        return existing;
    }
    
    
    /**
     * clear
     *
     * Description: Remove all boolean generated objects.
     */
    public void clear(LayoutWindow window){
        Scene theScene = window.getScene();
        Vector<ObjectInfo> sceneObjects = theScene.getObjects();
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo obj = sceneObjects.elementAt(i);
            //Object co = (Object)obj.getObject();
            //System.out.println(" Obj " + obj.getName() + " - " + co.getClass().getName() );
            if(obj.getGroupName().equals("ads_auto_generated") ){
                theScene.removeObjectInfo(obj);
            }
        }
    }
    
}


