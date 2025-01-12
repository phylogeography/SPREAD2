package parsers;

import java.io.IOException;
import java.util.LinkedList;

import exceptions.AnalysisException;
import jebl.evolution.io.ImportException;
import jebl.evolution.trees.RootedTree;
import settings.parsing.ContinuousTreeSettings;
import structure.data.Attribute;
import structure.data.AxisAttributes;
import structure.data.Layer;
import structure.data.SpreadData;
import structure.data.TimeLine;
import structure.data.attributable.Area;
import structure.data.attributable.Line;
import structure.data.attributable.Point;
import structure.geojson.GeoJsonData;
import utils.Utils;

public class ContinuousTreeSpreadDataParser {

	private final ContinuousTreeSettings settings;

	public ContinuousTreeSpreadDataParser(ContinuousTreeSettings settings) {

		this.settings = settings;

	}// END: Constructor

	public SpreadData parse() throws IOException, ImportException, AnalysisException {

		TimeLine timeLine = null;
		LinkedList<Attribute> mapAttributes = null;
		LinkedList<Attribute> lineAttributes = null;
		LinkedList<Attribute> pointAttributes = null;
		LinkedList<Attribute> areaAttributes = null;
		
		LinkedList<Layer> layersList = new LinkedList<Layer>();

		// ---IMPORT---//

		// tree
		RootedTree rootedTree;
		if (settings.rootedTree != null) {
			rootedTree = settings.rootedTree;
		} else {
			rootedTree = Utils.importRootedTree(settings.treeFilename);
		}
		
		// ---PARSE AND FILL STRUCTURES---//

		TimeParser timeParser = new TimeParser(settings.mrsd);
		timeLine = timeParser.getTimeLine(rootedTree.getHeight(rootedTree.getRootNode()));

		System.out.println("Parsed time line");

		ContinuousTreeParser treeParser = new ContinuousTreeParser(rootedTree, //
				settings.xCoordinate, //
				settings.yCoordinate, //
				settings.externalAnnotations, //
				settings.hpd,
				timeParser, //
				settings.timescaleMultiplier);

		treeParser.parseTree();

		System.out.println("Parsed the tree");
		
		lineAttributes = treeParser.getLineAttributes();
		pointAttributes = treeParser.getPointAttributes();
		areaAttributes = treeParser.getAreaAttributes();
		
		System.out.println("Parsed tree attributes");

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

		// ---DATA LAYER (TREE LINES & POINTS, AREAS)---//

		LinkedList<Line> linesList = treeParser.getLinesList();
		LinkedList<Point> pointsList = treeParser.getPointsList();
		LinkedList<Area> areasList = treeParser.getAreasList();

		String treeLayerId = Utils.splitString(settings.treeFilename, "/");
		Layer treeLayer = new Layer(treeLayerId, //
				"Tree layer", //
				pointsList, //
				linesList, //
				areasList);
		layersList.add(treeLayer);

		AxisAttributes axis = new AxisAttributes(settings.xCoordinate, settings.yCoordinate);

		return new SpreadData(timeLine, //
				axis, //
				mapAttributes, //
				lineAttributes, //
				pointAttributes, //
				areaAttributes,
				null, // locations
				layersList//
		);
	}// END: parse

}// END: class
