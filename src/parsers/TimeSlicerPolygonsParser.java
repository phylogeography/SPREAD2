package parsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.trees.RootedTree;
import utils.ProgressBar;
import utils.Trait;
import utils.Utils;
import contouring.ContourMaker;
import contouring.ContourPath;
import contouring.ContourWithSnyder;
import data.structure.Coordinate;
import data.structure.Polygon;
import exceptions.AnalysisException;

public class TimeSlicerPolygonsParser {

	private double[] sliceHeights;
	private NexusImporter treesImporter;
	
	// how many trees to burn in (in #trees)
	private int burnIn;
	// private String locationTrait;
	private int gridSize;
	private double hpdLevel;
	private String[] traits;
	private int assumedTrees;
	private String mrsd;
    private double timescaleMultiplier;
	
	public TimeSlicerPolygonsParser(
			double[] sliceHeights,
			NexusImporter treesImporter, //
			String[] traits, //
			// String locationTrait, //
			int burnIn, //
			int gridSize, //
			double hpdValue, //
			String mrsd,//
			double timescaleMultiplier //
			) {

		this.sliceHeights = sliceHeights;
		this.treesImporter = treesImporter;
		this.traits = traits;
		this.burnIn = burnIn;
		// this.locationTrait = locationTrait;
		this.gridSize = gridSize;
		this.hpdLevel = hpdValue;
		this.mrsd = mrsd;
        this.timescaleMultiplier = timescaleMultiplier;
		
		this.assumedTrees = 10000;

	}// END: Constructor

	public LinkedList<Polygon> parsePolygons() throws IOException,
			ImportException, AnalysisException {

		LinkedList<Polygon> polygonsList = new LinkedList<Polygon>();
		
		TimeParser timeParser = new TimeParser(mrsd);
		timeParser.parseTime();

		// ---PARSE TREES---//

		int barLength = 100;
		int treesRead = 0;
		double stepSize = (double) barLength / (double) assumedTrees;

		System.out.println("Reading trees (bar assumes " + assumedTrees
				+ " trees)");

		ProgressBar progressBar = new ProgressBar(barLength);
		progressBar.start();

		System.out
				.println("0                        25                       50                       75                       100%");
		System.out
				.println("|------------------------|------------------------|------------------------|------------------------|");

		RootedTree currentTree;
		ConcurrentHashMap<String, HashMap<Double, List<double[]>>> traitsMap = new ConcurrentHashMap<String, HashMap<Double, List<double[]>>>();

		int counter = 0;
		while (treesImporter.hasTree()) {

			try {

				currentTree = (RootedTree) treesImporter.importNextTree();

				if (counter >= burnIn) {

					new AnalyzeTree(traitsMap, //
							currentTree, //
							sliceHeights, //
							traits //
					).run();

					treesRead++;
				}// END: burnin check

				counter++;
				double progress = (stepSize * counter) / barLength;
				progressBar.setProgressPercentage(progress);

			} catch (Exception e) {
				// catch any unchecked exceptions coming from Runnable, pass
				// them to handlers
				throw new AnalysisException(e.getMessage());
			}// END: try-catch

		}// END: trees loop
		progressBar.showCompleted();
		progressBar.setShowProgress(false);

		System.out.print("\n");
		System.out.println("Analyzed " + treesRead + " trees with burn-in of "
				+ burnIn + " for the total of " + counter + " trees");

		// loop over traits
		for (String traitName : traitsMap.keySet()) {

			HashMap<Double, List<double[]>> traitMap = traitsMap.get(traitName);

			System.out.println("Creating contours for " + traitName
					+ " trait at " + hpdLevel + " HPD level");
			System.out
					.println("0                        25                       50                       75                       100%");
			System.out
					.println("|------------------------|------------------------|------------------------|------------------------|");

			counter = 0;
			stepSize = (double) barLength / (double) traitMap.size();

			progressBar = new ProgressBar(barLength);
			progressBar.start();

			// Iterator<Double> iterator = traitMap.keySet().iterator();
			// while (iterator.hasNext()) {
			// Double sliceHeight = iterator.next();

			// ---MAKE CONTOURS---//

			for (Double sliceHeight : traitMap.keySet()) {

				List<double[]> coords = traitMap.get(sliceHeight);
				int n = coords.size();

				double[] x = new double[n];
				double[] y = new double[n];

				for (int i = 0; i < n; i++) {

					if (coords.get(i) == null) {
						System.out.println("null found");
					}

					x[i] = coords.get(i)[Utils.LATITUDE_INDEX];
					y[i] = coords.get(i)[Utils.LONGITUDE_INDEX];

				}// END: i loop

				ContourMaker contourMaker = new ContourWithSnyder(x, y,
						gridSize);
				ContourPath[] paths = contourMaker.getContourPaths(hpdLevel);

				for (ContourPath path : paths) {

					double[] latitude = path.getAllX();
					double[] longitude = path.getAllY();

					List<Coordinate> coordinateList = new ArrayList<Coordinate>();

					for (int i = 0; i < latitude.length; i++) {
						coordinateList.add(new Coordinate(latitude[i],
								longitude[i]));
					}

					Map<String, Trait> attributes = new LinkedHashMap<String, Trait>();

					Trait hpdTrait = new Trait(hpdLevel);
					attributes.put(Utils.HPD, hpdTrait);

					Trait attributeName = new Trait(traitName);
					attributes.put(Utils.TRAIT, attributeName);

					String startTime = timeParser.getNodeDate(sliceHeight * timescaleMultiplier);

					Polygon polygon = new Polygon(coordinateList, //
							startTime, //
							attributes //
					);

					polygonsList.add(polygon);
				}// END: paths loop

				counter++;
				double progress = (stepSize * counter) / barLength;
				progressBar.setProgressPercentage(progress);

			}// END: iterate

			progressBar.showCompleted();
			progressBar.setShowProgress(false);
			System.out.print("\n");

		}// END: traits loop

		return polygonsList;
	}// END: parsePolygons

	public void setAssumedTrees(int assumedTrees) {
		this.assumedTrees = assumedTrees;
	}// END: setAssumedTrees

}// END: class