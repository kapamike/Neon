# HOW TO BUILD NEON

Run

```
$ mvn -DskipTests=true package
```

to generate the target/org.sat4j.moco-0.0.1-SNAPSHOT-jar-with-dependencies.jar jar file.

The MOCO solver supports an extended OPB format with multiple cost functions.


# HOW TO RUN NEON

The algorithm should be specified through the **-alg** option. The options are NSGAII, MOEAD and MCSE.
MCSE is enabled by default.

A time limit should always be provided using the **-t** option. The stochastic algorithms NSGAII and
MOEAD may not work properly without it.

Stratification is enabled by default. It can be disabled using the **-s** option.


The MCSE algorithm can be used by running:

```
$ java -jar target/org.sat4j.moco-0.0.1-SNAPSHOT-jar-with-dependencies.jar <intance path> -alg MCSE -t <time limit>
```

For the stochastic algorithms NSGAII and MOEAD a seed can be given using the **-seed** option.

To use the hybrid algorithms NSGAII and MOEAD, a rate for the smart operators can be given
using the **-smr** option. Default is set to 0.

The structure improvements technique is disabled by default. It can be enabled using the **-si** option.

A hybrid algorithm can the used by running:

```
$ java -jar target/org.sat4j.moco-0.0.1-SNAPSHOT-jar-with-dependencies.jar <intance path> -alg <stochastic algorithm> -smr <smart operator rate> -t <time limit>
```

A set of tests is given in the **tests** directory.


# HOW TO RUN THE ANALYZER

Neon includes an analysis tool for evaluating the quality of Pareto front approximations
through the hypervolume and inverted generational distance indicator.

Run it using the following command:

```
$ java -cp org.sat4j.moco.jar org.sat4j.moco.analysis.Analyzer <instance file> [<label>:<output file>]+
```

`<output file>` is expected to be in the output format produced by the MOCO solver.
If there exist multiple files for different runs of the same algorithm, these should have the same `<label>`.
