/* Copyright (C) 2022 Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.*;
import armarender.animation.*;
import armarender.animation.distortion.*;
import armarender.material.*;
import armarender.math.*;
import armarender.texture.*;
import armarender.object.*;
import java.lang.ref.*;
import java.util.*;
import javax.swing.*;

import javax.sound.sampled.*;
import java.io.*;

public class Sound {
    
    public Sound(){
        
    }
    
    /**
     * playSound
     *
     * Description: Play sound wav file in jar package.
     */
    public static synchronized void playSound(final String url) {
        Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            Clip clip = AudioSystem.getClip();
              InputStream in = getClass().getResourceAsStream("/armarender/sounds/" + url);
              InputStream bufferedIn = new BufferedInputStream(in);
            AudioInputStream inputStream = AudioSystem.getAudioInputStream( bufferedIn );
            clip.open(inputStream);
            clip.start();
          } catch (Exception e) {
            System.out.println("Error: ");
            System.err.println(e.getMessage());
          }
        }
      }); // .start();
        t.start();
        try {
            t.join();
        } catch (Exception e){
            
        }
    }
    
}
