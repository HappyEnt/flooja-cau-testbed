import java.io.IOException;

/**
 * Created by andreas on 30.10.14.
 */
public interface CurrentTrace {
  public int getMoteId();
  public Long getFirstTime();
  public Long getLastTime();
  public Double interpolationAt(long time);

  /**
   *
   * @param timeA
   * @param timeB
   * @return the average of all samples between the two times (inclusive); null if there are none
   */
  public Double averageIn(long timeA, long timeB);

  SampleIterator<Double> getMeasurementsCovering(long startTime, long endTime, long maxDeltaT) throws IOException;
}
