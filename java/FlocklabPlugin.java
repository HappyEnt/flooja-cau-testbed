/*
 * Copyright (c) 2006, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.contikios.cooja.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Timeline plugin for flocklab test result data (gpio trace and actuation, power measurements, ...)
 */
@ClassDescription("Flocklab Visualizer") /* Description shown in menu */
@PluginType(PluginType.COOJA_PLUGIN)
public class FlocklabPlugin extends VisPlugin {
  /**
   * @param gui        GUI object
   */
  public FlocklabPlugin(Cooja gui) throws IOException {
    super("Flocklab Plugin", gui);

    this.gpioTraceVisibilities = new HashMap<String, ObservableValue<Boolean>>();
    this.pinDescriptions = new HashMap<String, ObservableValue<String>>();
    this.currentVisibility = new ObservableValue<Boolean>(true);
    this.timePerPixel = new TimePerPixel(3000d);

    final AbstractAction zoomInAction = new AbstractAction("zoom in (Ctlr + wheel up)") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        timePerPixel.decrease();
      }
    };
    final AbstractAction zoomOutAction = new AbstractAction("zoom out (Ctrl + wheel down") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        timePerPixel.increase();
      }
    };

    timeLineFrame = new JFrame("FlockLab Timeline");
    timeLineFrame.setLayout(new BorderLayout());


    // set result dir
    JFileChooser measurementDirChooser = new JFileChooser();
    measurementDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int fileChooseResult = measurementDirChooser.showOpenDialog(this);
    assert fileChooseResult == JFileChooser.APPROVE_OPTION;


    // serial output
    FlocklabMeasurements measurementFiles = new FlocklabMeasurements(measurementDirChooser.getSelectedFile());

    final String charsetName = "UTF-8"; // or "ISO-8859-1"
    SerialLoader serialLoader = new SerialLoader(
        new InputStreamReader(
            new FileInputStream(measurementFiles.getSerialOutputFile()),
            charsetName));
    serialLoader.load();
    final SerialEvent[] serialEvents = serialLoader.getEvents();


    // gpioTrace
    GpioLoader gpioTraceLoader = new GpioLoader(
        new InputStreamReader(
            new FileInputStream(
                measurementFiles.getGpioTraceFile()),
            charsetName));
    gpioTraceLoader.load();
    Map<Integer, Map<String, NavigableMap<Long, Boolean>>> gpioEvents = gpioTraceLoader.getEvents();

    // timeline
    Set<Integer> ids = new HashSet<Integer>();
    ids.addAll(gpioEvents.keySet());

    long t0 = Math.min(gpioTraceLoader.getStartTime(), serialLoader.getStartTime());
    long tEnd = Math.max(gpioTraceLoader.getEndTime(), serialLoader.getEndTime());

    final long startTime = t0;
    final long endTime = tEnd;
    TimePlot.timePerPixel = this.timePerPixel;
    TimePlot.startTime = startTime;
    TimePlot.endTime = endTime;

    // info text
    Calendar c = Calendar.getInstance();
    TimeValue startTimeValue = new TimeValue(startTime);
    c.setTimeInMillis(startTimeValue.milliSeconds());
    String startDateText = String.format(
        "%04d.%02d.%02d %02d:%02d:%02d.%03d%03d",
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
        c.get(Calendar.SECOND),
        c.get(Calendar.MILLISECOND),
        startTimeValue.remainingMicroSeconds());
    TimeValue endTimeValue = new TimeValue(endTime);
    c.setTimeInMillis(endTimeValue.milliSeconds());
    String endDateText = String.format(
        "%04d.%02d.%02d %02d:%02d:%02d.%03d%03d",
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE),
        c.get(Calendar.SECOND),
        c.get(Calendar.MILLISECOND),
        endTimeValue.remainingMicroSeconds());
    TimeValue duration = new TimeValue(endTime - startTime);
    String durationText = String.format("%d:%02d:%02d.%03d%03d",
        duration.hours(),
        duration.remainingMinutes(),
        duration.remainingMicroSeconds(),
        duration.remainingMilliSeconds(),
        duration.remainingMicroSeconds());

    setLayout(new BorderLayout());
    add(
      new JLabel(String.format(
        "<html>" +
          "Showing Testbed results from:<br>" +
          "%s<br>" +
          "<br>" +
          "Start: %s<br>" +
          "End: %s<br>" +
          "Duration: %s" +
        "<html>",
        measurementDirChooser.getSelectedFile().getAbsolutePath(),
        startDateText,
        endDateText,
        durationText)),
      BorderLayout.CENTER);
    setSize(300, 200);


    //// SERIAL OUTPUT
    // TODO only construct if serial.csv is supplied
    // serialIoFrame = new SerialIoFrame(serialEvents, startTime);
    // serialIoFrame.setSize(400, 600);
    // serialIoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    // serialIoFrame.setVisible(true);


    //// TIMELINE
    JLabel timeInfoLabel = new JLabel("Start: " + startDateText + "    End: " + endDateText + "    Duration: " + durationText);
    timeLineFrame.add(timeInfoLabel, BorderLayout.PAGE_START);

    currentTime = new BoundedTimeValue(startTime, endTime, startTime);
    this.timeLinePanel = new TimeLinePanel(timePerPixel, currentTime);
    GpioPlot.eventDestination = timeLinePanel;

    // power and gpio GUI
    currentPlotFac = new CurrentPlotFactory(currentTime);

    for (int id : new TreeSet<Integer>(ids)) {
      final Box nodeBox = Box.createVerticalBox();

      // add gpio trace
      if (gpioEvents.containsKey(id)) {
        Map<String, NavigableMap<Long, Boolean>> traces = gpioEvents.get(id);
        for (String pinName : new TreeSet<String>(traces.keySet())) {
          NavigableMap<Long, Boolean> gpioTrace = traces.get(pinName);

          final GpioPlot line = new GpioPlot(gpioTrace, pinName, currentTime, getPinDescription(pinName));

          final ObservableValue<Boolean> traceVisibility = getGpioTraceVisibility(pinName);
          traceVisibility.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object o) {
              line.setVisible(traceVisibility.getValue());
            }
          });
          nodeBox.add(line);
        }
      }
      nodeBox.add(Box.createRigidArea(new Dimension(0, 2)));

      timeLinePanel.addNodeBox(id, nodeBox);
    }

    final JScrollPane timeScrollPane = new JScrollPane(
        timeLinePanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    timeLineFrame.add(timeScrollPane, BorderLayout.CENTER);

    // TODO only construct if serial.csv is supplied
    // serialIoFrame.addTimeSelectionListener(new TimeSelectionListener() {
    //   @Override
    //   public void timeSelected(long time) {
    //     timeLinePanel.moveCursorTo(time);
    //   }
    // });

    Box headerBox = Box.createHorizontalBox();
    headerBox.add(Box.createRigidArea(new Dimension(40, 0)));
    headerBox.add(new MillisecondRuler(currentTime, timePerPixel));
    timeScrollPane.setColumnHeaderView(headerBox);

    // time scroll bar stuff
    this.timeScrollBar = new JScrollBar(Adjustable.HORIZONTAL);
    timeScrollBar.setMaximum(Integer.MAX_VALUE);
    final AdjustmentListener adjustmentListener = new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
        if (!timeScrollBar.getValueIsAdjusting()) {
          long newTime = startTime + (long) (timeScrollBar.getValue() / (double) timeScrollBar.getMaximum() * (endTime - startTime));
          currentTime.setValue(newTime);
        }
      }
    };
    timeScrollBar.addAdjustmentListener(adjustmentListener);

    // adjusting time scrollbars extent value
    final ActionListener timeScrollBarAction =  new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        double maxValue = timeScrollBar.getMaximum();
        final double fractionShowed = timePerPixel.getValue() / (endTime - startTime) * TimePlot.getDisplayedWidth();
        timeScrollBar.removeAdjustmentListener(adjustmentListener);
        timeScrollBar.setValue((int) ((currentTime.getValue() - startTime) / (double) (endTime - startTime) * maxValue));
        timeScrollBar.setVisibleAmount((int) (maxValue * fractionShowed));
        timeScrollBar.setBlockIncrement(Math.max(1, (int) (maxValue * fractionShowed * 0.3)));
        timeScrollBar.setUnitIncrement(Math.max(1, (int) (maxValue * fractionShowed * 0.02)));
        timeScrollBar.addAdjustmentListener(adjustmentListener);
      }
    };
    currentTime.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        timeScrollBarAction.actionPerformed(null);
      }
    });
    TimePlot.addDisplayedWidthListener(new DisplayedWidthListener() {
      @Override
      public void widthChanged(int newWidth) {
        timeScrollBarAction.actionPerformed(null);
      }
    });
    timePerPixel.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        timeScrollBarAction.actionPerformed(null);
      }
    });

    timeLineFrame.add(timeScrollBar, BorderLayout.SOUTH);


    setPreferredSize(new Dimension(700, 300));

    // menu bar
    JMenuBar menuBar = new JMenuBar();
    timeLineFrame.setJMenuBar(menuBar);

    // events menu
    JMenu eventsMenu = new JMenu("Events");
    final JCheckBox currentCb = new JCheckBox("current", true);
    currentCb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        currentVisibility.setValue(currentCb.isSelected());
      }
    });
    currentVisibility.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        currentCb.setSelected(currentVisibility.getValue());
      }
    });
    eventsMenu.add(new AbstractAction("all") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        setAllEventVisibilities(true);
      }
    });
    eventsMenu.add(new AbstractAction("none") {

      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        setAllEventVisibilities(false);
      }
    });
    eventsMenu.add(currentCb);
    //// gpio trace
    for (final String pinName : new TreeSet<String>(gpioTraceLoader.getOccuringPinNames())) {
      final JCheckBox cb = new JCheckBox("gpio trace: " + pinName, true);
      final ObservableValue<Boolean> vis = getGpioTraceVisibility(pinName);
      cb.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
          vis.setValue(cb.isSelected());
        }
      });
      vis.addObserver(new Observer() {
        @Override
        public void update(Observable observable, Object o) {
          cb.setSelected(vis.getValue());
        }
      });
      eventsMenu.add(cb);
    }

    // nodes menu
    JMenu nodesMenu = new JMenu("Nodes");
    nodesMenu.add(new AbstractAction("all") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        for (ObservableValue<Boolean> vis : timeLinePanel.getMoteVisibilities().values()) {
          vis.setValue(true);
        }
      }
    });
    nodesMenu.add(new AbstractAction("none") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        for (ObservableValue<Boolean> vis : timeLinePanel.getMoteVisibilities().values()) {
          vis.setValue(false);
        }
      }
    });
    for (final int id : new TreeSet<Integer>(ids)) {
      final JCheckBox cb = new JCheckBox("" + id, true);
      final ObservableValue<Boolean> vis = timeLinePanel.getMoteVisibilities().get(id);
      cb.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
          vis.setValue(cb.isSelected());
        }
      });
      vis.addObserver(new Observer() {
        @Override
        public void update(Observable observable, Object o) {
          cb.setSelected(vis.getValue());
        }
      });
      nodesMenu.add(cb);
    }
    menuBar.add(nodesMenu);

    // zoom Menu
    JMenu zoomMenu = new JMenu("Zoom");
    zoomMenu.add(zoomInAction);
    zoomMenu.add(zoomOutAction);
    zoomMenu.add(new AbstractAction("Set max current") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        Double newMaxCurrent = null;
        while (true) {
          String input = JOptionPane.showInputDialog(FlocklabPlugin.this, "Set upper limit of displayed current in mA (> 0)", currentPlotFac.getMaxCurrent().getValue());
          try {
            newMaxCurrent = Double.parseDouble(input);
          } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(FlocklabPlugin.this, "Must enter a decimal value (e.g. 12.34)");
            continue;
          }
          if (newMaxCurrent <= 0) {
            JOptionPane.showMessageDialog(FlocklabPlugin.this, "Value must be > 0");
            continue;
          }
          break;
        }
        currentPlotFac.getMaxCurrent().setValue(newMaxCurrent);
      }
    });
    menuBar.add(zoomMenu);

    // pin description menu
    JMenu pinDescriptionMenu = new JMenu("GPIO-pins");
    final Set<String> pinNames = new TreeSet<String>();
    pinNames.addAll(gpioTraceLoader.getOccuringPinNames());
    for (final String pinName : pinNames) {
      pinDescriptionMenu.add(new AbstractAction(pinName) {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
          ObservableValue<String> pinDescription = getPinDescription(pinName);
          String currentDescription = pinDescription.getValue();
          if (currentDescription == null)
            currentDescription = "";
          String newDescription =
              JOptionPane.showInputDialog(FlocklabPlugin.this, "Set the description for pin: " + pinName, currentDescription);
          if (newDescription != null)
            pinDescription.setValue(newDescription);
        }
      });
    }
    menuBar.add(pinDescriptionMenu);

    // config menu
    JMenu timeLineConfigMenu = new JMenu("config");
    timeLineConfigMenu.add(new AbstractAction("save") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        int saveResult = fc.showSaveDialog(FlocklabPlugin.this);
        if (saveResult == JFileChooser.APPROVE_OPTION) {
          File file = fc.getSelectedFile();
          boolean doWrite = true;
          if (file.exists()) {
            int overwriteDecision = JOptionPane.showConfirmDialog(
                FlocklabPlugin.this,
                String.format("File '%s' already exists. Overwrite?", file.getAbsolutePath()),
                "Save timeline config",
                JOptionPane.YES_NO_OPTION);
            doWrite = overwriteDecision == JOptionPane.YES_OPTION;
          }
          if (doWrite) {
            Element root = new Element(XML_CONFIG_ROOT_NAME);
            root.addContent(getConfigXML());
            Document doc = new Document(root);
            try {
              OutputStream out = new FileOutputStream(file);

              XMLOutputter outputter = new XMLOutputter();
              outputter.setFormat(Format.getPrettyFormat());
              outputter.output(doc, out);
              out.close();
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    });
    timeLineConfigMenu.add(new AbstractAction("load") {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        int fileOpenResult = fc.showSaveDialog(FlocklabPlugin.this);
        if (fileOpenResult == JFileChooser.APPROVE_OPTION) {
          File file = fc.getSelectedFile();
          SAXBuilder builder = new SAXBuilder();
          try {
            InputStream in = new FileInputStream(file);
            Document doc = builder.build(in);
            Element root = doc.getRootElement();
            if (root.getName().equals(XML_CONFIG_ROOT_NAME))
              FlocklabPlugin.this.setConfigXML(root.getChildren(), true);
            in.close();
            return;
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          } catch (JDOMException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
          JOptionPane.showMessageDialog(FlocklabPlugin.this, "An error occured.");
        }
      }
    });
    menuBar.add(timeLineConfigMenu);


    final MouseWheelListener scrollListener = timeScrollPane.getMouseWheelListeners()[0];
    timeScrollPane.removeMouseWheelListener(scrollListener);
    timeScrollPane.addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        if (!(mouseWheelEvent.isControlDown() || mouseWheelEvent.isShiftDown() || mouseWheelEvent.isAltDown() || mouseWheelEvent.isAltGraphDown() || mouseWheelEvent.isMetaDown())) {
          scrollListener.mouseWheelMoved(mouseWheelEvent);
        }
      }
    });

    for (final TimePlot tl : TimePlot.getTimePlots()) {
      tl.addTimeZoomListener(new TimePlot.TimeZoomListener() {
        @Override
        public void zoomedIn(TimePlot.TimeZoomEvent timeZoomEvent) {
          zoomIn(timeZoomEvent.getTime());
        }

        @Override
        public void zoomedOut(TimePlot.TimeZoomEvent timeZoomEvent) {
          zoomOut(timeZoomEvent.getTime());
        }
      });
    }

    // TODO only construct if serial.csv is supplied
    // scroll serial table when clicking in timeline
    // TimePlot.addMouseTimeListener(new MouseTimeListener() {
    //   @Override
    //   public void timePressed(Object source, long time, int extendedModifiers) {
    //   }

    //   @Override
    //   public void timeDragged(Object source, long time, int extendedModifiers) {
    //   }

    //   @Override
    //   public void timeReleased(Object source, long time, int extendedModifiers) {
    //   }


    //   @Override
    //   public void timeClicked(Object source, long time, int extendedModifiers) {
    //     serialIoFrame.moveToTime(time);
    //   }
    // });

    timeLineFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    timeLineFrame.setSize(700, 200);
    timeLineFrame.setVisible(true);
  }

  @Override
  public Collection<Element> getConfigXML() {
    // measurement visibilities
    Element visibilities = new Element("visibilities");

    // gpio trace pins
    StringBuilder gpioTraceVisText = new StringBuilder();
    Iterator<Map.Entry<String, ObservableValue<Boolean>>> it = this.gpioTraceVisibilities.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, ObservableValue<Boolean>> vis = it.next();
      gpioTraceVisText.append(vis.getKey()).append(":").append(vis.getValue().getValue() ? "1" : "0");
      if (it.hasNext())
        gpioTraceVisText.append(",");
    }

    // mote visibilities
    StringBuilder moteVisText = new StringBuilder();
    Iterator<Map.Entry<Integer, ObservableValue<Boolean>>> iit = timeLinePanel.getMoteVisibilities().entrySet().iterator();
    while (iit.hasNext()) {
      Map.Entry<Integer, ObservableValue<Boolean>> vis = iit.next();
      moteVisText
          .append(vis.getKey())
          .append(":")
          .append(vis.getValue().getValue() ? "1" : "0");
      if (iit.hasNext())
        moteVisText.append(",");
    }

    ArrayList<Element> config = new ArrayList<Element>();
    config.add(
        new Element("visibilities")
            .addContent(new Element("gpiotrace").setText(gpioTraceVisText.toString()))
            .addContent(new Element("motes").setText(moteVisText.toString())));
    config.add(new Element("time").setText("" + currentTime.getValue()));
    config.add(new Element("timePerPixel").setText("" + timePerPixel.getValue()));
    Element gpioDescriptionsSection = new Element("gpioPinDescriptions");
    for (Map.Entry<String, ObservableValue<String>> pin_descr : this.pinDescriptions.entrySet()) {
      gpioDescriptionsSection.addContent(
          new Element("description")
              .setAttribute("pin", pin_descr.getKey())
              .setAttribute("text", pin_descr.getValue().getValue()));
    }
    config.add(gpioDescriptionsSection);
    return config;
  }
  @Override
  public boolean setConfigXML(Collection<Element> configXML, boolean visAvailable) {
    if (configXML == null)
      throw new IllegalArgumentException();

    for (Element e : configXML) {
      String name = e.getName();
      if ("visibilities".equals(name)) {
        for (Object vis : e.getChildren()) {
          name = ((Element) vis).getName();
          if ("current".equals(name)) {
            currentVisibility.setValue(((Element) vis).getText().equals("1"));
          } else if ("gpiotrace".equals(name)) {
            String[] pinConfigs = ((Element) vis).getText().split(",");
            for (String pinConfig : pinConfigs){
              String[] pin_vis = pinConfig.split(":");
              getGpioTraceVisibility(pin_vis[0]).setValue(pin_vis[1].equals("1"));
            }
          } else if ("motes".equals(name)) {
            String[] moteConfigs = ((Element) vis).getText().split(",");
            for (String moteConfig : moteConfigs){
              String[] id_vis = moteConfig.split(":");
              timeLinePanel.getMoteVisibility(Integer.parseInt(id_vis[0])).setValue(id_vis[1].equals("1"));
            }
          }
        }
      } else if ("time".equals(name)) {
        currentTime.setValue(Long.parseLong(e.getText()));
      } else if ("timePerPixel".equals(name)) {
        timePerPixel.setValue(Double.parseDouble(e.getText()));
      } else if ("gpioPinDescriptions".equals(name)) {
        for (Object o : e.getChildren("description")) {
          Element description = (Element) o;
          String pin = description.getAttributeValue("pin");
          if (pin != null) {
            getPinDescription(pin).setValue(description.getAttributeValue("text", ""));
          }
        }
      }
    }

    return true;
  }


  // PRIVATE

  private static final String XML_CONFIG_ROOT_NAME = "timelineConf";

  private final TimeLinePanel timeLinePanel;
  private final JScrollBar timeScrollBar;

  private final TimePerPixel timePerPixel;
  private final BoundedTimeValue currentTime;

  private final CurrentPlotFactory currentPlotFac;

  private final Map<String, ObservableValue<Boolean>> gpioTraceVisibilities;
  private final Map<String, ObservableValue<String>> pinDescriptions;
  private final ObservableValue<Boolean> currentVisibility;
  // TODO only construct if serial.csv is supplied
  // private final SerialIoFrame serialIoFrame;
  private final JFrame timeLineFrame;

  private void setAllEventVisibilities(boolean visible) {
    currentVisibility.setValue(visible);
    for (ObservableValue<Boolean> vis : gpioTraceVisibilities.values())
      vis.setValue(visible);
  }
  private ObservableValue<Boolean> getGpioTraceVisibility(String pinName) {
    if (pinName == null)
      throw new IllegalArgumentException();

    ObservableValue<Boolean> vis = gpioTraceVisibilities.get(pinName);
    if (vis == null)
      gpioTraceVisibilities.put(pinName, vis = new ObservableValue<Boolean>(true));
    return vis;
  }
  private ObservableValue<String> getPinDescription(String pinName) {
    if (pinName == null)
      throw new IllegalArgumentException();

    ObservableValue<String> description = pinDescriptions.get(pinName);
    if (description == null)
      pinDescriptions.put(pinName, description = new ObservableValue<String>(""));
    return description;
  }
  private void zoomOut(Long time) {
    if (time != null) {
      long newCurrentTime = time + (long)(timePerPixel.getNextHigherValue() / timePerPixel.getValue() * (currentTime.getValue() - time));
      timePerPixel.increase();
      currentTime.setValue(newCurrentTime);
    } else {
      timePerPixel.increase();
    }
  }
  private void zoomIn(Long time) {
    if (time != null) {
      long newCurrentTime = time + (long) (timePerPixel.getNextLowerValue() / timePerPixel.getValue() * (currentTime.getValue() - time));
      timePerPixel.decrease();
      currentTime.setValue(newCurrentTime);
    } else {
      timePerPixel.decrease();
    }
  }

  @Override
  public void closePlugin() {
    super.closePlugin();
    // TODO only construct if serial.csv is supplied
    // serialIoFrame.dispose();
    timeLineFrame.dispose();
    TimePlot.clearTimelines();
  }
}
