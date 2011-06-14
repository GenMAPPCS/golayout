/*******************************************************************************
 * Copyright 2010 Alexander Pico
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.genmapp.golayout;

import giny.model.Node;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPanel;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.layout.AbstractLayout;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.layout.TunableListener;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.CyNetworkView;

public class GOLayout extends CytoscapePlugin {

	protected static final String CC_ATTNAME = "annotation.GO CELLULAR_COMPONENT";
	protected static final String BP_ATTNAME = "annotation.GO BIOLOGICAL_PROCESS";
	protected static final String MF_ATTNAME = "annotation.GO MOLECULAR_FUNCTION";
	protected static final String CC_CODE = "Tc";
	protected static final String BP_CODE = "Tb";
	protected static final String MF_CODE = "Tm";

	//test
	
	/**
	 * The constructor registers our layout algorithm. The CyLayouts mechanism
	 * will worry about how to get it in the right menu, etc.
	 */
	public GOLayout() {

		CyLayouts.addLayout(new GOLayoutAlgorithm(), "GO Layout");
		CyLayouts.addLayout(new PartitionAlgorithm(), null);
		CyLayouts.addLayout(new CellAlgorithm(), null);
                CyLayouts.addLayout(new IdMapping(), "IdMapping");

		// JMenuItem item = new JMenuItem("Add GO-slim annotations");
		// JMenu layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
		// .getMenu("Layout");
		// item.addActionListener(new AddAnnotationCommandListener());
		//
		// layoutMenu.add(item);
	}

	public static void createVisualStyle(CyNetworkView view) {
		PartitionNetworkVisualStyleFactory.createVisualStyle(view);

	}

	public class GOLayoutAlgorithm extends AbstractLayout implements
			TunableListener {

		protected static final String LAYOUT_NAME = "0-golayout";
		private static final String HELP = "GOLayout Help";
		private LayoutProperties layoutProperties = null;

		public String annotationAtt = "canonicalName";
		public String annotationCode = null;
//		public String annotationSpecies = "Human";
		private List<String> dsValues = new ArrayList<String>();
		private List<String> speciesValues = new ArrayList<String>();
//		private IDMapper _mapper = null;
//		private Set<DataSource> dataSources = null;
                private Tunable gAttParTunable;
		private Tunable aSpeAnnTunable;
                private Tunable aAttAnnTunable;
                private Tunable dsTunable;
                private Tunable lPreTemTunable;
                private Tunable lLoaTemTunable;
                private Tunable pParLevTunable;

		/**
		 * Creates a new CellularLayoutAlgorithm object.
		 */
		public GOLayoutAlgorithm() {
                    super();

                    // hardcode species until web service support query of supported
                    // species
                    speciesValues.add("Human");
                    speciesValues.add("Mouse");
                    speciesValues.add("Zebra fish");
                    speciesValues.add("Fruit fly");
                    speciesValues.add("Worm");
                    speciesValues.add("Yeast");

                    dsValues.add("ensmble");
                    // dynamically populate list of datasource names
                    //populateDataSourceList();

                    layoutProperties = new LayoutProperties(getName());
                    //Panel of "Global Settings"
                    layoutProperties.add(new Tunable("global", "Global Settings",
                            Tunable.GROUP, new Integer(3)));
                    gAttParTunable = new Tunable("attributePartition",
                            "The attribute to use for partitioning",
                            Tunable.NODEATTRIBUTE, BP_ATTNAME,
                            getAttributeList(BP_ATTNAME), (Object) null, 0);
                    gAttParTunable.addTunableValueListener(this);
                    layoutProperties.add(gAttParTunable);
                    layoutProperties.add(new Tunable("attributeLayout",
                            "The attribute to use for the layout",
                            Tunable.NODEATTRIBUTE, CC_ATTNAME,
                            getAttributeList(CC_ATTNAME), (Object) null, 0));
                    layoutProperties.add(new Tunable("attributeNodeColor",
                            "The attribute to use for node color",
                            Tunable.NODEATTRIBUTE, MF_ATTNAME,
                            getAttributeList(MF_ATTNAME), (Object) null, 0));
                    //Panel of "Annotation Settings"
                    layoutProperties.add(new Tunable("annotation",
                            "Annotation Settings (optional)",
                            Tunable.GROUP, new Integer(3)));
                    aSpeAnnTunable = new Tunable("speciesAnnotation",
                            "Species of identifier", Tunable.LIST, new Integer(0),
                            (Object) speciesValues.toArray(), null, 0);
                    aSpeAnnTunable.addTunableValueListener(this);
                    layoutProperties.add(aSpeAnnTunable);
                    aAttAnnTunable = new Tunable("attributeAnnotation",
                            "The identifier to use for annotation retrieval",
                            Tunable.NODEATTRIBUTE, annotationAtt,
                            (Object) getInitialAttributeList(), (Object) null, 0);
                    layoutProperties.add(aAttAnnTunable);
                    dsTunable = new Tunable("dsAnnotation",
                            "Type of identifier, e.g., Entrez Gene", Tunable.LIST,
                            new Integer(0), (Object) dsValues.toArray(), null, 0);
                    layoutProperties.add(dsTunable);
                    //Panel of "Partition Settings"
                    layoutProperties.add(new Tunable("partition", "Partition Settings",
                            Tunable.GROUP, new Integer(3)));
                    layoutProperties.add(new Tunable("partitionMin",
                            "Don't show subnetworks with fewer nodes than",
                            Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MIN));
                    layoutProperties.add(new Tunable("partitionMax",
                            "Don't show subnetworks with more nodes than",
                            Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MAX));
                    pParLevTunable = new Tunable("partitionLevel",
                            "The deepest level of GO term for partition", Tunable.LIST,
                            new Integer(0), (Object) dsValues.toArray(), null, 0);
                    layoutProperties.add(pParLevTunable);
                    //Panel of "Layout Settings"
                    layoutProperties.add(new Tunable("layout", "Layout Settings",
                            Tunable.GROUP, new Integer(2)));
                    lPreTemTunable = new Tunable("presetTemplete",
                            "Choose on templete from",
                            Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MIN);
                    layoutProperties.add(lPreTemTunable);
                    lLoaTemTunable = new Tunable("uploadTemplete",
                            "Or upload your templete",
                            Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MAX);
                    layoutProperties.add(lLoaTemTunable);
                    if (GOLayoutUtil.checkGPMLPlugin() == false) {
                        lPreTemTunable.setImmutable(true);
                        lLoaTemTunable.setImmutable(true);
                    }
                    //Panel of "Floorplan Settings"
                    layoutProperties.add(new Tunable("floorplan", "Floorplan Settings",
                            Tunable.GROUP, new Integer(2)));
                    layoutProperties.add(new Tunable("nodeSpacing",
                            "Spacing between nodes", Tunable.DOUBLE,
                            CellAlgorithm.distanceBetweenNodes));
                    layoutProperties.add(new Tunable("pruneEdges",
                            "Prune cross-region edges?", Tunable.BOOLEAN, false));

                    /*
                     * We've now set all of our tunables, so we can read the property
                     * file now and adjust as appropriate
                     */
                    layoutProperties.initializeProperties();

                    /*
                     * Finally, update everything. We need to do this to update any of
                     * our values based on what we read from the property file
                     */
                    updateSettings(true);

                    // Add help menu item
                    JMenuItem getHelp = new JMenuItem(HELP);
                    getHelp.setToolTipText("Open online help for GOLayout");
                    GetHelpListener getHelpListener = new GetHelpListener();
                    getHelp.addActionListener(getHelpListener);
                    Cytoscape.getDesktop().getCyMenus().getHelpMenu().add(getHelp);

		}

                public List<String> getAttributeList(String goAttribute){
                    List tempList = getInitialAttributeList();
                    if(tempList.indexOf(goAttribute)==-1)
                        tempList.add(goAttribute);
                    return tempList;
                }

//		public void populateDataSourceList() {
//			try {
//				Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest");
//			} catch (ClassNotFoundException e) {
//				System.out
//						.println("Can't register org.bridgedb.rdb.IDMapperRdb");
//				e.printStackTrace();
//			}
//
//			BioDataSource.init();
//			// now we connect to the driver and create a IDMapper instance.
//			// TODO: Update to use multiple species
//			try {
//				_mapper = BridgeDb
//						.connect("idmapper-bridgerest:http://webservice.bridgedb.org/"
//								+ annotationSpecies);
//			} catch (IDMapperException e) {
//				e.printStackTrace();
//			}
//			try {
//				dataSources = _mapper.getCapabilities()
//						.getSupportedSrcDataSources();
//			} catch (IDMapperException e) {
//				e.printStackTrace();
//			}
//			dsValues.clear();
//			if (dataSources.size() > 0) {
//				Iterator it = dataSources.iterator();
//				while (it.hasNext()) {
//					dsValues.add(((DataSource) it.next()).getFullName());
//				}
//
//			}
//
//		}

		public void tunableChanged(Tunable t) {
			// TODO Auto-generated method stub
			if (t.getName().equals("speciesAnnotation")) {
				updateSettings();
//				populateDataSourceList(); // refresh list
//				dsTunable.setLowerBound(dsValues.toArray()); // and reset
																// tunable
			} else if(t.getName().equals("attributePartition")) {
                            if(t.getValue().equals(BP_ATTNAME)) {
                                dsTunable.setImmutable(false);
                                aSpeAnnTunable.setImmutable(false);
                                aAttAnnTunable.setImmutable(false);
                            } else {
                                dsTunable.setImmutable(true);
                                aSpeAnnTunable.setImmutable(true);
                                aAttAnnTunable.setImmutable(true);
                            }
                            if(!(t.getValue().equals(BP_ATTNAME)||t.getValue().equals(CC_ATTNAME)||t.getValue().equals(MF_ATTNAME)))
                                pParLevTunable.setImmutable(true);
                            else
                                pParLevTunable.setImmutable(false);
                            updateSettings();
                        }
		}

		/**
		 * External interface to update our settings
		 */
		public void updateSettings() {
			updateSettings(true);
		}

		/**
		 * Signals that we want to update our internal settings
		 * 
		 * @param force
		 *            force the settings to be updated, if true
		 */
		public void updateSettings(boolean force) {
			layoutProperties.updateValues();
			Tunable t = layoutProperties.get("attributePartition");
			if ((t != null) && (t.valueChanged() || force)) {
				String newValue = (String) t.getValue();
				if (newValue.equals("(none)")) {
					PartitionAlgorithm.attributeName = null;
				} else {
					PartitionAlgorithm.attributeName = newValue;
				}
			}

			t = layoutProperties.get("attributeLayout");
			if ((t != null) && (t.valueChanged() || force)) {
				String newValue = (String) t.getValue();
				if (newValue.equals("(none)")) {
					CellAlgorithm.attributeName = null;
				} else {
					CellAlgorithm.attributeName = newValue;
				}
			}

			t = layoutProperties.get("attributeNodeColor");
			if ((t != null) && (t.valueChanged() || force)) {
				String newValue = (String) t.getValue();
				if (newValue.equals("(none)")) {
					PartitionNetworkVisualStyleFactory.attributeName = null;
				} else {
					PartitionNetworkVisualStyleFactory.attributeName = newValue;
				}
			}

//			t = layoutProperties.get("attributeAnnotation");
//			if ((t != null) && (t.valueChanged() || force)) {
//				String newValue = (String) t.getValue();
//				annotationAtt = newValue;
//			}

//			t = layoutProperties.get("speciesAnnotation");
//			if ((t != null) && (t.valueChanged() || force)) {
//				String newValue = speciesValues.get((Integer) t.getValue());
//				annotationSpecies = newValue;
//			}
//
//			t = layoutProperties.get("dsAnnotation");
//			if ((t != null) && (t.valueChanged() || force)) {
//				String newValue = dsValues.get((Integer) t.getValue());
//				annotationCode = newValue;
//			}

			t = layoutProperties.get("partitionMin");
			if ((t != null) && (t.valueChanged() || force))
				PartitionAlgorithm.NETWORK_LIMIT_MIN = ((Integer) t.getValue())
						.intValue();

			t = layoutProperties.get("partitionMax");
			if ((t != null) && (t.valueChanged() || force))
				PartitionAlgorithm.NETWORK_LIMIT_MAX = ((Integer) t.getValue())
						.intValue();

			t = layoutProperties.get("nodeSpacing");
			if ((t != null) && (t.valueChanged() || force))
				CellAlgorithm.distanceBetweenNodes = ((Double) t.getValue())
						.doubleValue();

			t = layoutProperties.get("pruneEdges");
			if ((t != null) && (t.valueChanged() || force))
				CellAlgorithm.pruneEdges = ((Boolean) t.getValue())
						.booleanValue();

		}

		/**
		 * Reverts our settings back to the original.
		 */
		public void revertSettings() {
			layoutProperties.revertProperties();
		}

		public LayoutProperties getSettings() {
			return layoutProperties;
		}

		/**
		 * Returns the short-hand name of this algorithm NOTE: is related to the
		 * menu item order
		 * 
		 * @return short-hand name
		 */
		public String getName() {
			return LAYOUT_NAME;
		}

		/**
		 * Returns the user-visible name of this layout
		 * 
		 * @return user visible name
		 */
		public String toString() {
			return "GO Layout";
		}

		/**
		 * Gets the Task Title.
		 * 
		 * @return human readable task title.
		 */
		public String getTitle() {
			return new String("GO Layout");
		}

		/**
		 * Return true if we support performing our layout on a limited set of
		 * nodes
		 * 
		 * @return true if we support selected-only layout
		 */
		public boolean supportsSelectedOnly() {
			return false;
		}

		/**
		 * Returns the types of node attributes supported by this algorithm.
		 * 
		 * @return the list of supported attribute types, or null if node
		 *         attributes are not supported
		 */
		public byte[] supportsNodeAttributes() {
			return null;
		}

		/**
		 * Returns the types of edge attributes supported by this algorithm.
		 * 
		 * @return the list of supported attribute types, or null if edge
		 *         attributes are not supported
		 */
		public byte[] supportsEdgeAttributes() {
			return null;
		}

		/**
		 * Returns a JPanel to be used as part of the Settings dialog for this
		 * layout algorithm.
		 * 
		 */
		public JPanel getSettingsPanel() {
			JPanel panel = new JPanel(new GridLayout(0, 1));
			panel.add(layoutProperties.getTunablePanel());

			return panel;
		}

		/**
		 * 
		 * Add "(none)" to the list
		 * 
		 * @returns List of our "special" weights
		 */
		public List<String> getInitialAttributeList() {
			ArrayList<String> attList = new ArrayList<String>();
			attList.add("(none)");
			// also add special GOSlim attribute names to trigger
			// auto-annotation
//			if (null != attName) {
//				attList.add(attName);
//			}

			return attList;
		}

		/**
		 * The layout protocol...
		 */
		public void construct() {
			// Check to see if annotations are present
//			String[] attNames = Cytoscape.getNodeAttributes()
//					.getAttributeNames();
//			boolean ccPresent = false;
//			boolean bpPresent = false;
//			boolean mfPresent = false;
//			for (int i = 0; i < attNames.length; i++) {
//				if (attNames[i] == CC_ATTNAME) {
//					ccPresent = true;
//				} else if (attNames[i] == BP_ATTNAME) {
//					bpPresent = true;
//				} else if (attNames[i] == MF_ATTNAME) {
//					mfPresent = true;
//				}
//			}
			// Fetch annotations if they are needed
//			if (CC_ATTNAME == CellAlgorithm.attributeName && !ccPresent) {
//				setupBridgeDB(CC_CODE, CC_ATTNAME, annotationAtt,
//						annotationCode, annotationSpecies);
//			}
//			if (BP_ATTNAME == PartitionAlgorithm.attributeName && !bpPresent) {
//				setupBridgeDB(BP_CODE, BP_ATTNAME, annotationAtt,
//						annotationCode, annotationSpecies);
//			}
//			if (MF_ATTNAME == PartitionNetworkVisualStyleFactory.attributeName
//					&& !mfPresent) {
//				setupBridgeDB(MF_CODE, MF_ATTNAME, annotationAtt,
//						annotationCode, annotationSpecies);
//			}

			if (null != CellAlgorithm.attributeName) {
				PartitionAlgorithm.layoutName = CellAlgorithm.LAYOUT_NAME;
			}
			CyLayoutAlgorithm layout = CyLayouts.getLayout("partition");
			layout.doLayout(Cytoscape.getCurrentNetworkView(), taskMonitor);
		}

		/**
		 * This method connects to the BridgeDb web service and manages all the
		 * transactions through to setting node attributes.
		 * 
		 * @param code
		 * @param attname
		 * @param annAtt
		 * @param annCode
		 * @param annSpecies
		 */
//		public void setupBridgeDB(String code, String attname, String annAtt,
//				String annCode, String annSpecies) {
//
//			DataSource annDs = DataSource.getByFullName(annCode);
//
//			try {
//				Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest");
//			} catch (ClassNotFoundException e) {
//				System.out
//						.println("Can't register org.bridgedb.rdb.IDMapperRdb");
//				e.printStackTrace();
//			}
//
//			BioDataSource.init();
//			// now we connect to the driver and create a IDMapper instance.
//			// TODO: Update to use multiple species
//			try {
//				_mapper = BridgeDb
//						.connect("idmapper-bridgerest:http://webservice.bridgedb.org/"
//								+ annSpecies);
//			} catch (IDMapperException e) {
//				e.printStackTrace();
//			}
//			System.out.println("ID Mapper = " + _mapper);
//			System.out.println("ID Mapper capabilites = "
//					+ _mapper.getCapabilities());
//
//			CyAttributes attrs = Cytoscape.getNodeAttributes();
//			Xref inXref = null;
//
//			Iterator<Node> it = Cytoscape.getCurrentNetwork().nodesIterator();
//			while (it.hasNext()) {
//				Node n = it.next();
//				String inputID = (String) Cytoscape.getNodeAttributes()
//						.getAttribute(n.getIdentifier(), annAtt);
//				if (null == inputID) {
//					continue;
//				}
//				if ("Sym" == annCode && "Human" == annSpecies){
//					inputID = inputID.toUpperCase();
//				}
//
//				inXref = new Xref(inputID, annDs);
//
//				Set<Xref> outXrefs = null;
//				try {
//					outXrefs = _mapper.mapID(inXref, DataSource
//							.getBySystemCode(code));
//				} catch (IDMapperException e) {
//					System.out
//							.println("Got ID mapper exception trying to get mappings");
//				}
//				System.out.println("Got mappings: " + outXrefs);
//				if (outXrefs != null) {
//					List<String> att = new ArrayList<String>();
//					for (Xref xref : outXrefs) {
//						att.add(xref.getId());
//					}
//					Cytoscape.getNodeAttributes().setListAttribute(
//							n.getIdentifier(), attname, att);
//				}
//
//			}
//		}
//
	}
}

/**
 * This class direct a browser to the help manual web page.
 */
class GetHelpListener implements ActionListener {
	private String helpURL = "http://genmapp.org/GOLayout/GOLayout.html";

	public void actionPerformed(ActionEvent ae) {
		cytoscape.util.OpenBrowser.openURL(helpURL);
	}

}
