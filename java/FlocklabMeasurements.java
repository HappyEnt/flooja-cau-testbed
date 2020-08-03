import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by andreas on 08.10.14.
 */

/**
 * Class holding references to all the measurement files of one flocklab measurement.
 */
public class FlocklabMeasurements implements IFlockLabMeasurementFiles {
  private File gpioTrace;
  private File serial;

  public FlocklabMeasurements(File folder) {
    if (folder == null)
      throw new IllegalArgumentException("Folder can't be null");
    if (!folder.isDirectory())
      throw new IllegalArgumentException("Folder must be a directory");

    serial = new File(folder, "serial.csv");
    if (!serial.exists() || !serial.isFile())
      serial = null;

    gpioTrace = new File(folder, "gpiotraces.csv");
    if (!gpioTrace.exists() || !gpioTrace.isFile())
      gpioTrace = null;
  }

  public boolean hasSerialOut() {
    return serial != null;
  }
  @Override
  public File getSerialOutputFile() {
    return serial;
  }
  public boolean hasGpioTrace() {
    return gpioTrace != null;
  }
  @Override
  public File getGpioTraceFile() {
    return gpioTrace;
  }
}
