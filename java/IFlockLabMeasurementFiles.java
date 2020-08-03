import java.io.File;

/**
 * Created by andreas on 08.10.14.
 */

/**
 * Interface to the result files of a flocklab measurement.
 */
public interface IFlockLabMeasurementFiles {
  File getSerialOutputFile();
  File getGpioTraceFile();
}
