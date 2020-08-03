import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;


public class GpioPlot extends TimePlot {

  private final NavigableMap<Long, Boolean> trace;
  public static JComponent eventDestination;
  private final String pinName;
  private final ObservableValue<String> pinDescription;

  public GpioPlot(NavigableMap<Long, Boolean> trace, String pinName, BoundedTimeValue currentTime, ObservableValue<String> pinDescription) {
    super(currentTime);

    if (pinDescription == null)
      throw new IllegalArgumentException();
    if (trace == null)
      throw new IllegalArgumentException();

    this.trace = trace;
    this.pinName = pinName;
    this.pinDescription = pinDescription;
    setLayout(null);
  }

  @Override
  public int getHeight() {
    return 7;
  }

  @Override
  protected void paintEvents(Graphics g) {
    final double timePerPixel = TimePlot.timePerPixel.getValue();
    final long plotEndTime = currentTime + (long) (getWidth() * timePerPixel);

    Long firstTimeEver = null;
    try {
      firstTimeEver = trace.firstKey();
    } catch (NoSuchElementException ignored) {}
    if (firstTimeEver != null)
      fillIntervall(g, firstTimeEver, TimePlot.endTime, Color.WHITE, true);

    Long timeOfLastEventBeforeCurrentTime = trace.floorKey(currentTime);
    NavigableMap<Long, Boolean> events = trace.subMap(
        timeOfLastEventBeforeCurrentTime == null ?
            currentTime :
            timeOfLastEventBeforeCurrentTime,
        true,
        plotEndTime, true);

    long blockStart = -1;
    List<Long> additionalUpEvents = new ArrayList<Long>();
    for (Map.Entry<Long, Boolean> e : events.entrySet()) {
      if (!e.getValue()) {
        if (blockStart != -1) {
          drawBlock(g, blockStart, e.getKey());
          blockStart = -1;
        } else {
          drawArrow(g, e.getKey(), false);
        }
      } else {
        if (blockStart == -1)
          blockStart = e.getKey();
        else
          additionalUpEvents.add(e.getKey());
      }
    }
    if (blockStart != -1)
      drawBlock(g, blockStart, TimePlot.endTime);
    for (long t : additionalUpEvents)
      drawArrow(g, t, true);
  }

  private static final double ARROW_FRACTION = 0.5d;

  private void drawArrow(Graphics g, long time, boolean up) {
    g.setColor(Color.BLACK);
    int x = (int) ((time - currentTime) / timePerPixel.getValue());
    g.drawLine(x, 0, x, getHeight());
    if (up) {
      g.drawLine(x, 0, x + (int) (getHeight() * ARROW_FRACTION), (int) (getHeight() * ARROW_FRACTION));
      g.drawLine(x, 0, x - (int) (getHeight() * ARROW_FRACTION), (int) (getHeight() * ARROW_FRACTION));
    } else {
      g.drawLine(x, getHeight(), x + (int) (getHeight() * ARROW_FRACTION), (int)(getHeight() * (1 - ARROW_FRACTION)));
      g.drawLine(x, getHeight(), x - (int) (getHeight() * ARROW_FRACTION), (int) (getHeight() * (1 - ARROW_FRACTION)));
    }
  }

  private void drawBlock(Graphics g, long blockStart, long blockEnd) {
    fillIntervall(g, blockStart, blockEnd, getGpioPinColor(this.pinName), true);
  }

  @Override
  public void mouseMoved(MouseEvent mouseEvent) {
    super.mouseMoved(mouseEvent);

    long mouseTime = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime;

    // take earlier events as long as the value does not change
    Map.Entry<Long, Boolean> lastEvent = trace.floorEntry(mouseTime);
    Map.Entry<Long, Boolean> preLastEvent = lastEvent;
    while (preLastEvent != null && preLastEvent.getValue() == lastEvent.getValue()) {
      Map.Entry<Long, Boolean> temp = preLastEvent;
      preLastEvent = trace.lowerEntry(lastEvent.getKey());
      lastEvent = temp;
    }
    Map.Entry<Long, Boolean> nextEvent = trace.higherEntry(mouseTime);
    // take next event different from the last one
    if (lastEvent != null) {
      while (nextEvent != null && nextEvent.getValue() == lastEvent.getValue()) {
        nextEvent = trace.higherEntry(nextEvent.getKey());
      }
    }

    String toolTipText = pinName;
    toolTipText += " : ";
    Long start = null;
    Long end = null;
    if (preLastEvent != null) {
      start = lastEvent.getKey();
    }

    if (nextEvent != null) {
      end = nextEvent.getKey();
    }

    String endTimeString = "...";
    if (end != null) {
      end -= startTime;
      TimeValue t = new TimeValue(end);
      endTimeString = String.format("%d:%02d.%03d%03d", t.minutes(), t.remainingSeconds(), t.remainingMilliSeconds(), t.remainingMicroSeconds());
    }
    String startTimeString = "...";
    if (start != null) {
      start -= startTime;
      TimeValue t = new TimeValue(start);
      startTimeString = String.format("%d:%02d.%03d%03d", t.minutes(), t.remainingSeconds(), t.remainingMilliSeconds(), t.remainingMicroSeconds());
    }


    StringBuilder b = new StringBuilder();
    b.append("<html>")
     .append(pinName);

    String pinDescr = pinDescription.getValue();
    if (pinDescr != null && !pinDescr.equals(""))
      b.append(" (" + pinDescr + ")");

    b.append(" : ");

    if (lastEvent == null) {
      b.append("unknown");
      if (nextEvent != null) {
        b.append(" then ");
        if (nextEvent.getValue())
          b.append("high");
        else
          b.append("low");
      }
    } else if (lastEvent.getValue())
      b.append("high");
    else
      b.append("low");

    b.append("<br>")
     .append(startTimeString)
     .append(" -> ")
     .append(endTimeString);
    if (start != null && end != null)
      b.append(" (")
       .append(FloatUtils.convert((end - start) * 1e-8, 2))
       .append("s)");
    b.append("</html>");

    setToolTipText(b.toString());
  }

  private static final Map<String, Color> gpioColors;
  static {
    gpioColors = new HashMap<String, Color>();
    gpioColors.put("LED1", new Color(242, 99, 99)); // RED
    gpioColors.put("LED2", new Color(98, 204, 98)); // GREEN
    gpioColors.put("LED3", new Color(242, 242, 80)); // yellow
    gpioColors.put("INT1", new Color(102, 102, 237)); // blue
    gpioColors.put("INT2", new Color(142, 83, 201)); // purple
  }

  private static Color getGpioPinColor(String pinName) {
    if (pinName == null)
      throw new IllegalArgumentException();

    Color c = gpioColors.get(pinName);
    if (c == null)
      return Color.BLACK;
    else
      return c;
  }
}
