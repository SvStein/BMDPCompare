import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Main {

    // Used for logging (if you could not tell)
    private static Logger logger = Logger.getLogger(Main.class.getName());

    // Initializes logging to console
    private static void initLogging() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    // Main. Here, we start shit
    public static void main(String[] args) throws IOException {
        initLogging();
        createOverviewDoc("/home/spook/Documents/bunfbench_results", "/home/spook/Documents/bunfbench_results/pdfFiles", true);
        //createSummaryPDF("/home/spook/Documents/hi/costs_ogOver.gexf","/home/spook/Documents/hi/costs_unfOver.gexf", "/home/spook/Documents/hi/pdfFiles/over.pdf", "costs");
        //createSummaryPDF("/home/spook/Documents/hi/costs_ogUnder.gexf","/home/spook/Documents/hi/costs_unfUnder.gexf", "/home/spook/Documents/hi/pdfFiles/under.pdf", "costs");
    }

    // Iterates recursively over files in specified path and creates a pdf visualization for all gexf files found
    private static void exportPDFs(String dirPath) throws IOException {
        Path dir = Paths.get(dirPath);
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.forEach(path -> exportPDF(path.toFile()));
        }
    }

    // Creates a pdf visualization of a gexf file
    private static void exportPDF(File file) {
        if (!file.isDirectory() && file.getName().endsWith(".gexf")) {
            logger.info("MAKE PDF FOR: " + file.getName());
            BMDPViz visualizer = new BMDPViz();
            visualizer.prepAndExport(file);
        }
    }

    // Creates a pdf file with a summary of an instance (one og bmdp + one unfolded bmdp)
    private static void createSummaryPDF(String ogPath, String unfPath, String outputPath, String instanceName) throws IOException {
        logger.info("CREATING SUMMARY FOR " + instanceName);
        float fontSize = 15;
        float leading = 1.5f * fontSize;
        BMDPViz viz = new BMDPViz();
        AdditionalData metaDataUnf = viz.gatherMetaData(Paths.get(unfPath).toFile());
        AdditionalData metaDataOg = viz.gatherMetaData(Paths.get(ogPath).toFile());
        String[] unfLines;
        String[] ogLines;
        if (metaDataUnf != null) {
            unfLines = metaDataUnf.toString().split("\n");
        } else {
            unfLines = new String[0];
        }
        if (metaDataOg != null) {
            ogLines = metaDataOg.toString().split("\n");
        } else {
            ogLines = new String[0];
        }

        PDDocument doc = new PDDocument();
        PDPage summary = new PDPage();
        doc.addPage(summary);
        PDPageContentStream contentStream = new PDPageContentStream(doc, summary);
        contentStream.beginText();

        contentStream.newLineAtOffset(25, 700);
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD), 20);
        contentStream.showText(instanceName);

        contentStream.newLineAtOffset(0, -2*leading);
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD), fontSize);
        contentStream.showText("BMDP of unbounded Query");

        contentStream.newLineAtOffset(0, -leading);
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN), fontSize);
        for (String ogLine : ogLines) {
            if (!ogLine.isEmpty()) {
                contentStream.showText(ogLine);
                contentStream.newLineAtOffset(0, -leading);
            }
        }
        contentStream.newLineAtOffset(0, -leading);
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD), fontSize);
        contentStream.showText("BMDP of unfolding wrt bounded Query");

        contentStream.newLineAtOffset(0, -leading);
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN), fontSize);
        for (String unfLine : unfLines) {
            if (!unfLine.isEmpty()) {
                contentStream.showText(unfLine);
                contentStream.newLineAtOffset(0, -leading);
            }
        }

        contentStream.endText();
        contentStream.close();
        doc.save(outputPath);
        doc.close();
    }


    // creates a single pdf containing a summary page and two graph pages per instance where both og and unfolded bmdp can be found (recursively) in the resultsdirectorypath
    private static void createOverviewDoc (String resultsDirectoryPath, String outputDirectoryPath, boolean removeIntermediateResults) throws IOException {
        File outputDir = new File(outputDirectoryPath);
        clearPDFs(outputDirectoryPath);
        clearPDFs(resultsDirectoryPath);
        exportPDFs(resultsDirectoryPath);
        summarizeAndMergePDFs(resultsDirectoryPath, true, outputDirectoryPath);

        File[] mergedInstances = outputDir.listFiles();
        PDFMergerUtility merger = new PDFMergerUtility();
        for (int i = 0; i < Objects.requireNonNull(mergedInstances).length; i++) {
            if (mergedInstances[i].getName().endsWith(".pdf")) {
                merger.addSource(mergedInstances[i].getAbsolutePath());
            }
        }
        String doc = outputDirectoryPath;
        if (!outputDirectoryPath.endsWith("/")) doc += "/";
        doc += "overview";
        merger.setDestinationFileName(doc);
        merger.mergeDocuments(null);

        if (removeIntermediateResults) {
            clearIntermediates(resultsDirectoryPath, doc);
            clearIntermediates(outputDirectoryPath, doc);
        }
    }

    // Removes all pdfs that are not the final pdf that can be found recursively in a directory
    private static void clearIntermediates (String dirPath, String resultName) throws IOException {
        Path dir = Paths.get(dirPath);
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.forEach(path -> clearIntermediate(path.toFile(), resultName));
        }
    }

    // Removes a pdf if it is not the final pdf file
    private static void clearIntermediate (File file, String resultName) {
        if (!file.isDirectory() && file.getName().endsWith(".pdf") && !file.getAbsolutePath().contains(resultName)) {
            if (file.delete()) {
                logger.info("DELETED FILE " + file.getName());
            } else {
                logger.warning("UNABLE TO DELETE FILE " + file.getName());
            }
        }
    }

    // Removes all pdfs that can be recursively found in a directory
    private static void clearPDFs (String dirPath) throws IOException {
        Path dir = Paths.get(dirPath);
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.forEach(path -> clearPDF(path.toFile()));
        }
    }

    // Removes a pdf file
    private static void clearPDF (File file) {
        if (!file.isDirectory() && file.getName().endsWith(".pdf")) {
            if (file.delete()) {
                logger.info("DELETED FILE " + file.getName());
            } else {
                logger.warning("UNABLE TO DELETE FILE " + file.getName());
            }
        }
    }

    // For all instances consisting of two corresponding graph pdfs, creates a summary page for them and merges all three pages
    private static void summarizeAndMergePDFs(String resultsDirectoryPath) throws IOException {
        summarizeAndMergePDFs(resultsDirectoryPath, false, "");
    }

    // For all instances consisting of two corresponding graph pdfs, creates a summary page for them and merges all three pages
    private static void summarizeAndMergePDFs(String resultsDirectoryPath, boolean rerouteOutput, String outputDirectoryPath) throws IOException {
        // Document all pdf files we can recursively find in resultsDirectoryPath
        TreeMap<String, Boolean[]> pdfFiles = new TreeMap<>(); // I would use a fucking pair thingy but that's only in that javafx thing and I don't wanna open a whole new can of worms with that ugh
        Path dir = Paths.get(resultsDirectoryPath);
        TreeMap<String, Boolean[]> finalPdfFiles = pdfFiles; // what is this effectively final shit what does that even do????
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.forEach(path -> fillFileMap(path.toFile(), finalPdfFiles));
        }

        // Keep only those that have both graphs available
        pdfFiles = pdfFiles.entrySet()
                .stream()
                .filter(Main::filterTwoTrue)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (existing, replacement) -> existing, TreeMap<String,Boolean[]>::new));

        // For those, create summary and merge
        for (Map.Entry<String, Boolean[]> mergeCase : pdfFiles.entrySet()) {
            String path = mergeCase.getKey();
            String ogPath;
            String unfPath;
            String sumPath;
            if (path.endsWith("_Over")) {
                ogPath = path.replace("_Over", "_ogOver");
                unfPath = path.replace("_Over", "_unfOver");
                sumPath = path.replace("_Over", "_sumOver");
            } else if (path.endsWith("_Under")) {
                ogPath = path.replace("_Under", "_ogUnder");
                unfPath = path.replace("_Under", "_unfUnder");
                sumPath = path.replace("_Under", "_sumUnder");
            } else {
                logger.severe("Welp, that should not happen");
                assert false;
                continue;
            }

            createSummaryPDF(ogPath + ".gexf", unfPath + ".gexf", sumPath + ".pdf", path.substring(path.lastIndexOf("/") + 1));

            if (rerouteOutput) {
                path = outputDirectoryPath + path.substring(path.lastIndexOf("/")) + ".pdf";

            } else {
                path = path + ".pdf";
            }
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.addSource(sumPath + ".pdf");
            merger.addSource(ogPath + ".pdf");
            merger.addSource(unfPath + ".pdf");
            merger.setDestinationFileName(path);
            merger.mergeDocuments(null);
        }
    }

    // Filter to only keep entries where both files are present
    private static boolean filterTwoTrue(Map.Entry<String, Boolean[]> entry) {
        assert entry.getValue().length == 2;
        return ((Boolean[])entry.getValue())[0] && ((Boolean[])entry.getValue())[1];
    }

    // Debugging utility to view contents of a tree map
    private static void showMap(TreeMap<String, Boolean[]> map) {
        int i = 0;
        for (Map.Entry<String, Boolean[]> m : map.entrySet()) {
            String og = ((Boolean[])m.getValue())[0]? "TRUE, " : "FALSE, ";
            String unf = ((Boolean[])m.getValue())[1]? "TRUE" : "FALSE";
            System.out.println("(" + i + ") Case " + m.getKey() + ": " + og + unf);
            i++;
        }
    }

    // Sorts file into map that documents if we have og and/or unf for certain instances
    private static void fillFileMap(File file, TreeMap<String, Boolean[]> pdfFiles) {
        String absPath = file.getAbsolutePath();
        String onlyPath = absPath.substring(0, absPath.lastIndexOf("/") + 1);
        String fileName = file.getName();
        if (!file.isDirectory() && fileName.endsWith(".pdf")) {
            boolean isOG = fileName.contains("ogOver") || fileName.contains("ogUnder");
            boolean isUNF = fileName.contains("unfOver") || fileName.contains("unfUnder");
            String key = fileName.replace("ogOver", "Over");
            key = key.replace("ogUnder", "Under");
            key = key.replace("unfOver", "Over");
            key = key.replace("unfUnder", "Under");
            key = key.replace(".pdf", "");
            key = onlyPath + key;
            if (!pdfFiles.containsKey(key)) {
                pdfFiles.put(key, new Boolean[]{Boolean.FALSE, Boolean.FALSE});
            }
            pdfFiles.get(key)[0] |= isOG;
            pdfFiles.get(key)[1] |= isUNF;
        }
    }
}