import java.io.IOException;
import java.util.*;

/**
 * Created by andreas on 04.11.14.
 */
public class MapCurrentTrace implements CurrentTrace {
  private final NavigableMap<Long, Double> map;
  private final int nodeId;

  public MapCurrentTrace(int nodeId, NavigableMap<Long, Double> map) {
    if (map == null)
      throw new IllegalArgumentException();

    this.nodeId = nodeId;
    this.map = map;
  }

  @Override
  public int getMoteId() {
    return nodeId;
  }

  @Override
  public Long getFirstTime() {
    return map.firstKey();
  }

  @Override
  public Long getLastTime() {
    return map.lastKey();
  }

  @Override
  public Double interpolationAt(long time) {
    Map.Entry<Long, Double> lower = map.floorEntry(time);
    Map.Entry<Long, Double> upper = map.ceilingEntry(time);
    if (lower != null && upper != null) {
      return lower.getValue() + (double) (time - lower.getKey()) / (double) (upper.getKey() - lower.getKey()) * (upper.getValue() - lower.getValue());
    } else {
      return null;
    }
  }

  @Override
  public Double averageIn(long timeA, long timeB) {
    long startTime = timeA >= timeB ? timeB : timeA;
    long endTime = timeA >= timeB ? timeA : timeB;

    Collection<Double> sampleValues = map.subMap(startTime, true, endTime, true).values();
    final int nSamples = sampleValues.size();

    if (nSamples > 0) {
      double currentSum = 0;
      for (double v : sampleValues) {
        currentSum += v;
      }
      return currentSum / nSamples;
    } else
      return null;
  }

  @Override
  public SampleIterator<Double> getMeasurementsCovering(long startTime, long endTime, long maxDeltaT) throws IOException {
    Long lower = map.floorKey(startTime);
    Long upper = map.ceilingKey(endTime);
    lower = lower != null ? lower : startTime;
    upper = upper != null ? upper : endTime;

    return new MapSampleIterator<Double>(map.subMap(lower, true, upper, true));
  }
}
