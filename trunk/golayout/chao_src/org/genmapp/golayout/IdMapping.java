/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.genmapp.golayout;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.data.CyAttributes;
import cytoscape.layout.AbstractLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Chao
 */
public class IdMapping extends AbstractLayout {

    private static CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();

    public IdMapping() {
        super();
    }

    @Override
    public void construct() {
        System.out.println("Call idmapping success!");
        //CyDataset d
        connectFileDB();
        getSecKeyType();
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        System.out.println("Current Network Size: "+currentNetwork.nodesList().size());
        List<String> nodeIds = new ArrayList<String>();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            nodeIds.add(cn.getIdentifier());
        }
        mapAnnotation("canonicalName", "GOslim-BiologicalProcess");
        mapAnnotation("canonicalName", "GOslim-CellularComponent");
        mapAnnotation("canonicalName", "GOslim-MolecularFunction");
        //CyCommandResult result = mapIdentifiers(nodeIds, "Ensembl", "GOslim-BiologicalProcess");
        /**
        Map<String, Set<String>> secondaryKeyMap = new HashMap<String, Set<String>>();
        if (null != result) {
                Map<String, Set<String>> keyMappings = (Map<String, Set<String>>) result
                                .getResult();
                for (String primaryKey : keyMappings.keySet()) {
                    secondaryKeyMap.put(primaryKey, keyMappings.get(primaryKey));
                }
        }

         *
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
                String dnKey = cn.getIdentifier();
                Set<String> secKeys = secondaryKeyMap.get(dnKey);
                List<String> secKeyList = new ArrayList<String>();
                for (String sk : secKeys) {
                        secKeyList.add(sk);
                }


         * 
                nodeAttrs.setListAttribute(dnKey, "__" + "GOslim-BiologicalProcess", secKeyList);
                List<String> datasetlist;
                datasetlist = nodeAttrs.getListAttribute(dnKey,"");
                if (null == datasetlist)
                        datasetlist = new ArrayList<String>();
                datasetlist.add(datasetName);
                nodeAttrs.setListAttribute(dnKey, NET_ATTR_DATASETS,
                                datasetlist);
                        
       }
        /**/
    }

    @Override
    public String getName() {
        return "IdMapping";
    }

    @Override
    public String toString() {
        return "Test Idmapping";
    }

    /**
     *
     */
    public static String getSecKeyType() {
            String type = null;
            Map<String, Object> noargs = new HashMap<String, Object>();
            CyCommandResult result = null;
            try {
                    result = CyCommandManager.execute("idmapping",
                                    "get source id types", noargs);
            } catch (CyCommandException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            } catch (RuntimeException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
            if (null != result) {
                    Set<String> idTypes = (Set<String>) result.getResult();
                    for (String t : idTypes) {
                        System.out.println("Hits: "+t);
                    }
            }
            return type;
    }

    private static CyCommandResult mapAnnotation(String pkt, String skt) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("sourceattr", pkt);
		args.put("targettype", skt);
		CyCommandResult result = null;
		try {
			result = CyCommandManager.execute("idmapping", "attribute based mapping",
					args);
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String msg : result.getMessages()) {
			// System.out.println(msg);
		}
		return result;
	}

    private static CyCommandResult mapIdentifiers(List<String> l, String pkt,
			String skt) {
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("sourceid", l);
		args.put("sourcetype", pkt);
		args.put("targettype", skt);
		CyCommandResult result = null;
		try {
			result = CyCommandManager.execute("idmapping", "attribute based mapping",
					args);
		} catch (CyCommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String msg : result.getMessages()) {
			// System.out.println(msg);
		}
		return result;
	}
    /**
     *
     */
    public static void connectFileDB() {
        System.out.println("-----run connectFileDB-----");
        String type = null;
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.file.IDMapperText");
        args.put("connstring", "idmapper-text:dssep=	,transitivity=false@file:/C:/Users/Chao/Downloads/Sc_GOslim.tab");
        args.put("displayname", "fileGOslim");
        CyCommandResult result = null;
        try {
            result = CyCommandManager.execute("idmapping",
                                "register resource", args);
            List<String> results = result.getMessages();
            if (results.size() > 0) {
                    for (String re : results) {
                            if (re.contains("Success")) {
                                System.out.println("Success");
                            } else {
                                System.out.println("no Success");
                            }
                    }
            } else {
                System.out.println("Failed to connect!");
            };
            //System.out.println(count);
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
