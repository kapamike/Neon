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
package org.sat4j.moco.analysis;

import java.util.Collection;
import java.util.Iterator;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.Settings;
import org.moeaframework.core.Solution;
import org.moeaframework.util.ReferenceSetMerger;
import org.sat4j.moco.problem.Instance;
import org.sat4j.moco.util.Log;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.annotation.processing.SupportedSourceVersion;

// TODO: docstrings
class ResultNormalizer {
    
    private class NormalizedMOCOProblem extends MOCOProblem {

        private int n_removed_objs = 0;
        
        NormalizedMOCOProblem(Instance instance, int n_removed_objs) {
            super(instance);
            this.n_removed_objs = n_removed_objs;
        }
        
        @Override
        public int getNumberOfObjectives() { return super.getNumberOfObjectives() - this.n_removed_objs; }
        
        @Override
        public boolean[] getAssignment(Solution sol) {
            throw new RuntimeException("Unexpected getAssignment call on normalized MOCO problem");
        }
        
        @Override
        public void evaluate(Solution sol) {
            throw new RuntimeException("Unexpected evaluate call on normalized MOCO problem");
        }
        
    }
    
    private NormalizedMOCOProblem norm_problem = null;
    
    private Multimap<String, Result> norm_dataset = ArrayListMultimap.create();
    
    private ReferenceSetMerger merger = new ReferenceSetMerger();

    public ResultNormalizer(Instance m, Multimap<String, Result> dataset) {
        // Compute minimum and maximum objective values
        double[] min_obj_vals = new double[m.nObjs()], max_obj_vals = new double[m.nObjs()];
        boolean first = true;
        for (Iterator<String> it = dataset.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            Collection<Result> rs = dataset.get(key);
            for (Iterator<Result> r_it = rs.iterator(); r_it.hasNext();) {
                for (Iterator<Solution> s_it = r_it.next().getSolutions().iterator(); s_it.hasNext();) {
                    Solution s = s_it.next();
                    for (int i = 0; i < m.nObjs(); ++i) {
                        double obj_val = s.getObjective(i);
                        if (first) {
                            min_obj_vals[i] = obj_val;
                            max_obj_vals[i] = obj_val;
                        }
                        else {
                            min_obj_vals[i] = Math.min(obj_val, min_obj_vals[i]);
                            max_obj_vals[i] = Math.max(obj_val, max_obj_vals[i]);
                        }
                    }
                    first = false;
                }
            }
        }
        // Build normalized MOCO problem object for analysis
        int n_empty_range = 0;
        for (int i = 0; i < m.nObjs(); ++i) {
            if (max_obj_vals[i] - min_obj_vals[i] < Settings.EPS) {
                n_empty_range++;
            }
        }
        this.norm_problem = new NormalizedMOCOProblem(m, n_empty_range);
        // Build normalized dataset and reference set factory
        // TODO: very similar to min and max objective value computation; refactor
        int key_count = 0;
        for (Iterator<String> it = dataset.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            Collection<Result> rs = dataset.get(key);
            for (Iterator<Result> r_it = rs.iterator(); r_it.hasNext();) {
                Result norm_result = new Result(this.norm_problem);
                for (Iterator<Solution> s_it = r_it.next().getSolutions().iterator(); s_it.hasNext();) {
                    Solution s = s_it.next(), norm_s = this.norm_problem.newCostVec();
                    String str = "New point ";
                    for (int i = 0, j = 0; i < m.nObjs(); ++i) {
                        if (max_obj_vals[i] - min_obj_vals[i] >= Settings.EPS) {
                            assert(j <= this.norm_problem.getNumberOfObjectives());
                            double range_sz = max_obj_vals[i] - min_obj_vals[i];
                            str += ((s.getObjective(i) - min_obj_vals[i]) / range_sz) + " ";
                            norm_s.setObjective(j, (s.getObjective(i) - min_obj_vals[i]) / range_sz);
                            ++j;
                        }
                    }
                    System.out.println(str);
                    norm_result.addSolution(norm_s);
                }
                this.norm_dataset.put(key, norm_result);
                this.merger.add(Integer.toString(key_count++), norm_result.getSolutions());
            }
        }
        Population p = getMergedSet();
        //System.out.println("size " + p.size() + " objectives " + this.norm_problem.getNumberOfObjectives());
        for (int j = 0; j < p.size(); ++j) {
            String str = "points ";
            for (int i = 0; i < this.norm_problem.getNumberOfObjectives(); ++i) {
                str += p.get(j).getObjective(i) + " ";
            }
            System.out.println(str);
        }
    }
    
    public MOCOProblem getNormalizedMOCOProblem() { return this.norm_problem; }
    
