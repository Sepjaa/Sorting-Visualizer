package fi.sepjaa.visualizer.ui.pathfinding.algorithm.implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import fi.sepjaa.visualizer.pathfinding.ConnectedNodePair;
import fi.sepjaa.visualizer.pathfinding.ImmutablePathfindingData;
import fi.sepjaa.visualizer.pathfinding.Node;
import fi.sepjaa.visualizer.pathfinding.NodeDistance;
import fi.sepjaa.visualizer.pathfinding.NodeUtilities;
import fi.sepjaa.visualizer.pathfinding.PathfindingData;

@Component
public class Dijkstra extends AbstractPathfindingAlgorithm {

	@Override
	public void find(PathfindingData data) {
		ImmutablePathfindingData copy = data.getCopy();
		ImmutableMap<Integer, Node> nodes = copy.getNodes();
		int start = copy.getStart();
		int end = copy.getEnd();

		LOG.debug("Starting dijkstra pathfind with start {} end {}", nodes.get(start), nodes.get(end));
		Map<Integer, NodeDistance> distances = initializeDistances(nodes, start);
		List<Integer> evaluated = new ArrayList<>();
		Optional<Integer> current = getNext(distances, evaluated);
		while (current.isPresent()) {
			LOG.trace("Current {}", current);
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
			evaluated = evaluate(data, current.get(), nodes, distances);
			current = getNext(distances, evaluated);
			if (current.isPresent() && current.get() == end) {
				LOG.debug("Reached end node as current with distance {}", distances.get(end));
				data.addAndReturnEvaluated(current.get(), distances);
				break;
			}
		}
		LOG.debug("Stopped dijkstra");
	}

	private Optional<Integer> getNext(Map<Integer, NodeDistance> distances, List<Integer> evaluated) {
		Optional<NodeDistance> next = distances.values().stream().filter(n -> !evaluated.contains(n.getNodeId()))
				.sorted(NodeUtilities.getNodeDistanceComparator()).findFirst();
		Integer result = null;
		if (next.isPresent() && next.get().getDistance() < Float.MAX_VALUE) {
			result = next.get().getNodeId();
		}
		return Optional.ofNullable(result);
	}

	private Map<Integer, NodeDistance> initializeDistances(ImmutableMap<Integer, Node> nodes, int start) {
		Map<Integer, NodeDistance> distances = new HashMap<>();
		for (Node n : nodes.values()) {
			if (n.getId() != start) {
				distances.put(n.getId(), new NodeDistance(n.getId()));
			} else {
				distances.put(n.getId(), new NodeDistance(n.getId(), n.getId(), 0f));
			}
		}
		return distances;
	}

	private List<Integer> evaluate(PathfindingData data, int nodeId, ImmutableMap<Integer, Node> nodes,
			Map<Integer, NodeDistance> distances) {
		List<Integer> evaluatedNodes = data.addAndReturnEvaluated(nodeId, distances);
		Node node = nodes.get(nodeId);
		float nodeDistance = distances.get(nodeId).getDistance();
		for (Integer neighbourId : node.getConnections()) {
			if (!evaluatedNodes.contains(neighbourId) && !Thread.currentThread().isInterrupted()) {
				Node neighbour = nodes.get(neighbourId);
				ConnectedNodePair pair = new ConnectedNodePair(node, neighbour);
				float distance = nodeDistance + data.measure(pair);
				NodeDistance current = distances.get(neighbour.getId());
				if (distance < current.getDistance()) {
					NodeDistance neww = new NodeDistance(neighbourId, nodeId, distance);
					LOG.debug("Updated distance for node {} to {}", neighbour, neww);
					distances.put(neighbourId, neww);
				}
			}
		}
		return evaluatedNodes;
	}

	@Override
	public Type getType() {
		return Type.DIJKSTRA;
	}

}
