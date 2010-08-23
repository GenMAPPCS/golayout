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

import org.bridgedb.BridgeDb;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;

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

	protected static final String CC_ATTNAME = "annotation.GOSlim CELLULAR_COMPONENT";
	protected static final String BP_ATTNAME = "annotation.GOSlim BIOLOGICAL_PROCESS";
	protected static final String MF_ATTNAME = "annotation.GOSlim MOLECULAR_FUNCTION";
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
		public String annotationSpecies = "Human";
		private List<String> dsValues = new ArrayList<String>();
		private List<String> speciesValues = new ArrayList<String>();
		private IDMapper _mapper = null;
		private Set<DataSource> dataSources = null;
		private Tunable dsTunable;

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

			// dynamically populate list of datasource names
			populateDataSourceList();

			layoutProperties = new LayoutProperties(getName());
			layoutProperties.add(new Tunable("global", "Global Settings",
					Tunable.GROUP, new Integer(3)));
			layoutProperties.add(new Tunable("attributePartition",
					"The attribute to use for partitioning",
					Tunable.NODEATTRIBUTE, PartitionAlgorithm.attributeName,
					(Object) getInitialAttributeList(BP_ATTNAME),
					(Object) null, 0));
			layoutProperties.add(new Tunable("attributeLayout",
					"The attribute to use for the layout",
					Tunable.NODEATTRIBUTE, CellAlgorithm.attributeName,
					(Object) getInitialAttributeList(CC_ATTNAME),
					(Object) null, 0));
			layoutProperties.add(new Tunable("attributeNodeColor",
					"The attribute to use for node color",
					Tunable.NODEATTRIBUTE,
					PartitionNetworkVisualStyleFactory.attributeName,
					(Object) getInitialAttributeList(MF_ATTNAME),
					(Object) null, 0));
			layoutProperties.add(new Tunable("annotation",
					"Annotation Settings (optional)", Tunable.GROUP, new Integer(3)));
			layoutProperties.add(new Tunable("attributeAnnotation",
					"The identifier to use for annotation retrieval",
					Tunable.NODEATTRIBUTE, annotationAtt,
					(Object) getInitialAttributeList(null), (Object) null, 0));
			Tunable t = new Tunable("speciesAnnotation", "Species of identifier",
					Tunable.LIST, new Integer(0), (Object) speciesValues
							.toArray(), null, 0);
			t.addTunableValueListener(this);
			layoutProperties.add(t);
			dsTunable = new Tunable("dsAnnotation",
					"Type of identifier, e.g., Entrez Gene", Tunable.LIST,
					new Integer(0), (Object) dsValues.toArray(), null, 0);
			layoutProperties.add(dsTunable);
			layoutProperties.add(new Tunable("partition", "Partition Settings",
					Tunable.GROUP, new Integer(2)));
			layoutProperties.add(new Tunable("partitionMin",
					"Don't show subnetworks with fewer nodes than",
					Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MIN));
			layoutProperties.add(new Tunable("partitionMax",
					"Don't show subnetworks with more nodes than",
					Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MAX));
			layoutProperties.add(new Tunable("floorplan", "Floorplan Settings",
					Tunable.GROUP, new Integer(2)));
			layoutProperties.add(new Tunable("nodeSpacing",
					"Spacing between nodes", Tunable.DOUBLE,
					CellAlgorithm.distanceBetweenNodes));
			layoutProperties.add(new Tunable("pruneEdges",
					"Prune cross-region edges?", Tunable.BOOLEAN, false));

			
class GetHelpListener implements ActionListener {
	private String helpURL = "http://genmapp.org/GOLayout/GOLayout.html";

	public void actionPerformed(ActionEvent ae) {
		cytoscape.util.OpenBrowser.openURL(helpURL);
	}

}
