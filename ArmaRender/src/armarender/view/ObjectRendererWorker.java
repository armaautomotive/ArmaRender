/**
 *
 * Doesn't work. No performance gain. And state is not threadable.
 * - Would need a version of renderObject for each ObjectInfo that accepts a parameter for Cemera with ObjectTransform
 *    This is because the Viewer Canvas camera can be used by many objects if threaded.
 */

package armarender.view;


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


public class ObjectRendererWorker implements Callable {
//public class ObjectRendererWorker implements Runnable {
    SceneViewer sceneViewer;
    
    Vector<ObjectInfo> renderObjects;
    
    public ObjectRendererWorker(SceneViewer sceneViewer, Vector<ObjectInfo> renderObjects){
        this.sceneViewer = sceneViewer;
        //renderObjects = new Vector<ObjectInfo>();
        this.renderObjects = renderObjects;
    }
    
    public void setRenderObjects(Vector<ObjectInfo> renderObjects){
        this.renderObjects.clear();
        this.renderObjects.addAll(renderObjects);
    }
    
    public void run(){
        if(renderObjects != null){
            Vec3 cameraLocation = sceneViewer.getCamera().getCameraCoordinates().getOrigin();
            Vec3 viewdir = sceneViewer.getCamera().getViewToWorld().timesDirection(Vec3.vz());
            
            //System.out.println(" run " + renderObjects.size());
            
            for(int i = 0; i < renderObjects.size(); i++){
                ObjectInfo obj = renderObjects.elementAt(i);
                
                //System.out.println(" obj " + obj);
                
                sceneViewer.getCamera().setObjectTransform(obj.getCoords().fromLocal());
                obj.getObject().renderObject(obj, sceneViewer, viewdir);
            }
        }
    }
    
    
    public Integer call(){
        run();
        return 0;
    }
}