    // TODO: returned map should be read-only
    public Multimap<String, Result> getNormalizedDataset() { return this.norm_dataset; }
    
    /**
     * Retrieves the merged normalized reference set, as a MOEA framework {@link Population}, produced by
     * the {@link ReferenceSetMerger}.
     * @return The reference set.
     */
    private Population getMergedSet() { 
        return new NondominatedPopulation(this.merger.getCombinedPopulation());
    }
    
    /**
     * Computes the ideal or nadir point of the MOCO instance based on the current reference set.
     * @param nadir True (false) if the nadir (ideal) point is to be computed.
     * @return A cost vector that corresponds to the requested point.
     */
    private Solution getPoint(boolean nadir) {
        Population p = getMergedSet();
        if (p.size() == 0) { return null; }
        Solution s = this.norm_problem.newCostVec();
        /**System.out.println("size " + p.size() + " objectives " + this.norm_problem.getNumberOfObjectives());
        for (int j = 0; j < p.size(); ++j) {
            String str = "points ";
            for (int i = 0; i < this.norm_problem.getNumberOfObjectives(); ++i) {
                str += p.get(j).getObjective(i) + " ";
            }
            System.out.println(str);
        }*/
        for (int i = 0; i < this.norm_problem.getNumberOfObjectives(); ++i) {
            double val = p.get(0).getObjective(i);
            for (int j = 1; j < p.size(); ++j) {
                double j_val = p.get(j).getObjective(i);
                val = nadir ? Math.max(val, j_val) : Math.min(val, j_val);
            }
            s.setObjective(i, val);
        }
        return s;
    }
    
    /**
     * Computes the nadir point based of the MOCO instance based on the current reference set.
     * @return The nadir point.
     */
    // TODO: multiple calls can become inefficient; use caching?
    public Solution getNadirPoint() {
        Solution s = getPoint(true);
        String line = "nadir: ";
        for(int i = 0; i < s.getNumberOfObjectives(); i++){
            line += s.getObjective(i) + " ";
        }
        System.out.println(line);
        return s;
    }
    
    /**
     * Computes the ideal point of the MOCO instance based on the current reference set.
     * @return The ideal point.
     */
    // TODO: multiple calls can become inefficient; use caching?
    public Solution getIdealPoint() {
        Solution s = getPoint(false);
        String line = "ideal: ";
        for(int i = 0; i < s.getNumberOfObjectives(); i++){
            line += s.getObjective(i) + " ";
        }
        System.out.println(line);
        return s;
    }
    
    /**
     * Computes a reference point of the MOCO instance, for hypervolume computation, based on the current
     * reference set.
     * @return The reference point.
     */
    public Solution getReferencePoint() {
        Solution ideal = getIdealPoint(), nadir = getNadirPoint(), s = nadir.copy();
        double r = 1.0 + 1.0 / Math.max(1.0, getMergedSet().size()-1);
        for (int i = 0; i < s.getNumberOfObjectives(); ++i) {
            double val = s.getObjective(i);
            // MOEA Framework's analysis fails if an objective's range is empty; in order to avoid such
            // scenarios, we set a custom reference point if necessary
            if (nadir.getObjective(i) - ideal.getObjective(i) < Settings.EPS) {
                Log.comment("WARNING: hard-coded reference point value");
                val = 1.0;
            }
            s.setObjective(i, val * r);
        }
        String line = "ref: ";
        for(int i = 0; i < s.getNumberOfObjectives(); i++){
            line += s.getObjective(i) + " ";
        }
        System.out.println(line);
        return s;
    }
    
    // TODO: returned reference set should be read-only
    public Population getNormalizedReferenceSet() {
        Population p = getMergedSet();
        if (p.size() == 0) { return p; }
        // MOEA Framework's analysis fails if the reference set has a single individual or if an objective's
        // range is empty; in order to avoid such scenarios, new solutions are injected based on the reference
        // and ideal points
        boolean single = p.size() == 1;
        Solution nadir = getNadirPoint(), ideal = getIdealPoint(), ref = getReferencePoint();
        for (int i = 0; i < this.norm_problem.getNumberOfObjectives(); ++i) {
            if (single || nadir.getObjective(i) - ideal.getObjective(i) < Settings.EPS) {
                Solution s = ideal.copy();
                s.setObjective(i, 1.0);
                // This next step is required when the reference set has a single solution, or else the
                // new point would be dominated by it
                int j = (i + 1) % this.norm_problem.getNumberOfObjectives();
                s.setObjective(j, s.getObjective(j) - 2*Settings.EPS);
                p.add(s);
                Log.comment("WARNING: injected solution in reference set");
            }
        }
        Log.comment(1, ":ref-set-size " + p.size());
        return p;
    }
    
}
