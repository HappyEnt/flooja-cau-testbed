import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;


public class GpioPlot extends TimePlot {

  private final NavigableMap<Long, Boolean> trace;
  public static JComponent eventDestination;
  private final String pinName;
  private final ObservableValue<String> pinDescription;

  private final float strokeThickness = 2.0f;
  private final BasicStroke traceStroke =
      new BasicStroke(strokeThickness,
                      BasicStroke.CAP_SQUARE,
                      BasicStroke.JOIN_ROUND
                      );

  Color lowColor = new Color(115,19,44);
  Color highColor = new Color(19,115,44);
  Color verticalLineColor = new Color(200,200,200);

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
    return 40;
  }

  @Override
  protected void paintEvents(Graphics g) {
    final double timePerPixel = TimePlot.timePerPixel.getValue();
    final long plotEndTime = currentTime + (long) (getWidth() * timePerPixel);
    // final long plotEndTime = TimePlot.endTime;

    fillIntervall(g, currentTime, TimePlot.endTime, Color.WHITE, true);

    Long timeOfLastEventBeforeCurrentTime = trace.floorKey(currentTime);
    NavigableMap<Long, Boolean> events = trace.subMap(
        timeOfLastEventBeforeCurrentTime == null ?
            currentTime :
            timeOfLastEventBeforeCurrentTime,
        true,
        plotEndTime, true);


    Graphics2D g2d = (Graphics2D) g;
    Path2D horizontalOffscreenLine = new Path2D.Double();
    int previousSignal = -1;
    double previousYPos = 0;
    long previousTimestamp = -1;
    double currentXPos = 0;


    g2d.setStroke(traceStroke);

    // -- Draw Traces --
    for (Map.Entry<Long, Boolean> e : events.entrySet()) {
        double length;
        long startT;
        int currentSignal;
        long currentTimestamp;
        double currentYPos;
        Path2D horizontalLine = new Path2D.Double();
        Path2D verticalLine   = new Path2D.Double();

        // Because inverted coordinate system. replace with screen space transformation.
        currentSignal = e.getValue() ? 0 : 1;
        currentTimestamp = e.getKey();
        currentYPos = currentSignal * getHeight();

        startT = Math.max(currentTime, previousTimestamp);
        length = (double) ((Math.min(currentTimestamp, plotEndTime) - startT) / TimePlot.timePerPixel.getValue());

        // determine initial signal state
        if (previousSignal == -1 && previousTimestamp == -1) {
            currentXPos = (int) ((startT - currentTime) / TimePlot.timePerPixel.getValue());
            previousTimestamp = currentTimestamp;
            previousSignal = currentSignal;
        } else {
            horizontalLine.moveTo(currentXPos, previousYPos);
            currentXPos += length;
            horizontalLine.lineTo(currentXPos, previousYPos);
            g2d.setColor(currentSignal == 1 ? highColor : lowColor);
            g2d.draw(horizontalLine);

            verticalLine.moveTo(currentXPos, strokeThickness*1.5);
            verticalLine.lineTo(currentXPos, getHeight() - strokeThickness*1.5);
            g2d.setColor(verticalLineColor);
            g2d.draw(verticalLine);

            // path.lineTo(currentXPos, currentSignal * getHeight());
            previousSignal = currentSignal;
            previousTimestamp = currentTimestamp;
        }

        previousYPos = previousSignal * getHeight() + 0.5*strokeThickness * (previousSignal == 1 ? -1 : 1);

        // used to draw line from last element to end of timeline
        horizontalOffscreenLine.moveTo(currentXPos, previousYPos);
    }


    // previous loop has executed at least once, => initial point for path is set via moveTo
    if(previousSignal != -1) {
        horizontalOffscreenLine.lineTo(getWidth(), previousSignal * getHeight() + 0.5 * strokeThickness * (previousSignal == 1 ? -1 : 1));
        g2d.setColor(previousSignal == 1 ? lowColor : highColor);
        g2d.draw(horizontalOffscreenLine);
    }


  }

  private static final double ARROW_FRACTION = 0.5d;

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
    gpioColors.put("1", new Color(242, 99, 99)); // RED
    gpioColors.put("2", new Color(98, 204, 98)); // GREEN
    gpioColors.put("3", new Color(242, 242, 80)); // yellow
    gpioColors.put("4", new Color(102, 102, 237)); // blue
    gpioColors.put("5", new Color(142, 83, 201)); // purple
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
