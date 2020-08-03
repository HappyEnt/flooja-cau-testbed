import java.io.Reader;
import java.util.*;

/**
 * Created by andreas on 08.10.14.
 */
public class GpioLoader extends FlocklabCsvLoader {
  private final Map<Integer, Map<String, NavigableMap<Long, Boolean>>> eventTable;

  public GpioLoader(Reader reader) {
    super(reader);
    eventTable = new HashMap<Integer, Map<String, NavigableMap<Long, Boolean>>>();
  }

  private NavigableMap<Long, Boolean> nodePinEvents(int nodeId, String pinName) {
    Map<String, NavigableMap<Long, Boolean>> nodeMap = eventTable.get(nodeId);
    NavigableMap<Long, Boolean> events;

    // TODO add the nodeMap to the eventTable in the processRow method
    if (nodeMap == null) {
      nodeMap = new HashMap<String, NavigableMap<Long, Boolean>>();
      eventTable.put(nodeId, nodeMap);
      events = null;
    } else {
      events = nodeMap.get(pinName);
    }

    if (events == null) {
      events = new TreeMap<Long, Boolean>();
      nodeMap.put(pinName, events);
    }

    return events;
  }

  @Override
  protected void processRow(long time, int overseerID, String rest) {
    String[] restParts = rest.split(",", 2);
    String pinID = restParts[0].trim();

    nodePinEvents(overseerID, pinID).put(time, restParts[1].trim().equals("1"));
  }

  public Map<Integer, Map<String, NavigableMap<Long, Boolean>>> getEvents() {
    return eventTable;
  }

  public final Set<String> getOccuringPinNames() {
    Set<String> pinNames = new HashSet<String>();
    for (Map<String, NavigableMap<Long, Boolean>> nodeEvents : eventTable.values()) {
      pinNames.addAll(nodeEvents.keySet());
    }
    return Collections.unmodifiableSet(pinNames);
  }
}
