/**
 * Copyright 2017 LinkedIn Corporation. All rights reserved. Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.dynamometer;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.hadoop.conf.Configuration;


/**
 * Options supplied to the Client which are then passed through to the
 * ApplicationMaster.
 */
class AMOptions {

  public static final String NAMENODE_MEMORY_MB_ARG = "namenode_memory_mb";
  public static final String NAMENODE_MEMORY_MB_DEFAULT = "2048";
  public static final String NAMENODE_VCORES_ARG = "namenode_vcores";
  public static final String NAMENODE_VCORES_DEFAULT = "1";
  public static final String NAMENODE_ARGS_ARG = "namenode_args";
  public static final String DATANODE_MEMORY_MB_ARG = "datanode_memory_mb";
  public static final String DATANODE_MEMORY_MB_DEFAULT = "2048";
  public static final String DATANODE_VCORES_ARG = "datanode_vcores";
  public static final String DATANODE_VCORES_DEFAULT = "1";
  public static final String DATANODE_ARGS_ARG = "datanode_args";
  public static final String NAMENODE_METRICS_PERIOD_ARG = "namenode_metrics_period";
  public static final String NAMENODE_METRICS_PERIOD_DEFAULT = "60";
  public static final String SHELL_ENV_ARG = "shell_env";
  public static final String DATANODES_PER_CLUSTER_ARG = "datanodes_per_cluster";
  public static final String DATANODES_PER_CLUSTER_DEFAULT = "1";
  public static final String DATANODE_LAUNCH_DELAY_ARG = "datanode_launch_delay";
  public static final String DATANODE_LAUNCH_DELAY_DEFAULT = "0s";

  private final int datanodeMemoryMB;
  private final int datanodeVirtualCores;
  private final String datanodeArgs;
  private final int datanodesPerCluster;
  private final String datanodeLaunchDelay;
  private final int namenodeMemoryMB;
  private final int namenodeVirtualCores;
  private final String namenodeArgs;
  private final int namenodeMetricsPeriod;
  // Original shellEnv as passed in through arguments
  private final Map<String, String> originalShellEnv;
  // Extended shellEnv including custom environment variables
  private final Map<String, String> shellEnv;

  AMOptions(int datanodeMemoryMB, int datanodeVirtualCores, String datanodeArgs,
      int datanodesPerCluster, String datanodeLaunchDelay, int namenodeMemoryMB, int namenodeVirtualCores,
      String namenodeArgs, int namenodeMetricsPeriod, Map<String, String> shellEnv) {
    this.datanodeMemoryMB = datanodeMemoryMB;
    this.datanodeVirtualCores = datanodeVirtualCores;
    this.datanodeArgs = datanodeArgs;
    this.datanodesPerCluster = datanodesPerCluster;
    this.datanodeLaunchDelay = datanodeLaunchDelay;
    this.namenodeMemoryMB = namenodeMemoryMB;
    this.namenodeVirtualCores = namenodeVirtualCores;
    this.namenodeArgs = namenodeArgs;
    this.namenodeMetricsPeriod = namenodeMetricsPeriod;
    this.originalShellEnv = shellEnv;
    this.shellEnv = new HashMap<>(this.originalShellEnv);
    this.shellEnv.put(DynoConstants.NN_ADDITIONAL_ARGS_ENV, this.namenodeArgs);
    this.shellEnv.put(DynoConstants.DN_ADDITIONAL_ARGS_ENV, this.datanodeArgs);
    this.shellEnv.put(DynoConstants.NN_FILE_METRIC_PERIOD_ENV,
        String.valueOf(this.namenodeMetricsPeriod));
  }

