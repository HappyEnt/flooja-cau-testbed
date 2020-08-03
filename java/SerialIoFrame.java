import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * Created by andreas on 19.11.14.
 */
public class SerialIoFrame extends JFrame {
  public SerialIoFrame(final SerialEvent [] serialEvents, long testStartTime) {
    super("Serial Output");

    if (serialEvents == null)
      throw new IllegalArgumentException();

    this.serialEvents = serialEvents;
    this.testStartTime = testStartTime;

    // serial output UI
    SerialTableModel serialTableModel = new SerialTableModel();
    serialTable = new JTable(serialTableModel);
    serialTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    logFilter = new TableRowSorter<TableModel>(serialTableModel);
    for (int i = 0, n = serialTableModel.getColumnCount(); i < n; i++) {
      logFilter.setSortable(i, false);
    }
    serialTable.setRowSorter(logFilter);
    serialTable.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          int row = serialTable.getSelectedRow();
          if (row != -1) {
            int iModel = serialTable.convertRowIndexToModel(row);
            long time = serialEvents[iModel].getTimestampTenNanoseconds();
            fireTimeSelected(time);
          }
        }
      }
    });

    final JScrollPane serialScroll = new JScrollPane(
        serialTable,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(serialScroll, BorderLayout.CENTER);


    // bottom pane (filter, filter invert checkbox)

    final JPanel serialBottomPane = new JPanel(new BorderLayout());

    serialBottomPane.add(new JLabel("Filter:"), BorderLayout.WEST);

    serialFilterInput = new JTextField();
    serialFilterInput.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        updateFilter();
      }
    });
    isFilterInverted.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        updateFilter();
      }
    });
    serialBottomPane.add(serialFilterInput, BorderLayout.CENTER);

    final JCheckBox filterInvertCheckbox = new JCheckBox("inverted");
    filterInvertCheckbox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        isFilterInverted.setValue(filterInvertCheckbox.isSelected());
      }
    });
    serialBottomPane.add(filterInvertCheckbox, BorderLayout.EAST);


    add(serialBottomPane, BorderLayout.SOUTH);
  }

  public void fireTimeSelected(long time) {
    for (TimeSelectionListener listener : timeSelectionListeners)
      listener.timeSelected(time);
  }
  public void addTimeSelectionListener(TimeSelectionListener listener) {
    timeSelectionListeners.add(listener);
  }
  public boolean removeTimeSelectionListener(TimeSelectionListener listener) {
    if (listener == null)
      throw new IllegalArgumentException();

    return timeSelectionListeners.remove(listener);
  }

  public void moveToTime(long time) {
    int iSerialEvent = Arrays.binarySearch(serialEvents, new SerialEvent(time, -1, SerialEvent.SerialDirection.NONE, ""), FlocklabEvent.ORDER);
    if (iSerialEvent < 0)
      iSerialEvent = -(iSerialEvent + 1);
    int viewRow = -1;
    if (iSerialEvent < serialEvents.length) {
      // select first row starting at iSerialEvent which is visible
      while (iSerialEvent < serialEvents.length) {
        viewRow = serialTable.convertRowIndexToView(iSerialEvent++);
        if (viewRow != -1) {
          serialTable.setRowSelectionInterval(viewRow, viewRow);
          break;
        }
      }

    } else {
      viewRow = serialTable.getRowCount() - 1;
      serialTable.clearSelection();
    }
    serialTable.scrollRectToVisible(new Rectangle(serialTable.getCellRect(viewRow, 0, true)));
  }


  // PRIVATE

  private static final int COLUMN_TIME = 0;
  private static final int COLUMN_OVERSEER = 1;
  private static final int COLUMN_DIRECTION = 2;
  private static final int COLUMN_OUTPUT = 3;
  private static final String[] COL_NAMES = {"Time", "overseer ID", "direction", "output"};

  private final ObservableValue<Boolean> isFilterInverted = new ObservableValue<Boolean>(false);
  private final Set<TimeSelectionListener> timeSelectionListeners = new HashSet<TimeSelectionListener>();
  private final JTextField serialFilterInput;
  private final TableRowSorter<TableModel> logFilter;
  private final long testStartTime;
  private final SerialEvent[] serialEvents;
  private final JTable serialTable;

  private void updateFilter() {
    RowFilter<Object, Object> filter;
    if (!serialFilterInput.getText().equals("")) {
      final RowFilter<Object, Object> nonInvertedFilter =
          // TODO was: COLUMN_NODE, COLUMN_OVERSEER ok here?
          RowFilter.regexFilter(serialFilterInput.getText(), COLUMN_OUTPUT, COLUMN_OVERSEER);

      if (isFilterInverted.getValue()) {
        filter = new RowFilter<Object, Object>() {
          @Override
          public boolean include(Entry<?, ?> entry) {
            return !nonInvertedFilter.include(entry);
          }
        };
      } else {
        filter = nonInvertedFilter;
      }
    } else {
      filter = null;
    }

    logFilter.setRowFilter(filter);
  }

  private class SerialTableModel extends AbstractTableModel {
    @Override
    public int getRowCount() {
      return serialEvents.length;
    }
    @Override
    public int getColumnCount() {
      return 5;
    }
    @Override
    public String getColumnName(int column) {
      return COL_NAMES[column];
    }
    @Override
    public Object getValueAt(int i, int j) {
      SerialEvent e = serialEvents[i];
      switch (j) {
        case COLUMN_TIME:
          TimeValue t = new TimeValue(e.getTimestampTenNanoseconds() - testStartTime);
          return String.format(
              "%d:%02d.%03d%03d",
              t.minutes(),
              t.remainingSeconds(),
              t.remainingMilliSeconds(),
              t.remainingMicroSeconds());
        case COLUMN_OVERSEER:
          return e.getOverseerID();
        case COLUMN_DIRECTION:
          return e.getDirection();
        case COLUMN_OUTPUT:
          return e.getOutput();
        default:
          throw new IllegalArgumentException("No column " + j);
      }
    }
  }
}
