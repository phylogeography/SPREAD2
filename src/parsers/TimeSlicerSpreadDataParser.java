package parsers;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;

import exceptions.AnalysisException;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.trees.RootedTree;
import settings.parsing.TimeSlicerSettings;
import structure.data.Attribute;
import structure.data.AxisAttributes;
import structure.data.Layer;
import structure.data.SpreadData;
import structure.data.TimeLine;
import structure.data.attributable.Area;
import structure.geojson.GeoJsonData;
import utils.Utils;

public class TimeSlicerSpreadDataParser {

	// private static final int FIRST_SLICE_INDEX = 0;
	private final TimeSlicerSettings settings;

	public TimeSlicerSpreadDataParser(TimeSlicerSettings settings) {
		this.settings = settings;
	}// END: Constructor

	public SpreadData parse() throws IOException, ImportException, AnalysisException {

		TimeLine timeLine = null;
		// TODO: we need those axis Att's
		AxisAttributes axis = null;
		LinkedList<Attribute> mapAttributes = null;
		// LinkedList<Attribute> lineAttributes = null;
		// LinkedList<Attribute> pointAttributes = null;
		LinkedList<Attribute> areaAttributes = null;
		
		LinkedList<Layer> layersList = new LinkedList<Layer>();

		// ---IMPORT---//

		// import slice heights
		Double sliceHeights[] = null;
		if (settings.sliceHeightsFilename != null) {

			SliceHeightsParser sliceHeightsParser = new SliceHeightsParser(settings.sliceHeightsFilename);
			sliceHeights = sliceHeightsParser.parseSliceHeights();

		} else if (settings.treeFilename != null) {

			RootedTree rootedTree = Utils.importRootedTree(settings.treeFilename);
			sliceHeights = generateSliceHeights(rootedTree, settings.intervals);

		} else {

			throw new AnalysisException("Error parsing slice heights!");

		} // END: settings check

		// sort them in ascending order
		Arrays.sort(sliceHeights);

		System.out.println("Using as slice heights: ");
		Utils.printArray(sliceHeights);

		// import trees
		NexusImporter treesImporter = new NexusImporter(new FileReader(settings.treesFilename));

		// ---PARSE AND FILL STRUCTURES---//

		TimeParser timeParser = new TimeParser(settings.mrsd);
		// timeParser.parseTime();
		timeLine = timeParser.getTimeLine(sliceHeights[sliceHeights.length - 1]);

		System.out.println("Parsed time line");

		// ---GEOJSON LAYER---//

		if (settings.geojsonFilename != null) {

			GeoJSONParser geojsonParser = new GeoJSONParser(settings.geojsonFilename);
			GeoJsonData geojson = geojsonParser.parseGeoJSON();

			mapAttributes = geojsonParser.getUniqueMapAttributes();

			String geojsonLayerId = Utils.splitString(settings.geojsonFilename, "/");
			Layer geojsonLayer = new Layer(geojsonLayerId, //
					"GeoJson layer", //
					geojson);

			layersList.add(geojsonLayer);

			System.out.println("Parsed map attributes");

		} // END: null check

		// ---DATA LAYER (CONTOURING AREAS)---//

		int assumedTrees;
		if (settings.assumedTrees != null) {
			assumedTrees = settings.assumedTrees;
		} else {
			assumedTrees = getAssumedTrees(settings.treesFilename);
		}

		if (settings.burnIn >= assumedTrees) {
			throw new AnalysisException("Trying to burn too many trees!");
		}

		TimeSlicerParser parser = new TimeSlicerParser(settings.trait, //
				treesImporter, //
				timeParser, //
				settings.burnIn, //
				assumedTrees, //
//				settings.hasRRWrate, //
				settings.rrwRate, //
				settings.hpdLevel, //
				settings.gridSize, //
				settings.timescaleMultiplier, //
				sliceHeights //
		);
		parser.parse();

		areaAttributes = parser.getAreaAttributes();
		
		LinkedList<Area> areasList = parser.getAreasList();

		String contoursLayerId = Utils.splitString(settings.treesFilename, "/");
		Layer contoursLayer = new Layer(contoursLayerId, //
				"Density contour layer", //
				null, //
				null, //
				areasList //
		);

		layersList.add(contoursLayer);

		return new SpreadData(timeLine, //
				axis, //
				mapAttributes, //
				null, // lineAttributes
				null, // pointAttributes
				areaAttributes , // areaAttributes
				null, // locationsList
				layersList //
		);
	}// END: parse

	private Double[] generateSliceHeights(RootedTree rootedTree, int numberOfIntervals) {

		double rootHeight = rootedTree.getHeight(rootedTree.getRootNode());
		Double[] timeSlices = new Double[numberOfIntervals];

		for (int i = 0; i < numberOfIntervals; i++) {
			timeSlices[i] = rootHeight - (rootHeight / (double) numberOfIntervals) * ((double) i);
		}

		return timeSlices;
	}// END: generateSliceHeights

	public static int getAssumedTrees(String file) throws IOException {
		// this method is a hack

		InputStream is = new BufferedInputStream(new FileInputStream(file));

		try {

			String mark = ";";
			int markCount = 0;
			int markBorder = 6;

			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;

			while ((readChars = is.read(c)) != -1) {

				empty = false;
				for (int i = 0; i < readChars; i++) {

					if (String.valueOf((char) c[i]).equalsIgnoreCase(mark)) {

						markCount++;

					}

					if (c[i] == '\n' && markCount > markBorder) {
						count++;
					}

				}

			} // END: loop

			count = count - 1;
			return (count == 0 && !empty) ? 1 : count;

		} finally {
			is.close();
		}
	}// END: getAssumedTrees

}// END: class