  /**
   * Verifies that arguments are valid; throws IllegalArgumentException if not
   */
  void verify(int maxMemory, int maxVcores) throws IllegalArgumentException {
    Preconditions.checkArgument(datanodeMemoryMB > 0 && datanodeMemoryMB <= maxMemory,
        "datanodeMemoryMB (%s) must be between 0 and %s", datanodeMemoryMB, maxMemory);
    Preconditions.checkArgument(datanodeVirtualCores > 0 && datanodeVirtualCores <= maxVcores,
        "datanodeVirtualCores (%s) must be between 0 and %s", datanodeVirtualCores, maxVcores);
    Preconditions.checkArgument(namenodeMemoryMB > 0 && namenodeMemoryMB <= maxMemory,
        "namenodeMemoryMB (%s) must be between 0 and %s", namenodeMemoryMB, maxMemory);
    Preconditions.checkArgument(namenodeVirtualCores > 0 && namenodeVirtualCores <= maxVcores,
        "namenodeVirtualCores (%s) must be between 0 and %s", namenodeVirtualCores, maxVcores);
    Preconditions.checkArgument(datanodesPerCluster > 0, "datanodesPerCluster (%s) must be > 0", datanodesPerCluster);
  }

  /**
   * Same as {@link #verify(int, int)} but does not set a max
   */
  void verify() throws IllegalArgumentException {
    verify(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  void addToVargs(List<String> vargs) {
    vargs.add("--" + DATANODE_MEMORY_MB_ARG + " " + String.valueOf(datanodeMemoryMB));
    vargs.add("--" + DATANODE_VCORES_ARG + " " + String.valueOf(datanodeVirtualCores));
    if (!datanodeArgs.isEmpty()) {
      vargs.add("--" + DATANODE_ARGS_ARG + " \\\"" + datanodeArgs + "\\\"");
    }
    vargs.add("--" + DATANODES_PER_CLUSTER_ARG + " " + String.valueOf(datanodesPerCluster));
    vargs.add("--" + DATANODE_LAUNCH_DELAY_ARG + " " + datanodeLaunchDelay);
    vargs.add("--" + NAMENODE_MEMORY_MB_ARG + " " + String.valueOf(namenodeMemoryMB));
    vargs.add("--" + NAMENODE_VCORES_ARG + " " + String.valueOf(namenodeVirtualCores));
    if (!namenodeArgs.isEmpty()) {
      vargs.add("--" + NAMENODE_ARGS_ARG + " \\\"" + namenodeArgs + "\\\"");
    }
    vargs.add("--" + NAMENODE_METRICS_PERIOD_ARG + " " + String.valueOf(namenodeMetricsPeriod));
    for (Map.Entry<String, String> entry : originalShellEnv.entrySet()) {
      vargs.add("--" + SHELL_ENV_ARG + " " + entry.getKey() + "=" + entry.getValue());
    }
  }

  int getDataNodeMemoryMB() {
    return datanodeMemoryMB;
  }

  int getDataNodeVirtualCores() {
    return datanodeVirtualCores;
  }

  int getDataNodesPerCluster() {
    return datanodesPerCluster;
  }

  long getDataNodeLaunchDelaySec() {
    // Leverage the human-readable time parsing capabilities of Configuration
    String tmpConfKey = "___temp_config_property___";
    Configuration tmpConf = new Configuration();
    tmpConf.set(tmpConfKey, datanodeLaunchDelay);
    return tmpConf.getTimeDuration(tmpConfKey, 0, TimeUnit.SECONDS);
  }

  int getNameNodeMemoryMB() {
    return namenodeMemoryMB;
  }

  int getNameNodeVirtualCores() {
    return namenodeVirtualCores;
  }

  Map<String, String> getShellEnv() {
    return shellEnv;
  }

  /**
   * Set all of the command line options relevant to this class into
   * the passed {@link Options}.
   * @param opts Where to set the command line options.
   */
  static void setOptions(Options opts) {
    opts.addOption(SHELL_ENV_ARG, true, "Environment for shell script. Specified as env_key=env_val pairs");
    opts.addOption(NAMENODE_MEMORY_MB_ARG, true,
        "Amount of memory in MB to be requested to run the NN (default " + NAMENODE_MEMORY_MB_DEFAULT + "). " +
            "Ignored unless the NameNode is run within YARN.");
    opts.addOption(NAMENODE_VCORES_ARG, true,
        "Amount of virtual cores to be requested to run the NN (default " + NAMENODE_VCORES_DEFAULT + "). " +
            "Ignored unless the NameNode is run within YARN.");
    opts.addOption(NAMENODE_ARGS_ARG, true,
        "Additional arguments to add when starting the NameNode. Ignored unless the NameNode is run within YARN.");
    opts.addOption(NAMENODE_METRICS_PERIOD_ARG, true,
        "The period in seconds for the NameNode's metrics to be emitted to file; if <=0, " +
            "disables this functionality. Otherwise, a metrics file will be stored in the " +
            "container logs for the NameNode (default " + NAMENODE_METRICS_PERIOD_DEFAULT + ").");
    opts.addOption(DATANODE_MEMORY_MB_ARG, true,
        "Amount of memory in MB to be requested to run the DNs (default " + DATANODE_MEMORY_MB_DEFAULT + ")");
    opts.addOption(DATANODE_VCORES_ARG, true,
        "Amount of virtual cores to be requested to run the DNs (default " + DATANODE_VCORES_DEFAULT + ")");
    opts.addOption(DATANODE_ARGS_ARG, true, "Additional arguments to add when starting the DataNodes.");
    opts.addOption(DATANODES_PER_CLUSTER_ARG, true, "How many simulated DataNodes to run within each YARN container " +
        "(default " + DATANODES_PER_CLUSTER_DEFAULT + ")");
    opts.addOption(DATANODE_LAUNCH_DELAY_ARG, true, "The period over which to launch the DataNodes; this will " +
        "be used as the maximum delay and each DataNode container will be launched with some random delay less than " +
        "this value. Accepts human-readable time durations (e.g. 10s, 1m) (default " +
        DATANODE_LAUNCH_DELAY_DEFAULT + ")");

    opts.addOption("help", false, "Print usage");
  }

  /**
   * Initialize an {@code AMOptions} from a command line parser.
   * @param cliParser Where to initialize from.
   * @return A new {@code AMOptions} filled out with options from the parser.
   */
  static AMOptions initFromParser(CommandLine cliParser) {
    Map<String, String> originalShellEnv = new HashMap<>();
    if (cliParser.hasOption(SHELL_ENV_ARG)) {
      String envs[] = cliParser.getOptionValues(SHELL_ENV_ARG);
      for (String env : envs) {
        String trimmed = env.trim();
        int index = trimmed.indexOf('=');
        if (index == -1) {
          originalShellEnv.put(trimmed, "");
          continue;
        }
        String key = trimmed.substring(0, index);
        String val = "";
        if (index < (trimmed.length()-1)) {
          val = trimmed.substring(index+1);
        }
        originalShellEnv.put(key, val);
      }
    }
    return new AMOptions(
        Integer.parseInt(cliParser.getOptionValue(DATANODE_MEMORY_MB_ARG, DATANODE_MEMORY_MB_DEFAULT)),
        Integer.parseInt(cliParser.getOptionValue(DATANODE_VCORES_ARG, DATANODE_VCORES_DEFAULT)),
        cliParser.getOptionValue(DATANODE_ARGS_ARG, ""),
        Integer.parseInt(cliParser.getOptionValue(DATANODES_PER_CLUSTER_ARG, DATANODES_PER_CLUSTER_DEFAULT)),
        cliParser.getOptionValue(DATANODE_LAUNCH_DELAY_ARG, DATANODE_LAUNCH_DELAY_DEFAULT),
        Integer.parseInt(cliParser.getOptionValue(NAMENODE_MEMORY_MB_ARG, NAMENODE_MEMORY_MB_DEFAULT)),
        Integer.parseInt(cliParser.getOptionValue(NAMENODE_VCORES_ARG, NAMENODE_VCORES_DEFAULT)),
        cliParser.getOptionValue(NAMENODE_ARGS_ARG, ""),
        Integer.parseInt(cliParser.getOptionValue(NAMENODE_METRICS_PERIOD_ARG, NAMENODE_METRICS_PERIOD_DEFAULT)),
        originalShellEnv);
  }

}
