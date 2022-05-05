import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Module;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.Utilities;

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
public class App {

  /**
   * Display usage info - what are the command line args, examples, etc.
   */
  public static void usage() {
    System.out.println("Usage: run_synthea [options] [state [city]]");
    System.out.println("Options: [-s seed] [-cs clinicianSeed] [-p populationSize]");
    System.out.println("         [-r referenceDate as YYYYMMDD]");
    System.out.println("         [-e endDate as YYYYMMDD]");
    System.out.println("         [-g gender] [-a minAge-maxAge]");
    System.out.println("         [-o overflowPopulation]");
    System.out.println("         [-m moduleFileWildcardList]");
    System.out.println("         [-c localConfigFilePath]");
    System.out.println("         [-d localModulesDirPath]");
    System.out.println("         [-i initialPopulationSnapshotPath]");
    System.out.println("         [-u updatedPopulationSnapshotPath]");
    System.out.println("         [-t updateTimePeriodInDays]");
    System.out.println("         [-f fixedRecordPath]");
    System.out.println("         [-k keepMatchingPatientsPath]");
    System.out.println("         [--config* value]");
    System.out.println("          * any setting from src/main/resources/synthea.properties");
    System.out.println("Examples:");
    System.out.println("run_synthea Massachusetts");
    System.out.println("run_synthea Alaska Juneau");
    System.out.println("run_synthea -s 12345");
    System.out.println("run_synthea -p 1000");
    System.out.println("run_synthea -s 987 Washington Seattle");
    System.out.println("run_synthea -s 21 -p 100 Utah \"Salt Lake City\"");
    System.out.println("run_synthea -g M -a 60-65");
    System.out.println("run_synthea -p 10 --exporter.fhir.export true");
    System.out.println("run_synthea -m moduleFilename" + File.pathSeparator + "anotherModule"
        + File.pathSeparator + "module*");
    System.out.println("run_synthea --exporter.baseDirectory \"./output_tx/\" Texas");
  }

