import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;

/**
 * Created by andreas on 13.10.14.
 */
public class CurrentPlot extends TimePlot {
  public CurrentPlot(CurrentTrace trace, BoundedTimeValue currentTime, final ObservableValue<Double> maxCurrent) {
    super(currentTime);

    if (trace == null)
      throw new IllegalArgumentException();
    if (maxCurrent == null)
      throw new IllegalArgumentException();

    this.trace = trace;

    recalculateResolution(maxCurrent.getValue());

    maxCurrent.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        recalculateResolution(maxCurrent.getValue());
        repaint();
      }
    });
  }

  public CurrentTrace getTrace() {
    return trace;
  }
  @Override
  public void mouseMoved(MouseEvent mouseEvent) {
    super.mouseMoved(mouseEvent);

    long time = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime;
    Double interpolation = trace.interpolationAt(time);

    if (interpolation != null) {
      setToolTipText(String.format("%.3f mA", interpolation));
    } else {
      setToolTipText("unknown");
    }
  }
  @Override
  public int getHeight() {
    return HEIGHT;
  }


  // PROTECTED

  @Override
  protected void paintEvents(Graphics g) {
    Long firstTime = trace.getFirstTime();
    Long lastTime = trace.getLastTime();

    // mark time area where something happened
    if (firstTime != null && lastTime != null) {
      fillIntervall(g, firstTime, lastTime, Color.WHITE, true);
    }

    final double timePerPixel = TimePlot.timePerPixel.getValue();

    g.setColor(Color.BLACK);

    long end = Math.min(currentTime + (long) (timePerPixel * getWidth()), TimePlot.endTime);

    try {
//      long tA = System.nanoTime();
      SampleIterator<Double> ms = trace.getMeasurementsCovering(currentTime, end, (long) (timePerPixel));
//      logger.debug("prep: " + (System.nanoTime() - tA));
      paintIterating(g, ms);

//      Samples<Double> samples = trace.getSamplesCovering(currentTime, end, (long) (timePerPixel / 2d));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  // PRIVATE

  private static final int HEIGHT = 40;
  private static final int MAX_PIXEL_DISTANCE_OF_CONNECTED_POINTS = 20;
  private static final Logger logger = Logger.getLogger(CurrentPlot.class);

  private final CurrentTrace trace;

  private double milliAmpsPerPixel;

  private void paintIterating(Graphics g, SampleIterator<Double> ms) {
    long loadTime = 0;
    long drawTime = 0;

    long tA = System.nanoTime();
    if (ms.next()) {
      loadTime += System.nanoTime() - tA;
      final double timePerPixel = TimePlot.timePerPixel.getValue();
      final double pixelPerTime = 1d / timePerPixel;

      long oldTime = ms.time();
      int oldX = (int) ((oldTime - currentTime) / timePerPixel);
      int oldY = (int) (HEIGHT - ms.value() / milliAmpsPerPixel);

      tA = System.nanoTime();
      while (ms.next()) {
        loadTime += System.nanoTime() - tA;
        tA = System.nanoTime();
        long newTime = ms.time();
        int newX = (int) ((newTime - currentTime) / timePerPixel);
        int newY = (int) (HEIGHT - ms.value() / milliAmpsPerPixel);
        if ((newTime - oldTime) * pixelPerTime <= MAX_PIXEL_DISTANCE_OF_CONNECTED_POINTS)
          g.drawLine(oldX, oldY, newX, newY);
        else {
          g.drawRect(oldX, oldY - 1, 1, 1);
          g.drawRect(newX, newY - 1, 1, 1);
        }
        oldTime = newTime;
        oldX = newX;
        oldY = newY;
        drawTime += System.nanoTime() - tA;
        tA = System.nanoTime();
      }
    }
    logger.debug("load: " + loadTime);
    logger.debug("draw: " + drawTime);
  }
  private void recalculateResolution(double maxCurrent) {
    milliAmpsPerPixel = maxCurrent / HEIGHT;
  }
}
