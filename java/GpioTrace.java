/**
 * Created by andreas on 08.10.14.
 */
public interface GpioTrace {
  long getStartTime();
  GpioEvent[] getEventsCoveringTimespan(long start, long end);
}
