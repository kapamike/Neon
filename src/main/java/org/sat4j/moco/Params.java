/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2012 Artois University and CNRS
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Contributors:
 *   CRIL - initial API and implementation
 *   Miguel Terra-Neves, Ines Lynce and Vasco Manquinho - MOCO solver
 *******************************************************************************/
package org.sat4j.moco;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.sat4j.moco.util.Real;

/**
 * Class used to store the solver's configuration.
 * @author Miguel Terra-Neves
 */
// TODO: implement setters?
public class Params {

    /**
     * Default verbosity level.
     */
    private static final String DEFAULT_VERB = "0";
    
    /**
     * Default decimal scale.
     */
    private static final String DEFAULT_SCALE = "5";
    
    /**
     * Default literal-weight ratio for stratification.
     */
    private static final String DEFAULT_LWR = "15.0";
    
    /**
     * Default maximum conflicts allowed before partition merging in stratified algorithms.
     */
    private static final String DEFAULT_PMC = "200000";
    
    /**
     * Default trivial threshold (number of trivially solved partitions in a row before merging the
     * remaining ones) for stratified algorithms.
     */
    private static final String DEFAULT_TT = "20";

    /**
     * Default algorithm to be used.
     */
    private static final String DEFAULT_ALG = "MCSE";

    /**
     * Default value for the crossover rate.
     */
    private static final String DEFAULT_CR = "0.8";

    /**
     * Default value for the mutation rate.
     */
    private static final String DEFAULT_MR = "0.05";

    /**
     * Default time for the algorithm.
     */
    private static final String DEFAULT_TIMEOUT = "100";

    /**
     * Default population size.
     */
    private static final String DEFAULT_POP_SIZE = "100";

    /**
     * Default population size.
     */
    private static final String DEFAULT_NEIGHBORHOOD_SIZE = "0.1";

    /**
     * Default population size.
     */
    private static final String DEFAULT_ETA = "0.01";

    /**
     * Default population size.
     */
    private static final String DEFAULT_DELTA = "0.9";

    /**
     * Default value for the mutation rate.
     */
    private static final String DEFAULT_SMR = "0.0";

    /**
     * Default value for the improvement relaxation rate.
     */
    private static final String DEFAULT_SIR = "0.4";

    /**
     * Default value for the maximum conflicts on smart mutation.
     */
    private static final String DEFAULT_MAX_CONFLICTS = "50000";

    /**
     * Default value for the maximum conflicts on smart improvement.
     */
    private static final String DEFAULT_IMPROVEMENT_MAX_CONFLICTS = "50000";


    private static final String DEFAULT_SEED = "-1";
    
