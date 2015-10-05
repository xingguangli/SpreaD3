package parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import structure.data.Attribute;
import structure.data.attributable.Area;
import structure.data.attributable.Line;
import structure.data.attributable.Point;
import structure.data.primitive.Coordinate;
import structure.data.primitive.Polygon;
import utils.Utils;
import exceptions.AnalysisException;

public class ContinuousTreeParser {

	private RootedTree rootedTree;
	private String xCoordinate;
	private String yCoordinate;
	private String hpd;
	private String mrsd;
	private double timescaleMultiplier;
	private TimeParser timeParser;

	private LinkedList<Attribute> uniqueBranchAttributes;
	private LinkedList<Attribute> uniqueNodeAttributes;

	private LinkedList<Line> linesList;
	private LinkedList<Point> pointsList;
	private LinkedList<Area> areasList;

	public ContinuousTreeParser(RootedTree rootedTree, //
			String xCoordinate, //
			String yCoordinate, //
			String hpd, //
			String mrsd, //
			double timescaleMultiplier //

	) {

		this.rootedTree = rootedTree;
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
		this.hpd = hpd;
		this.mrsd = mrsd;
		this.timescaleMultiplier = timescaleMultiplier;

		this.uniqueBranchAttributes = new LinkedList<Attribute>();
		this.uniqueNodeAttributes = new LinkedList<Attribute>();

		this.linesList = new LinkedList<Line>();
		this.pointsList = new LinkedList<Point>();
		this.areasList = new LinkedList<Area>();

	}// END: Constructor

