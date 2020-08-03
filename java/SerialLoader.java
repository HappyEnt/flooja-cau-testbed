import java.io.*;
import java.util.*;

/**
 * Created by andreas on 01.10.14.
 */
public class SerialLoader extends FlocklabCsvLoader {
  private final TreeMap<TimeAndOrder, SerialEvent> serialEvents;
  private int i=0;

  public SerialLoader(Reader reader) {
    super(reader);
    serialEvents = new TreeMap<TimeAndOrder, SerialEvent>();
  }

  private class TimeAndOrder implements Comparable {
    public long time;
    public int order;

    public TimeAndOrder (long time, int order) {
      this.time = time;
      this.order= order;
    }

    public int compareTo(Object o) {
      long diff = this.time - ((TimeAndOrder)o).time;
      if (diff < 0)
        return -1;
      if (diff > 0)
        return 1;
      return this.order - ((TimeAndOrder)o).order;
    }
  }

  @Override
  protected void processRow(long time, int overseerID, String rest) {
    String[] restParts = rest.split(",",2);

    // direction of serial communication
    SerialEvent.SerialDirection direction = SerialEvent.SerialDirection.valueOf(restParts[0].trim());

    serialEvents.put(
        new TimeAndOrder(time, i),
        new SerialEvent(
            time,
            overseerID,
            direction,
            restParts[1]));
    i++;
  }

  public SerialEvent[] getEvents() {
    return serialEvents.values().toArray(new SerialEvent[serialEvents.size()]);
  }
}

