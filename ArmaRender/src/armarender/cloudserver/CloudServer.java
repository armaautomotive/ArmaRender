/* Copyright (C) 2023 by Jon Taylor
 
 Hosts a WebGPU client interface to ADS.
 
 Debug:
 Error: Unable to initialize main class armarender.ArmaDesignStudio
 Caused by: java.lang.NoClassDefFoundError: buoy/xml/IconResource
 
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

public class CloudServer {
    int count = 0;
    private LayoutWindow window = null;
    private Scene scene = null;
    
    // setRequestHeader("Access-Control-Allow-Origin","*")
    // httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    
    /**
     *
     */
    public CloudServer(){
        System.out.println("CloudServer loading...");
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/", new MyHandler()); // "/test"
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (Exception e){
            
        }
    }
    
    public void setLayoutWindow(LayoutWindow w){
        this.window = w;
        
        this.scene = w.getScene();
    }

    public void setScene(Scene s){
        this.scene = s;
    }
    
    
    /**
     * MyHandler
     *
     * Description:
     */
     class MyHandler implements HttpHandler {
            @Override
            public void handle(HttpExchange t) throws IOException {
                String response = "";
                //Map<String,List<String>> headers = t.getRequestHeaders();
                URI uri = t.getRequestURI();
                
                System.out.println("uri: " + uri);
                
                Vector<String> tokens = new Vector<String>();
                StringTokenizer tokenizer = new StringTokenizer(uri.toString(), "/");
                for (int i = 1; tokenizer.hasMoreTokens(); i++){
                    String token = tokenizer.nextToken();
                    tokens.addElement(token);
                    //System.out.println("Token "+i+":  "+tokenizer.nextToken());
                    //response += " <request>" + token + "</request>\n";
                }
                
                System.out.println("uri tokens.size() : " + tokens.size() );
                
                if(tokens.size() == 0){ // Index Page
                    response = getFilePage("WebClient/WebGL.html");
                    
                } else if(tokens.size() > 0 && tokens.elementAt(0).indexOf("jquery-3.7.0.js") != -1){
                    response = getFilePage("WebClient/jquery-3.7.0.js");
                    
                    
                } else if(tokens.size() > 0 && tokens.elementAt(0).indexOf("xxx") != -1){
                    
                    
                    
                    
                } else { // List
                    response = getSceneObjects();
                    
                    t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    //t.getResponseHeaders().add("Content-type", "application/json");
                    //t.getResponseHeaders().add("Content-length", Integer.toString(response.length()));
                    
                }
                
                count++;
                
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
     }
    
    
    /**
     * getFilePage
     *
     * Description:
     */
    public String getFilePage(String fileName){
        System.out.println("Get File: " + fileName);
        String response = "";
        try {
            //String fileName = "WebClient/WebGL.html";
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            try (InputStream is = classLoader.getResourceAsStream(fileName)) {
                if(is != null){
                    try (InputStreamReader isr = new InputStreamReader(is);
                         BufferedReader reader = new BufferedReader(isr)) {
                        for (String line; (line = reader.readLine()) != null;) {
                            response += line + "\n";
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e){
            System.out.println("Error: "  + e);
            e.printStackTrace();
        }
        return response;
    }
    
    
    /**
     *
     */
    public String getSceneObjects(){
        if(window == null){
            return "Error: Scene is null.";
        }
        
        String response =
        //"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n" +
        "{\n"; // Start
        
        // Get object list
        if(window != null){
            scene = window.getScene();
        }
        Vector<ObjectInfo> objects = scene.getObjects();
        System.out.println("List Object count: " + objects.size());
        
        response += " \"objects\": [\n";
        
        for(int i = 0; i < objects.size(); i++){
            ObjectInfo currObject = objects.elementAt(i);
            //System.out.println("name: " +currObject.getName() );
            
            response += "  {\n"; // object start
            response += "   \"id\": " + currObject.getId() + ", \n";
            
            String objectName = currObject.getName();
            //objectName = objectName.replaceAll("\"", Matcher.quoteReplacement("\""));
            objectName = objectName.replaceAll("\"", "0x22"); //  \\\"
            
            response += "   \"name\": \"" + objectName + "\",\n";
            response += "   \"locked\": " + currObject.isLocked() + ", \n";
            response += "   \"visible\": " + currObject.isVisible() + ", \n";
            response += "   \"toggle_hidden\": " + currObject.isChildrenHiddenWhenHidden() + ", \n";
            
            // Coords
            // Layout Coords
            
            ObjectInfo parent = currObject.getParent();
            if(parent != null){
                response += "   \"parent\": " + parent.getId() + ", \n";
            }
            
            response += "   \"type\": \"" + currObject.getObject().getClass().getName() + "\" \n";
            
            if(currObject.getObject() instanceof Curve){
                MeshVertex[] verts = ((Curve)currObject.getObject()).getVertices();
                response += "   ,\"vec\": [\n";
                for(int v = 0; v < verts.length; v++){
                    Vec3 vec = verts[v].r;
                    response += "    {\n";
                    response += "     \"x\": " + vec.x + ",\n"+
                                "     \"y\": " + vec.y + ",\n"+
                                "     \"z\": " + vec.z + "\n";
                    response += "    }\n";
                    if(v < verts.length - 1){
                        response += "    ,\n";
                    }
                }
                response += "   ]\n";
            }
            response += "  }\n"; // end object
            if(i < objects.size() - 1){
                response += "  ,\n";
            }
        }
        response += " ]\n"; // end object list
        response += "}\n"; // end file
        return response;
    }
    
    
    // Recent Files
    
    
    // List files.
    
    
    // List directories
    
    
    

}
