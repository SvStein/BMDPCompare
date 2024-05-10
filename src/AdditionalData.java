import java.util.TreeMap;

// Class representing data about a (colored) BeliefMDP
public class AdditionalData {
    public AdditionalData(boolean unf) {
        numberOfStates = 0;
        numberOfTargetStates = 0;
        numberOfInitStates = 0;
        numberOfActionsInBeliefDistr = new TreeMap<>();
        numberOfColors = 0;
        unfolded = unf;
        percentageColored = 0.0;
        numberOfEpochsInBeliefDistr = new TreeMap<>();
        maxSingleStateEpochInBeliefDistr = new TreeMap<>();
    }

    private int numberOfStates;
    private int numberOfTargetStates;
    private int numberOfInitStates;
    private TreeMap<Integer, Integer> numberOfActionsInBeliefDistr;
    private int numberOfColors;
    private int numberOfColorStates;
    private boolean unfolded;
    private double percentageColored;
    private TreeMap<Integer, Integer> numberOfEpochsInBeliefDistr;
    private TreeMap<Integer, Integer> maxSingleStateEpochInBeliefDistr;

    @Override
    public String toString() {
        String res = "";
        res += "Number of (Belief) States: " + numberOfStates + " (init: " + numberOfInitStates + ", target: " + numberOfTargetStates + ")\n";
        res += "Number of different Colors: " + numberOfColors + "\n";
        res += "Distribution of Number of Actions among (Belief) States: " + numberOfActionsInBeliefDistr.toString() + "\n";
        if (unfolded) {
            res += "Unfolded.\n";
            // TODO round / cut off double output
            double actualPercentage = percentageColored*100;
            String percentageString = Double.toString(actualPercentage);
            int cutOffIndex = percentageString.lastIndexOf(".");
            cutOffIndex = Math.min(cutOffIndex + 3, percentageString.length());
            percentageString = percentageString.substring(0, cutOffIndex);
            res += "Colored States: " + numberOfColorStates + " (" + percentageString + " %)\n";
            res += "Distribution of Number of Epochs in entire Belief among (Belief) States:\n" + numberOfEpochsInBeliefDistr.toString() + "\n";
            res += "Distribution of Maximal Number of Epochs for a single POMDP state in Belief \namong (Belief) States:\n" + maxSingleStateEpochInBeliefDistr.toString() + "\n";
        } else {
            res += "Not unfolded";
        }
        return res;
    }

    public int getNumberOfColorStates() {
        return numberOfColorStates;
    }

    public void setNumberOfColorStates(int numberOfColorStates) {
        this.numberOfColorStates = numberOfColorStates;
    }

    public void incrNumberOfColorStates() {
        this.numberOfColorStates++;
    }

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public void setNumberOfStates(int numberOfStates) {
        this.numberOfStates = numberOfStates;
    }

    public void incrNumberOfStates() {
        numberOfStates++;
    }

    public int getNumberOfTargetStates() {
        return numberOfTargetStates;
    }

    public void setNumberOfTargetStates(int numberOfTargetStates) {
        this.numberOfTargetStates = numberOfTargetStates;
    }

    public void incrNumberOfTargetStates() {
        numberOfTargetStates++;
    }

    public int getNumberOfInitStates() {
        return numberOfInitStates;
    }

    public void setNumberOfInitStates(int numberOfInitStates) {
        this.numberOfInitStates = numberOfInitStates;
    }

    public void incrNumberOfInitStates() {
        numberOfInitStates++;
    }

    public TreeMap<Integer, Integer> getNumberOfActionsInBeliefDistr() {
        return numberOfActionsInBeliefDistr;
    }

    public void setNumberOfActionsInBeliefDistr(TreeMap<Integer, Integer> numberOfActionsInBeliefDistr) {
        this.numberOfActionsInBeliefDistr = numberOfActionsInBeliefDistr;
    }

    public int getNumberOfColors() {
        return numberOfColors;
    }

    public void setNumberOfColors(int numberOfColors) {
        this.numberOfColors = numberOfColors;
    }

    public boolean isUnfolded() {
        return unfolded;
    }

    public void setUnfolded(boolean unfolded) {
        this.unfolded = unfolded;
    }

    public double getPercentageColored() {
        return percentageColored;
    }

    public void setPercentageColored(double percentageColored) {
        this.percentageColored = percentageColored;
    }

    public void computeColoredPercentage() {
        assert numberOfStates != 0;
        percentageColored = ((double) numberOfColorStates) / ((double) numberOfStates);
    }

    public TreeMap<Integer, Integer> getNumberOfEpochsInBeliefDistr() {
        return numberOfEpochsInBeliefDistr;
    }

    public void setNumberOfEpochsInBeliefDistr(TreeMap<Integer, Integer> numberOfEpochsInBeliefDistr) {
        this.numberOfEpochsInBeliefDistr = numberOfEpochsInBeliefDistr;
    }

    public TreeMap<Integer, Integer> getMaxSingleStateEpochInBeliefDistr() {
        return maxSingleStateEpochInBeliefDistr;
    }

    public void setMaxSingleStateEpochInBeliefDistr(TreeMap<Integer, Integer> maxSingleStateEpochInBeliefDistr) {
        this.maxSingleStateEpochInBeliefDistr = maxSingleStateEpochInBeliefDistr;
    }
}
