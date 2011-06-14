/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.genmapp.golayout;

import org.pathvisio.cytoscape.GpmlPlugin;

/**
 *
 * @author Chao
 */
public class GOLayoutUtil {


    public static boolean checkGPMLPlugin(){
        try {
            GpmlPlugin.getInstance();
            return true;
        } catch(NoClassDefFoundError e){
            return false;
        }
    }
}
