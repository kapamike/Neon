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

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.sat4j.moco.algorithm.MOCOAlgorithm;
import org.sat4j.moco.algorithm.MOEADFactory;
import org.sat4j.moco.algorithm.NSGAIIFactory;
import org.sat4j.moco.algorithm.ParetoMCS;
import org.sat4j.moco.analysis.Result;
import org.sat4j.moco.parsing.OPBReader;
import org.sat4j.moco.problem.Instance;
import org.sat4j.moco.util.Clock;
import org.sat4j.moco.util.IOUtils;
import org.sat4j.moco.util.Log;
import org.sat4j.moco.util.Real;

/**
 * MOCO solver's main class.
 * @author Miguel Terra-Neves
 */
public class Launcher {

    /**
     * MOCO solver's shutdown handler class. The solver should log the current result before exiting.
     * @author Miguel Terra-Neves
     */
    private static class ShutdownHandler extends Thread {
        
        /**
         * The algorithm being executed.
         */
        private MOCOAlgorithm solver;
        
        /**
         * Creates an instance of the shutdown handler.
         * @param solver The algorithm being executed.
         */
        public ShutdownHandler(MOCOAlgorithm solver) {
            super();
            this.solver = solver;
        }
        
        /**
         * Logs the current result obtained by the solver.
         */
        public void run() {
            logResult(this.solver.getResult());
        }
        
    }
    
    /**
     * Adds a shutdown hook that logs the best result found by a given algorithm.
     * @param solver The algorithm being executed.
     */
    private static void setShutdownHandler(MOCOAlgorithm solver) {
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler(solver));
    }
    
    /**
     * Prints a help message to standard output.
     * @param options An object that represents the application's options.
     */
    private static void printHelpMessage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("org.sat4j.moco [options] file", options);
    }
    
    /**
     * Logs the result (e.g. nondominated solutions) produced by the MOCO solver's execution.
     * @param r The result.
     */
    private static void logResult(Result r) {
        if (r.isParetoFront() && r.nSolutions() > 0) Log.optimum();
        else if (r.isParetoFront()) Log.unsat();
        else if (r.nSolutions() > 0) Log.sat();
        else Log.unknown();
        for (int i = 0; i < r.nSolutions(); ++i) {
            Log.assignment(r.getAssignment(i));
            Log.costs(r.getCosts(i));
        }
        Log.successful(r.getProblem());
    }
    
    /**
     * Reads a MOCO instance from a file given as a command line argument.
     * @param cl The command line object that contains the input file.
     * @return The instance.
     * @throws IOException if an error occurs parsing the instance.
     */
    // TODO: support DIMACS format
    // TODO: support MOBP format
    private static Instance readMOCO(CommandLine cl) throws IOException {
        OPBReader reader = IOUtils.mkFileReader(cl.getArgs()[0]);
        Instance moco = reader.readMOCO();
        reader.close();
        Log.comment(1, ":parse-time " + Clock.instance().getElapsed());
        return moco;
    }
    
    /**
     * The solver's entry point.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        Clock.instance().reset();
        Options options = Params.buildOpts();
        CommandLineParser cl_parser = new DefaultParser();
        CommandLine cl;
        try {
            cl = cl_parser.parse(options, args);
            if (cl.getArgs().length < 1) {
                System.out.println("Illegal number of arguments");
                printHelpMessage(options);
                return;
            }
            Params params = new Params(cl);
            Log.updtParams(params);
            Real.updtParams(params);
            Clock.instance().updtParams(params);
            Instance moco = readMOCO(cl);
            Log.comment(0, "Version from 9 December");
            if (params.getAlgorithm().equals("NSGAII")){
                NSGAIIFactory solver = new NSGAIIFactory(moco, params.getStructureImprovements(), params.getUnitaryPropagation());
                setShutdownHandler(solver);
                solver.setParams(params);
                solver.nsgaiiPrintConfiguration(params);
                solver.solve();
            }
            else if (params.getAlgorithm().equals("MOEAD")){
                MOEADFactory solver = new MOEADFactory(moco, params.getStructureImprovements(), params.getUnitaryPropagation());
                setShutdownHandler(solver);
                solver.setParams(params);
                solver.moeadPrintConfiguration(params);
                solver.solve();
            }
            else if (params.getAlgorithm().equals("MCSE")){
                ParetoMCS solver = new ParetoMCS(moco);
                setShutdownHandler(solver);
                solver.updtParams(params);
                solver.solve();
            }
            else {
                printHelpMessage(options);
            }
        }
        catch (ParseException e) {
            printHelpMessage(options);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            printHelpMessage(options);
        }
        catch (IOException e) {
            System.out.println("PARSER ERROR!");
            e.printStackTrace();
        }
    }

}
