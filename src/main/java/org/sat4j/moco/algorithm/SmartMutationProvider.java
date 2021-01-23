package org.sat4j.moco.algorithm;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Variation;
import org.moeaframework.core.spi.OperatorProvider;
import org.moeaframework.util.TypedProperties;

import java.util.Properties;

public class SmartMutationProvider extends OperatorProvider {

    @Override
    public String getMutationHint(Problem problem) {
        return null;
    }

    @Override
    public String getVariationHint(Problem problem) {
        return null;
    }

    @Override
    public Variation getVariation(String s, Properties properties, Problem problem) {
        if (s.equals("sm")) {
            TypedProperties typed_props = new TypedProperties(properties);
            SmartMutation sm = new SmartMutation(typed_props.getDouble("sm.rate", 0.0),
                                   typed_props.getBoolean("sm.evolutionary", false),
                                   problem);
            sm.setMax_conflicts(typed_props.getInt("sm.max_conflicts", 50000));
            sm.setImprove_max_conflicts(typed_props.getInt("sm.improve_max_conflicts", 50000));
            sm.setLWR(typed_props.getDouble("sm.lwr", 15.0));
            sm.setStratify(typed_props.getBoolean("sm.stratify", false));
            sm.setImprovement_relax(typed_props.getDouble("sm.improvement_rate", 0.4));
            return sm;
        }
        return null;
    }
}
