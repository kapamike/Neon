package org.sat4j.moco.algorithm;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Variation;
import org.moeaframework.core.spi.OperatorProvider;
import org.moeaframework.util.TypedProperties;

import java.util.Properties;

public class SinglePointMutationProvider extends OperatorProvider {

    @Override
    public String getMutationHint(Problem problem) { return null; }

    @Override
    public String getVariationHint(Problem problem) { return null; }

    @Override
    public Variation getVariation(String s, Properties properties, Problem problem) {
        if (s.equals("spm")) {
            TypedProperties typed_props = new TypedProperties(properties);
            return new SinglePointMutation(typed_props.getDouble("spm.rate", 0.05));
        }
        return null;
    }
}