    /**
     * Builds an {@link Options} object with the solver's configuration parameters to be used for parsing
     * command line options.
     * @return An {@link Options} object to be used by the command line interface.
     */
    public static Options buildOpts() {
        Options o = new Options();
        o.addOption("v", "verbosity", true,
                    "Set the verbosity level (from 0 to 3). Default is " + DEFAULT_VERB + ".");
        o.addOption("t", "timeout", true, "Set the time limit in seconds. 20 seconds limit by default.");
        o.addOption("sa", "suppress-assign", false, "Suppress assignment output.");
        o.addOption("ds", "decimal-scale", true,
                    "Set the maximum scale (number of digits to the right of the decimal point) for real numbers." +
                    "Default is " + DEFAULT_SCALE + ".");
        o.addOption("s", "stratify", false, "Disable stratification in MCS based algorithm.");
        o.addOption("lwr", "lit-weight-ratio", true,
                    "Set the literal-weight ratio for stratification. Default is " + DEFAULT_LWR + ".");
        o.addOption("pmc", "part-max-confl", true,
                    "Set the maximum conflicts allowed before merging with the next partition in stratified " +
                    "algorithms. Default is " + DEFAULT_PMC + ".");
        o.addOption("tt", "trivial-thres", true,
                    "Set the trivial threshold for stratified algorithms (number of trivially solved partitions " +
                    "in a row before merging the remaining ones). Default is " + DEFAULT_TT + ".");
        o.addOption("alg", "algorithm", true, "Set the algorithm to be used (between MCSE, NSGAII and MOEAD). Default is "
                    + DEFAULT_ALG + ".");
        o.addOption("cr", "crossover-rate", true,
                "Set the crossover rate for the stochastic algorithms. Default value is " + DEFAULT_CR + ".");
        o.addOption("mr", "mutation-rate", true,
                "Set the mutation rate for the stochastic algorithms. Default value is " + DEFAULT_MR + ".");
        o.addOption("ps", "population-size", true,
                "Set size of the population for the stochastic algorithms. Default value is " + DEFAULT_POP_SIZE + ".");
        o.addOption("ns", "neighborhood-size", true,
                "Set size of the neighborhood for the MOEAD algorithm. Default value is " + DEFAULT_NEIGHBORHOOD_SIZE + ".");
        o.addOption("eta", "eta-moead", true,
                "Set the maximum number os spots in the population that an offspring can replace in the MOEAD algorithm, " +
                    "given as a percentage of the population size. Default value is " + DEFAULT_ETA + ".");
        o.addOption("delta", "delta-moead", true,
                "The probability of mating with an individual from the neighborhood versus the entire population, " +
                    "in the MOEAD algorithm. Default value is " + DEFAULT_DELTA + ".");
        o.addOption("smr", "smart-mutation-rate", true,
                "Set the smart mutation rate for the stochastic algorithms. Default value is " + DEFAULT_SMR + ".");
        o.addOption("sir", "smart-improvement-relax-rate", true,
                "Set the smart improvement relaxation rate for the stochastic algorithms. Default value is " + DEFAULT_SIR + ".");
        o.addOption("si", "stucture-improvement", false,
                "Enables the usage of structure improvements for the stochastic algorithms.");
        o.addOption("mc", "max-conflicts", true,
                "Set the amount of max conflicts to be used in the smart mutation operator.");
        o.addOption("imc", "improvement-max-conflicts", true,
                "Set the amount of max conflicts to be used in the smart improvement operator.");
        o.addOption("eso", "evolutionary-smart-operators", false,
                "Enables the usage of evolutionary smart operators stochastic algorithms.");
        o.addOption("up", "unitary-propagation", false,
                "Disables the usage of unitary propagation on stochastic algorithms.");
        o.addOption("seed", "set-seed", true,
                "Selects the seed value for the PRNG class.");
        o.addOption("um", "uniform-mutation", false,
                "Enables the uniform mutation operator from the moea framework, instead of the single point mutation.");
        return o;
    }
    
    /**
     * Stores the verbosity level of the solver.
     */
    private int verb = 0;
    
    /**
     * Stores the maximum time, in seconds, allowed for the solver to run.
     * If less than 0, then no time limit is imposed.
     */
    private int timeout = 100;
    
    /**
     * Stores if assignment logging should be suppressed.
     * If true, then assignments should not be logged.
     */
    private boolean suppress_assign = false;
    
    /**
     * Stores the maximum scale (number of digits to the right of the decimal point) to be considered for
     * {@link Real} operations.
     */
    private int scale = 5;
    
    /**
     * Stores if stratification is to be enabled for Pareto-MCS based algorithms.
     */
    private boolean stratify = true;
    
    /**
     * Stores the literal-weight ratio to be used when partitioning objectives for stratification in
     * Pareto-MCS based algorithms.
     */
    private double lwr = 15.0;
    
    /**
     * Stores the maximum conflicts allowed in stratified algorithms before merging a partition with the next
     * one.
     */
    private int pmc = 200000;
    
    /**
     * Stores the trivial threshold (number of trivially solved partitions in a row before merging the
     * remaining ones) for stratified algorithms.
     */
    private int tt = 20;

    /**
     * Stores the name of the algorithm to be used.
     */
    private String alg = "MCSE";

    /**
     * Stores the crossover ratio to be used with stochastic algorithms.
     */
    private double cr = 0.8;

    /**
     * Stores the mutation ratio to be used with stochastic algorithms.
     */
    private double mr = 0.05;

    /**
     * Stores the size of each population in the stochastic algorithms.
     */
    private int ps = 100;

    /**
     * Stores the neighborhood size for the MOEAD algorithm.
     */
    private double ns = 0.1;

    /**
     * Stores the ETA value for the MOEAD algorithm.
     */
    private double eta = 0.01;

    /**
     * Stores the delta value for the MOEAD algorithm.
     */
    private double delta = 0.9;

    /**
     * Stores the mutation ratio to be used with stochastic algorithms.
     */
    private double smr = 0.0;

    /**
     * Stores the mutation ratio to be used with stochastic algorithms.
     */
    private double sir = 0.4;

    /**
     * Stores if structure improvements should be used.
     */
    private boolean structure_improv = false;