  /**
   * Run Synthea generation.
   * @param args None. See documentation on configuration.
   * @throws Exception On errors.
   */
  public static void main(String[] args) throws Exception {
    Generator.GeneratorOptions options = new Generator.GeneratorOptions();

    boolean validArgs = true;
    boolean overrideFutureDateError = false;
    if (args != null && args.length > 0) {
      try {
        Queue<String> argsQ = new LinkedList<String>(Arrays.asList(args));

        while (!argsQ.isEmpty()) {
          String currArg = argsQ.poll();

          if (currArg.equalsIgnoreCase("-h")) {
            usage();
            System.exit(0);
          } else if (currArg.equalsIgnoreCase("-s")) {
            String value = argsQ.poll();
            options.seed = Long.parseLong(value);
          } else if (currArg.equalsIgnoreCase("-cs")) {
            String value = argsQ.poll();
            options.clinicianSeed = Long.parseLong(value);
          } else if (currArg.equalsIgnoreCase("-r")) {
            String value = argsQ.poll();
            // note that Y = "week year" and y = "year" per the formatting guidelines
            // and D = "day in year" and d = "day in month", so what we actually want is yyyyMMdd
            // see: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            options.referenceTime = format.parse(value).getTime();
          } else if (currArg.equalsIgnoreCase("-e")) {
            if (currArg.equals("-E")) {
              overrideFutureDateError = true;
            }
            String value = argsQ.poll();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            options.endTime = format.parse(value).getTime();
          } else if (currArg.equalsIgnoreCase("-p")) {
            String value = argsQ.poll();
            options.population = Integer.parseInt(value);
          } else if (currArg.equalsIgnoreCase("-o")) {
            String value = argsQ.poll();
            options.overflow = Boolean.parseBoolean(value);
          } else if (currArg.equalsIgnoreCase("-g")) {
            String value = argsQ.poll();
            if (value.equals("M") || value.equals("F")) {
              options.gender = value;
            } else {
              throw new Exception("Legal values for gender are 'M' or 'F'.");
            }
          } else if (currArg.equalsIgnoreCase("-a")) {
            String value = argsQ.poll();
            if (value.contains("-")) {
              String[] values = value.split("-");
              options.ageSpecified = true;
              options.minAge = Integer.parseInt(values[0]);
              options.maxAge = Integer.parseInt(values[1]);
            } else {
              throw new Exception("Age format: minAge-maxAge. E.g. 60-65.");
            }
          } else if (currArg.equalsIgnoreCase("-m")) {
            String value = argsQ.poll();
            String[] values = value.split(File.pathSeparator);
            options.enabledModules = Arrays.asList(values);
          } else if (currArg.equalsIgnoreCase("-c")) {
            String value = argsQ.poll();
            File configFile = new File(value);
            Config.load(configFile);
            // Any options that are automatically set by reading the configuration
            // file during options initialization need to be reset here.
            options.population = Config.getAsInteger("generate.default_population", 1);
            options.threadPoolSize = Config.getAsInteger("generate.thread_pool_size", -1);
          } else if (currArg.equalsIgnoreCase("-d")) {
            String value = argsQ.poll();
            File localModuleDir = new File(value);
            if (localModuleDir.exists() && localModuleDir.isDirectory()) {
              Module.addModules(localModuleDir);
            } else {
              throw new FileNotFoundException(String.format(
                      "Specified local module directory (%s) is not a directory",
                      localModuleDir.getAbsolutePath()));
            }
          } else if (currArg.equalsIgnoreCase("-u")) {
            String value = argsQ.poll();
            failIfPhysiologyEnabled(currArg);
            File file = new File(value);
            try {
              if (file.createNewFile()) {
                options.updatedPopulationSnapshotPath = file;
              } else {
                throw new IOException("File exists");
              }
            } catch (IOException ex) {
              throw new IOException(String.format("Unable to create snapshot file (%s): %s",
                      file.getAbsolutePath(), ex.getMessage()));
            }
          } else if (currArg.equalsIgnoreCase("-i")) {
            String value = argsQ.poll();
            failIfPhysiologyEnabled(currArg);
            File file = new File(value);
            try {
              if (file.exists() && file.canRead()) {
                options.initialPopulationSnapshotPath = file;
              } else {
                throw new IOException("File does not exist or is not readable");
              }
            } catch (IOException ex) {
              throw new IOException(String.format("Unable to load snapshot file (%s): %s",
                      file.getAbsolutePath(), ex.getMessage()));
            }
          } else if (currArg.startsWith("-t")) {
            String value = argsQ.poll();
            try {
              options.daysToTravelForward = Integer.parseInt(value);
              if (options.daysToTravelForward < 1) {
                throw new NumberFormatException("Must be a positive, non-zero integer");
              }
            } catch (NumberFormatException ex) {
              throw new IllegalArgumentException(
                      String.format(
                              "Error in specified updateTimePeriodInDays (%s): %s",
                              value,
                              ex.getMessage()));
            }
          } else if (currArg.equalsIgnoreCase("-f")) {
            String value = argsQ.poll();
            File fixedRecordPath = new File(value);
            if (fixedRecordPath.exists()) {
              options.fixedRecordPath = fixedRecordPath;
            } else {
              throw new FileNotFoundException(String.format(
                  "Specified fixed record file (%s) does not exist", value));
            }
          } else if (currArg.equals("-k")) {
            String value = argsQ.poll();
            File keepPatientsModule = new File(value);
            if (keepPatientsModule.exists()) {
              options.keepPatientsModulePath = keepPatientsModule;
            } else {
              throw new FileNotFoundException(String.format(
                  "Specified keep-patients file (%s) does not exist", value));
            }
          } else if (currArg.startsWith("--")) {
            String configSetting;
            String value;
            // accept either:
            // --config.setting=value
            // --config.setting value
            if (currArg.contains("=")) {
              String[] parts = currArg.split("=", 2);
              configSetting = parts[0].substring(2);
              value = parts[1];
            } else {
              configSetting = currArg.substring(2);
              value = argsQ.poll();
            }

            Config.set(configSetting, value);
          } else if (options.state == null) {
            options.state = currArg;
          } else {
            // assume it must be the city
            options.city = currArg;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        usage();
        validArgs = false;
      }
    }

    if (validArgs && validateConfig(options, overrideFutureDateError)) {
      Generator generator = new Generator(options);
      generator.run();
    }
  }

  private static boolean validateConfig(Generator.GeneratorOptions options,
          boolean overrideFutureDateError) {
    boolean valid = true;
    if (Config.getAsBoolean("exporter.fhir.transaction_bundle")
            && ! Config.getAsBoolean("exporter.practitioner.fhir.export")
            && ! Config.getAsBoolean("exporter.hospital.fhir.export")) {
      System.out.println("Warning: Synthea is configured to export FHIR transaction bundles "
              + "for generated patients but not to export the practitioners and organizations "
              + "that the patient bundle entries will reference. "
              + "See https://github.com/synthetichealth/synthea/wiki/FHIR-Transaction-Bundles "
              + "for more information."
      );
    }
    if (!overrideFutureDateError) {
      int yearsOfHistory = Config.getAsInteger("exporter.years_of_history");
      long millisToEndTime = options.endTime - System.currentTimeMillis();
      if (millisToEndTime > Utilities.convertTime("years", yearsOfHistory)) {
        System.out.println("Error: the specified end time is further in the future than the "
                + "number of years of export history. The first exported events will be in the "
                + "future. Consider adjusting the values of the '-e endDate' command line switch "
                + "and the 'exporter.years_of_history' configuration file entry.\n"
                + "You may override this error by using '-E endDate' instead of '-e EndDate'."
        );
        valid = false;
      }
    }
    return valid;
  }

  private static void failIfPhysiologyEnabled(String arg) {
    if (Boolean.valueOf(Config.get("physiology.generators.enabled", "false"))) {
      String errString = String.format(
              "The %s command line switch %s - %s",
              arg,
              "cannot be used when physiology generators are enabled",
              "set configuration option physiology.generators.enabled=false to use"
      );
      throw new IllegalArgumentException(errString);
    }
  }
}