	public void parseTree() throws AnalysisException {

		HashMap<Node, Point> pointsMap = new HashMap<Node, Point>();
		
		//TODO
		String prefix = xCoordinate.replaceAll("\\d*$", "");
		String modalityAttributeName = prefix.concat("_").concat(hpd)
				.concat("%").concat("HPD_modality");
		
		this.timeParser = new TimeParser(mrsd);
		timeParser.parseTime();

		int index = 0;
		for (Node node : rootedTree.getNodes()) {
			if (!rootedTree.isRoot(node)) {

				// node parsed first

				Coordinate nodeCoordinate = null;
				try {

					Double nodeCoordinateY = (Double) Utils
							.getObjectNodeAttribute(node, yCoordinate);
					Double nodeCoordinateX = (Double) Utils
							.getObjectNodeAttribute(node, xCoordinate);

					nodeCoordinate = new Coordinate(nodeCoordinateX,
							nodeCoordinateY);

				} catch (AnalysisException e) {

					String message = xCoordinate
							+ " and/or "
							+ yCoordinate
							+ " attribute could not be found in the tree. Resulting visualisation may be incomplete!";
					System.out.println(message);
					continue;

				}

				Point nodePoint = pointsMap.get(node);
				if (nodePoint == null) {

					nodePoint = createPoint(index, node, nodeCoordinate);
					pointsMap.put(node, nodePoint);
					index++;

				}// END: null check

				// parent node parsed second
				Node parentNode = rootedTree.getParent(node);

				Double parentCoordinateX = (Double) Utils
						.getObjectNodeAttribute(parentNode, xCoordinate);
				Double parentCoordinateY = (Double) Utils
						.getObjectNodeAttribute(parentNode, yCoordinate);

				Coordinate parentCoordinate = new Coordinate(parentCoordinateX,
						parentCoordinateY);
				Point parentPoint = pointsMap.get(parentNode);
				if (parentPoint == null) {

					parentPoint = createPoint(index, parentNode,
							parentCoordinate);
					pointsMap.put(parentNode, parentPoint);
					index++;

				}// END: null check

				Line line = new Line(parentPoint.getId(), //
						nodePoint.getId(), //
						parentPoint.getStartTime(), //
						nodePoint.getStartTime(), //
						nodePoint.getAttributes() //
				);

				linesList.add(line);

				if (!rootedTree.isExternal(node)) {

					Integer modality = (Integer) Utils.getObjectNodeAttribute(
							node, modalityAttributeName);

					for (int m = 1; m <= modality; m++) {

						// trait1_80%HPD_1
						String xCoordinateHPDName = xCoordinate.concat("_")
								.concat(hpd).concat("%")
								.concat(Utils.HPD.toUpperCase() + "_" + m);
						String yCoordinateHPDName = yCoordinate.concat("_")
								.concat(hpd).concat("%")
								.concat(Utils.HPD.toUpperCase() + "_" + m);

						Object[] xCoordinateHPD = Utils
								.getObjectArrayNodeAttribute(node,
										xCoordinateHPDName);

						Object[] yCoordinateHPD = Utils
								.getObjectArrayNodeAttribute(node,
										yCoordinateHPDName);

						List<Coordinate> coordinateList = new ArrayList<Coordinate>();
						for (int c = 0; c < xCoordinateHPD.length; c++) {

							Double xCoordinate = (Double) xCoordinateHPD[c];
							Double yCoordinate = (Double) yCoordinateHPD[c];

							Coordinate coordinate = new Coordinate(xCoordinate,
									yCoordinate);
							coordinateList.add(coordinate);

						}// END: c loop

						Polygon polygon = new Polygon(coordinateList);

						// TODO: no attributes?
						Area area = new Area(polygon, nodePoint.getStartTime(),
								null);
						areasList.add(area);

					}// END: modality loop

				}// external check

			}// END: root check
		}// END: nodes loop

		pointsList.addAll(pointsMap.values());

		// collect attributes from lines
		Map<String, Attribute> branchAttributesMap = new HashMap<String, Attribute>();

		for (Line line : linesList) {

			for (Entry<String, Object> entry : line.getAttributes().entrySet()) {

				String attributeId = entry.getKey();
				Object attributeValue = entry.getValue();

				if (branchAttributesMap.containsKey(attributeId)) {

					Attribute attribute = branchAttributesMap.get(attributeId);

					if (attribute.getScale().equals(Attribute.ORDINAL)) {

						attribute.getDomain().add(attributeValue);

					} else {

						double value = Utils
								.round((double) attributeValue, 100);

						if (value < attribute.getRange()[Attribute.MIN_INDEX]) {
							attribute.getRange()[Attribute.MIN_INDEX] = value;
						} // END: min check

						if (value > attribute.getRange()[Attribute.MAX_INDEX]) {
							attribute.getRange()[Attribute.MAX_INDEX] = value;
						} // END: max check

					} // END: scale check

				} else {

					Attribute attribute;
					if (attributeValue instanceof Double) {

						double[] range = new double[2];
						range[Attribute.MIN_INDEX] = (double) attributeValue;
						range[Attribute.MAX_INDEX] = (double) attributeValue;

						attribute = new Attribute(attributeId, range);

					} else {

						HashSet<Object> domain = new HashSet<Object>();
						domain.add(attributeValue);

						attribute = new Attribute(attributeId, domain);

					} // END: isNumeric check

					branchAttributesMap.put(attributeId, attribute);

				} // END: key check

			} // END: attributes loop

		} // END: lines loop

		uniqueBranchAttributes.addAll(branchAttributesMap.values());

		// collect attributes from nodes
		Map<String, Attribute> nodeAttributesMap = new HashMap<String, Attribute>();

		for (Point point : pointsList) {

			for (Entry<String, Object> entry : point.getAttributes().entrySet()) {

				String attributeId = entry.getKey();
				Object attributeValue = entry.getValue();

				if (nodeAttributesMap.containsKey(attributeId)) {

					Attribute attribute = nodeAttributesMap.get(attributeId);

					if (attribute.getScale().equals(Attribute.ORDINAL)) {

						attribute.getDomain().add(attributeValue);

					} else {

						double value = Utils
								.round((double) attributeValue, 100);

						if (value < attribute.getRange()[Attribute.MIN_INDEX]) {
							attribute.getRange()[Attribute.MIN_INDEX] = value;
						} // END: min check

						if (value > attribute.getRange()[Attribute.MAX_INDEX]) {
							attribute.getRange()[Attribute.MAX_INDEX] = value;
						} // END: max check

					} // END: scale check

				} else {

					Attribute attribute;
					if (attributeValue instanceof Double) {

						double[] range = new double[2];
						range[Attribute.MIN_INDEX] = (double) attributeValue;
						range[Attribute.MAX_INDEX] = (double) attributeValue;

						attribute = new Attribute(attributeId, range);

					} else {

						HashSet<Object> domain = new HashSet<Object>();
						domain.add(attributeValue);

						attribute = new Attribute(attributeId, domain);

					} // END: isNumeric check

					nodeAttributesMap.put(attributeId, attribute);

				} // END: key check

			} // END: attributes loop

		}// END: points loop

		uniqueNodeAttributes.addAll(branchAttributesMap.values());

	}// END: parseTree

	private Point createPoint(int index, Node node, Coordinate coordinate) {

		String id = "point_" + index;
		Double height = Utils.getNodeHeight(rootedTree, node)
				* timescaleMultiplier;
		String startTime = timeParser.getNodeDate(height);

		Map<String, Object> attributes = new LinkedHashMap<String, Object>();
		for (String attributeName : node.getAttributeNames()) {

			Object nodeAttribute = node.getAttribute(attributeName);

			if (!(nodeAttribute instanceof Object[])) {
				attributes.put(attributeName, nodeAttribute);
			} // END: multivariate check

		} // END: attributes loop

		Point point = new Point(id, coordinate, startTime, attributes);

		return point;
	}// END: createPoint

	public LinkedList<Line> getLinesList() {
		return linesList;
	}

	public LinkedList<Point> getPointsList() {
		return pointsList;
	}

	public LinkedList<Area> getAreasList() {
		return areasList;
	}

	public LinkedList<Attribute> getLineAttributes() {
		return uniqueBranchAttributes;
	}

	public LinkedList<Attribute> getPointAttributes() {
		return uniqueNodeAttributes;
	}

}// END: class