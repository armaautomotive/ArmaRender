


package armarender;

import armarender.object.*;
import armarender.*;
import armarender.texture.*;
import armarender.animation.*;
import armarender.math.*;
import armarender.ui.*;
import buoy.widget.*;
import java.io.*;
import armarender.view.*;
import armarender.texture.*;
import java.awt.Color;
import java.util.*;

public class VecUtils {
    
    public VecUtils(){
        
    }
    

    
    /**
     * getPerpendicular
     *
     * Description: Get perpendicular normal vector from two other vectors.
     */
    static Vec3 getPerpendicular(Vec3 a, Vec3 b){
        Vec3 result = new Vec3(0,0,0);
        
        
        
        result.normalize();
        return result;
    }
}
