
package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;


public class Examples {
    
    public Examples(){
        
    }
    
    /**
     * demo
     * Description: Example of common tasks.
     */
    public void demo(LayoutWindow window){
        Scene scene = window.getScene();
        
        
        // list objects in the scene.
        Vector<ObjectInfo> sceneObjects = scene.getObjects();
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo currInfo = sceneObjects.elementAt(i);
                System.out.println(" iterate scene objects: " + currInfo.getName() );
        }
        
        
        
        // Add a cube to the scene.
        
        
        
        
        
    }
    
}

