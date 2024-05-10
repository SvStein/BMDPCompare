import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;


import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.*;

public class BMDPViz {

    // used for logging
    private static Logger logger = Logger.getLogger(BMDPViz.class.getName());

    // time limit for applying the layout algorithm
    private int layoutLimitSeconds;

    // step limit for applying the layout algorithm
    private int layoutLimitSteps;

    // initializes logging
    private static void initLogging() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    public BMDPViz() {
        layoutLimitSeconds = 300;
        layoutLimitSteps = 200;
        initLogging();
    }

    public BMDPViz(int layoutLimitSeconds) {
        this.layoutLimitSeconds = layoutLimitSeconds;
        layoutLimitSteps = 200;
        initLogging();
    }

    public int getLayoutLimitSteps() {
        return layoutLimitSteps;
    }

    public void setLayoutLimitSteps(int layoutLimitSteps) {
        this.layoutLimitSteps = layoutLimitSteps;
    }

    // Gathers data about a bmdp from its gexf file -- import partly taken / based on the gephi toolkit examples
    public AdditionalData gatherMetaData(File file) {
        //Init a project - and therefore a workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        //Get controllers and models
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);

        //Import file
        Container container;
        try {
            container = importController.importFile(file);
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);   //Force DIRECTED
            container.getLoader().setAllowAutoNode(false);  //Don't create missing nodes
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not import GEXF file", ex);
            return null;
        }

        //Append imported data to GraphAPI
        importController.process(container, new DefaultProcessor(), workspace);

        boolean unf = file.getName().contains("_unfOver") || file.getName().contains("_unfUnder");
        AdditionalData metaData = new AdditionalData(unf);
        GraphModel model = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        LinkedList<Integer> colorsEncountered = new LinkedList<>();
        for (Node n : model.getGraph().getNodes()) {
            String id = (String)n.getAttribute("Id");
            if (!id.contains("a")) { // only consider proper nodes, not the intermediate ones of the actions
                metaData.incrNumberOfStates();

                // Ok for some reason, the attribute names get lost in parsing, apparently. See command below:
                // System.out.println(n.getAttributeKeys());
                // 0 is init, 1 is target, 2 is colored, 3 is colorcode,
                // 4 is maxsinglestateepochnumber, 5 is numberOfEpochs

                if ((Boolean) n.getAttribute("0")) {//init
                    metaData.incrNumberOfInitStates();
                }

                if ((Boolean) n.getAttribute("1")) {//target
                    metaData.incrNumberOfTargetStates();
                }

                int outDeg = model.getDirectedGraph().getOutDegree(n);
                if (!metaData.getNumberOfActionsInBeliefDistr().containsKey(outDeg)) {
                    metaData.getNumberOfActionsInBeliefDistr().put(outDeg, 0);
                }
                metaData.getNumberOfActionsInBeliefDistr().put(outDeg, metaData.getNumberOfActionsInBeliefDistr().get(outDeg) + 1);

                if (metaData.isUnfolded()) {
                    Integer maxSingleStateEpochNumber = (Integer)n.getAttribute("4");
                    if (!metaData.getMaxSingleStateEpochInBeliefDistr().containsKey(maxSingleStateEpochNumber)) {
                        metaData.getMaxSingleStateEpochInBeliefDistr().put(maxSingleStateEpochNumber, 0);
                    }
                    metaData.getMaxSingleStateEpochInBeliefDistr().put(maxSingleStateEpochNumber, metaData.getMaxSingleStateEpochInBeliefDistr().get(maxSingleStateEpochNumber) + 1);

                    Integer numOfEpochs = (Integer)n.getAttribute("5");
                    if (!metaData.getNumberOfEpochsInBeliefDistr().containsKey(numOfEpochs)) {
                        metaData.getNumberOfEpochsInBeliefDistr().put(numOfEpochs, 0);
                    }
                    metaData.getNumberOfEpochsInBeliefDistr().put(numOfEpochs, metaData.getNumberOfEpochsInBeliefDistr().get(numOfEpochs) + 1);
                }

                if ((Boolean)n.getAttribute("2")) {//colored
                    metaData.incrNumberOfColorStates();
                    Integer colorCode = (Integer)n.getAttribute("3");
                    if (!colorsEncountered.contains(colorCode)) {
                        colorsEncountered.add(colorCode);
                    }
                }
            }
        }

        metaData.setNumberOfColors(colorsEncountered.size());
        metaData.computeColoredPercentage();
        return metaData;
    }

    // Processes a gexf file and exports it as a pdf of the graph -- partly taken / based on the gephi toolkit examples
    public void prepAndExport(File file) {
        //Init a project - and therefore a workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        //Get controllers and models
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);

        //Import file
        Container container;
        try {
            container = importController.importFile(file);
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);   //Force DIRECTED
            container.getLoader().setAllowAutoNode(false);  // Don't create missing nodes
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not import GEXF file", ex);
            return;
        }

        //Append imported data to GraphAPI
        importController.process(container, new DefaultProcessor(), workspace);

        // Change preview settings
        PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel previewModel = previewController.getModel();
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);

        // ForceAtlas Layout Algorithm
        ForceAtlasLayout layout = new ForceAtlasLayout(null);
        layout.setGraphModel(Lookup.getDefault().lookup(GraphController.class).getGraphModel());
        layout.resetPropertiesValues();
        layout.setInertia(0.1);
        layout.setRepulsionStrength(1000.0);
        layout.setAttractionStrength(10.0);
        layout.setMaxDisplacement(10.0);
        // I'm guessing autostab is the freeze thing
        layout.setFreezeBalance(true);
        layout.setFreezeStrength(80.0);
        layout.setFreezeInertia(0.2);
        layout.setGravity(30.0);
        layout.setOutboundAttractionDistribution(true);
        layout.setAdjustSizes(true);
        layout.setSpeed(1.0);

        layout.initAlgo();
        long startTime = System.currentTimeMillis(); // Rudimentary timekeeping so we don't get stuck here forever with large models
        for (int i = 0; i < layoutLimitSteps && layout.canAlgo() && (System.currentTimeMillis() - startTime) / 1000 <= layoutLimitSeconds; i++){
            layout.goAlgo();
        }

        //Simple PDF export
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        String pdfPath =  file.getAbsolutePath().replace(".gexf", ".pdf");
        try {
            ec.exportFile(new File(pdfPath));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not export PDF file", ex);
        }

    }

}
