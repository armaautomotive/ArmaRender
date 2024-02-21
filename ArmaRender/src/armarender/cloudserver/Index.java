/* Copyright (C) 2023 by Jon Taylor
 
 Hosts a WebGPU client interface to ADS.
*/

package armarender.cloudserver;

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
import java.util.Vector;
import armarender.object.Cylinder;
import armarender.object.TriangleMesh.*;
import armarender.object.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.util.StringTokenizer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

public class Index {
    int count = 0;
    private LayoutWindow window = null;
    private Scene scene = null;
    
    // setRequestHeader("Access-Control-Allow-Origin","*")
    // httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    
    /**
     *
     */
    public Index(){
        
    }
    
    public void setLayoutWindow(LayoutWindow w){
        this.window = w;
        this.scene = w.getScene();
    }

    public void setScene(Scene s){
        this.scene = s;
    }
    
    
    
    
    
    
    
    
    
    

}