    /**
     * Stores the maximum of conflicts to be used by the smart mutation operator.
     */
    private int max_conflicts = 50000;

    /**
     * Stores the maximum of conflicts to be used by the smart improvement operator.
     */
    private int improve_max_conflicts = 50000;

    /**
     * Stores if evolutionary smart operators should be used.
     */
    private boolean evolutionary_smart = false;

    /**
     * Stores if unitary propagation should be used.
     */
    private boolean unitary_propagation = true;

    /**
     * Stores the seed value for the PRNG class.
     */
    private int seed = -1;

    /**
     * Stores uniform mutation is to be used
     */
    private boolean um = false;

    /**
     * Creates a parameters object with default configuration options.
     */
    public Params() {
        this.verb = Integer.parseInt(DEFAULT_VERB);
        this.scale = Integer.parseInt(DEFAULT_SCALE);
        this.lwr = Double.parseDouble(DEFAULT_LWR);
        this.pmc = Integer.parseInt(DEFAULT_PMC);
        this.tt = Integer.parseInt(DEFAULT_TT);
        this.alg = DEFAULT_ALG;
        this.cr = Double.parseDouble(DEFAULT_CR);
        this.mr = Double.parseDouble(DEFAULT_MR);
        this.timeout = Integer.parseInt(DEFAULT_TIMEOUT);
        this.ps = Integer.parseInt(DEFAULT_POP_SIZE);
        this.ns = Double.parseDouble(DEFAULT_NEIGHBORHOOD_SIZE);
        this.delta = Double.parseDouble(DEFAULT_DELTA);
        this.eta = Double.parseDouble(DEFAULT_ETA);
        this.smr = Double.parseDouble(DEFAULT_SMR);
        this.sir = Double.parseDouble(DEFAULT_SIR);
        this.max_conflicts = Integer.parseInt(DEFAULT_MAX_CONFLICTS);
        this.improve_max_conflicts = Integer.parseInt(DEFAULT_IMPROVEMENT_MAX_CONFLICTS);
        this.seed = Integer.parseInt(DEFAULT_SEED);
    }
    
    /**
     * Creates a parameters object with the configuration options provided in the command line.
     * @param cl The command line object.
     */
    public Params(CommandLine cl) {
        this.verb = Integer.parseInt(cl.getOptionValue("v", DEFAULT_VERB));
        this.suppress_assign = cl.hasOption("sa");
        this.scale = Integer.parseInt(cl.getOptionValue("ds", DEFAULT_SCALE));
        this.stratify = !cl.hasOption("s");
        this.lwr = Double.parseDouble(cl.getOptionValue("lwr", DEFAULT_LWR));
        this.pmc = Integer.parseInt(cl.getOptionValue("pmc", DEFAULT_PMC));
        this.tt = Integer.parseInt(cl.getOptionValue("tt", DEFAULT_TT));
        this.alg = cl.getOptionValue("alg", DEFAULT_ALG);
        this.cr = Double.parseDouble(cl.getOptionValue("cr", DEFAULT_CR));
        this.mr = Double.parseDouble(cl.getOptionValue("mr", DEFAULT_MR));
        this.timeout = Integer.parseInt(cl.getOptionValue("t", DEFAULT_TIMEOUT));
        this.ps = Integer.parseInt(cl.getOptionValue("ps", DEFAULT_POP_SIZE));
        this.ns = Double.parseDouble(cl.getOptionValue("ns", DEFAULT_NEIGHBORHOOD_SIZE));
        this.delta = Double.parseDouble(cl.getOptionValue("delta", DEFAULT_DELTA));
        this.eta = Double.parseDouble(cl.getOptionValue("eta", DEFAULT_ETA));
        this.smr = Double.parseDouble(cl.getOptionValue("smr", DEFAULT_SMR));
        this.sir = Double.parseDouble(cl.getOptionValue("sir", DEFAULT_SIR));
        this.structure_improv = cl.hasOption("si");
        this.max_conflicts = Integer.parseInt(cl.getOptionValue("mc", DEFAULT_MAX_CONFLICTS));
        this.improve_max_conflicts = Integer.parseInt(cl.getOptionValue("imc", DEFAULT_IMPROVEMENT_MAX_CONFLICTS));
        this.evolutionary_smart = cl.hasOption("eso");
        this.unitary_propagation = !cl.hasOption("up");
        this.seed = Integer.parseInt(cl.getOptionValue("seed", DEFAULT_SEED));
        this.um = cl.hasOption("um");
    }
    
