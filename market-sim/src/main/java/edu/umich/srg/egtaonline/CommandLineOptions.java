package edu.umich.srg.egtaonline;

import com.github.rvesse.airline.Arguments;
import com.github.rvesse.airline.Command;
import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.Option;

import javax.inject.Inject;

@Command(name = "egta", description = "TODO Fill out this description.") // TODO
public class CommandLineOptions {

  @Inject
  public HelpOption help;

  @Option(name = {"-s", "--spec"}, description = "Path to simulation spec. (default: stdin)")
  public String simspec = "-";

  @Option(name = {"-o", "--obs"},
      description = "Path to observaton file location. (default: stdout)")
  public String observations = "-";

  @Option(name = {"-l", "--log"}, description = "Path to log file location. (default: stderr)")
  public String logs = "-";

  @Option(name = {"-v"}, description = "Verbosity of logging. (default: 0)")
  public int verbosity = 0;

  @Option(name = {"--egta"},
      description = "Flag that this is running on egta. Disables logging among other things. (default: false)")
  public boolean egta = false;

  @Arguments(title = {"numobs"}, arity = 1,
      description = "The number of observations to gather from the simulation spec. If multiple simulation specs are passed in, this many observations will be sampled for each. (default: 1)")
  public int numObs = 1;

}
