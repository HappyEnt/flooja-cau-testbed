import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Created by andreas on 13.10.14.
 */
public abstract class TimePlot extends JPanel implements MouseInputListener, MouseWheelListener {
  private static final ComponentListener widthListener = new ComponentListener() {
    @Override
    public void componentResized(ComponentEvent componentEvent) {
      Component line = (Component) (componentEvent.getSource());
      if (line.isVisible()) {
        displayedWidth.setValue(line.getWidth());
      }
    }

    @Override
    public void componentMoved(ComponentEvent componentEvent) {
    }

    @Override
    public void componentShown(ComponentEvent componentEvent) {
    }

    @Override
    public void componentHidden(ComponentEvent componentEvent) {
    }
  };
  public static TimePerPixel timePerPixel;
  public static long startTime;
  public static long endTime;
  private static Set<TimePlot> timePlots = new HashSet<TimePlot>();
  private static ObservableValue<Integer> displayedWidth = new ObservableValue<Integer>();
  private static Set<DisplayedWidthListener> displayedWidthListeners = new HashSet<DisplayedWidthListener>();
  private static Set<MouseTimeListener> mouseTimeListeners = new HashSet<MouseTimeListener>();
  private final ArrayList<TimeZoomListener> timeZoomListeners = new ArrayList<TimeZoomListener>();
  protected long currentTime;

  public static void clearTimelines() {
    timePlots.clear();
    displayedWidthListeners.clear();
    mouseTimeListeners.clear();
  }

  public TimePlot(final ObservableValue<Long> currentTime) {
    super();

    if (currentTime == null)
      throw new IllegalArgumentException();
    this.currentTime = currentTime.getValue();

    currentTime.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        TimePlot.this.currentTime = currentTime.getValue();
        repaint();
      }
    });
    timePlots.add(this);
    addMouseListener(this);
    addMouseMotionListener(this);
    addMouseWheelListener(this);
    addComponentListener(widthListener);
  }

  public static void addDisplayedWidthListener(DisplayedWidthListener listener) {
    if (listener == null)
      throw new IllegalArgumentException();
    displayedWidthListeners.add(listener);
  }

  public static int getDisplayedWidth() {
    return displayedWidth.getValue();
  }

  static {
    displayedWidth.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        final int newWidth = displayedWidth.getValue();
        for (DisplayedWidthListener l : displayedWidthListeners)
          l.widthChanged(newWidth);
      }
    });
  }

  public static Set<TimePlot> getTimePlots() {
    return Collections.unmodifiableSet(timePlots);
  }

  public static void addMouseTimeListener(MouseTimeListener listener) {
    if (listener == null)
      throw new IllegalArgumentException();
    mouseTimeListeners.add(listener);
  }

  protected final void fillIntervall(Graphics g, long start, long end, Color color, boolean atLeast1Wide) {
    Graphics2D g2d = (Graphics2D) g;
    long lowerTimeLimit = currentTime;
    long upperTimeLimit = currentTime + (long) (getWidth() * timePerPixel.getValue());
    if (start <= upperTimeLimit && end >= lowerTimeLimit && start <= end) {
      long startT = Math.max(lowerTimeLimit, start);
      int startX = (int) ((startT - currentTime) / timePerPixel.getValue());
      int length = (int) ((Math.min(end, upperTimeLimit) - startT) / timePerPixel.getValue());
      if (atLeast1Wide)
        length = Math.max(length, 1);

      g2d.setColor(color);
      g2d.fillRect(startX, 0, length, getHeight());
    }
  }

  public abstract int getHeight();

  @Override
  protected void paintComponent(Graphics g) {
    this.setOpaque(false);
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;

    // fillIntervall(g2d, TimePlot.startTime, TimePlot.endTime, new Color(245, 245, 245), true);
    paintEvents(g2d);
    // repaint();
  }

  protected abstract void paintEvents(Graphics g);

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(super.getPreferredSize().width, this.getHeight());
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(super.getMinimumSize().width, this.getHeight());
  }

  @Override
  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, this.getHeight());
  }

  @Override
  public void mouseClicked(MouseEvent mouseEvent) {
    long time = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime;
    for (MouseTimeListener l : mouseTimeListeners) {
      l.timeClicked(this, time, mouseEvent.getModifiersEx());
    }
  }

  @Override
  public void mousePressed(MouseEvent mouseEvent) {
    long time = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime;
    for (MouseTimeListener l : mouseTimeListeners) {
      l.timePressed(this, time, mouseEvent.getModifiersEx());
    }
  }

  @Override
  public void mouseReleased(MouseEvent mouseEvent) {
    long time = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime;
    for (MouseTimeListener l : mouseTimeListeners) {
      l.timeReleased(this, time, mouseEvent.getModifiersEx());
    }
  }

  @Override
  public void mouseEntered(MouseEvent mouseEvent) {

  }

  @Override
  public void mouseExited(MouseEvent mouseEvent) {

  }

  @Override
  public void mouseDragged(MouseEvent mouseEvent) {
    long time = (long) (mouseEvent.getX() * timePerPixel.getValue()) + currentTime;
    for (MouseTimeListener l : mouseTimeListeners) {
      l.timeDragged(this, time, mouseEvent.getModifiersEx());
    }
  }

  @Override
  public void mouseMoved(MouseEvent mouseEvent) {

  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
    if (mouseWheelEvent.isControlDown()) {
      long time = currentTime + (long) (mouseWheelEvent.getX() * timePerPixel.getValue());
      boolean zoomingIn = mouseWheelEvent.getPreciseWheelRotation() < 0;
      processTimeZoomEvent(new TimeZoomEvent(this, time, zoomingIn));
    }
    getParent().dispatchEvent(mouseWheelEvent);
  }

  public synchronized void addTimeZoomListener(TimeZoomListener listener) {
    if (listener == null)
      throw new IllegalArgumentException();
    timeZoomListeners.add(listener);
  }

  private void processTimeZoomEvent(TimeZoomEvent timeZoomEvent) {
    ArrayList<TimeZoomListener> notifiedListeners;
    synchronized (this) {
      notifiedListeners = (ArrayList<TimeZoomListener>) timeZoomListeners.clone();
    }
    if (timeZoomEvent.isZoomingIn()) {
      for (TimeZoomListener l : notifiedListeners) {
        l.zoomedIn(timeZoomEvent);
      }
    } else {
      for (TimeZoomListener l : notifiedListeners) {
        l.zoomedOut(timeZoomEvent);
      }
    }
  }

  public interface TimeZoomListener extends EventListener {
    void zoomedIn(TimeZoomEvent timeZoomEvent);

    void zoomedOut(TimeZoomEvent timeZoomEvent);
  }

  public class TimeZoomEvent extends EventObject {
    private final Long time;
    private final boolean zoomingIn;

    /**
     * @param source
     * @param time      if not null, the time which should be kept centered while zooming
     * @param zoomingIn
     */
    public TimeZoomEvent(Object source, Long time, boolean zoomingIn) {
      super(source);
      this.time = time;
      this.zoomingIn = zoomingIn;
    }

    public Long getTime() {
      return time;
    }

    public boolean isZoomingIn() {
      return zoomingIn;
    }
  }
}