    /**
     * Retrieves the desired verbosity level.
     * @return The verbosity level.
     */
    public int getVerbosity() { return this.verb; }
    
    /**
     * Checks if a time limit was provided by the user.
     * @return True if a time limit was provided, false otherwise.
     */
    public boolean hasTimeout() { return this.timeout >= 0; }
    
    /**
     * If a time limit was provided by the user, retrieves that time limit in seconds.
     * @return The time limit.
     */
    public int getTimeout() { return this.timeout; }
    
    /**
     * Checks if assignment logging should be suppressed.
     * @return True if assignments are to be suppressed, false otherwise.
     */
    public boolean getSuppressAssignments() { return this.suppress_assign; }
    
    /**
     * Retrieves the maximum scale (number of digits to the right of the decimal point) to be considered for
     * {@link Real} operations.
     * @return The scale.
     */
    public int getScale() { return this.scale; }
    
    /**
     * Checks if stratification is to be enabled for Pareto-MCS based algorithms.
     * @return True if stratification is enabled, false otherwise.
     */
    public boolean getStratify() { return this.stratify; }
    
    /**
     * Retrieves the literal-weight ratio to be used by the stratified Pareto-MCS algorithm in the
     * partitioning process.
     * @return The literal-weight ratio.
     */
    public double getLWR() { return this.lwr; }
    
    /**
     * Retrieves the maximum number of conflicts to be allowed in stratified algorithms before merging a
     * partition with the next one.
     * @return The maximum conflicts per partition.
     */
    public int getPartMaxConfl() { return this.pmc; }
    
    /**
     * Retrieves the trivial threshold (number of trivially solved partitions in a row before merging the
     * remaining ones) for stratified algorithms.
     * @return The trivial threshold.
     */
    public int getTrivialThres() { return this.tt; }

    /**
     * Retrieves the algorithm to be used.
     * @return The name of the algorithm.
     */
    public String getAlgorithm() { return this.alg; }

    /**
     * Retrieves the mutation rate to be used in the stochastic algorithm.
     * @return The mutation rate.
     */
    public double getMR() { return this.mr; }

    /**
     * Retrieves the crossover rate to be used by the stochastic algorithm.
     * @return The crossover rate.
     */
    public double getCR() { return this.cr; }

    /**size of each population in the stochastic algorithms.
     * @return The population size.
     */
    public int getPS() { return this.ps; }

    /**size of the neighborhood in MOEAD.
     * @return The neighborhood size.
     */
    public double getNS() { return this.ns; }

    /**maximum number of spots in the population that an offspring can replace.
     * @return The eta.
     */
    public double getEta() { return this.eta; }

    /**the probability of mating with an individual from the neighborhood.
     * @return The delta.
     */
    public double getDelta() { return this.delta; }

    /**
     * Retrieves the smart mutation rate to be used in the stochastic algorithm.
     * @return The smart mutation rate.
     */
    public double getSMR() { return this.smr; }

    /**
     * Retrieves the smart improvement rate to be used in the stochastic algorithm.
     * @return The smart improvement rate.
     */
    public double getSIR() { return this.sir; }

    /**
     * Checks if structure improvements should be used.
     * @return True if improvements are to be suppressed, false otherwise.
     */
    public boolean getStructureImprovements() { return this.structure_improv; }

    /**maximum of conflicts for each smart mutation call.
     * @return The max conflicts.
     */
    public int getMC() { return this.max_conflicts; }

    /**maximum of conflicts for each smart improvement call.
     * @return The improvement max conflicts.
     */
    public int getIMC() { return this.improve_max_conflicts; }

    /**
     * Checks if evolutionary smart operators should be used.
     * @return True if evolution is to be suppressed, false otherwise.
     */
    public boolean getEvolutionarySmart() { return this.evolutionary_smart; }

    /**
     * Checks if unitary propagation should be used.
     * @return True if unitary propagation should be used, false otherwise.
     */
    public boolean getUnitaryPropagation() { return this.unitary_propagation; }

    /**
     * Gets value of the seed to be used in smart operators
     * @return Value of the seed
     */
    public int getSeed() { return this.seed; }

    /**
     * Checks if the uniform mutation operator should be used.
     * @return True if it should be used, false otherwise.
     */
    public boolean getUM(){ return this.um; }
}
