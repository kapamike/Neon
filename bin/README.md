# HOW TO BUILD SAT4J-MOCO

Run

```
$ mvn -DskipTests=true package
```

to generate the target/org.sat4j.moco-0.0.1-SNAPSHOT-jar-with-dependencies.jar jar file.

The MOCO solver supports an extended OPB format with multiple cost functions.
Stratification is disabled by default. Should be enabled using the '-s' option.

# HOW TO RUN THE ANALYZER

The org.sat4j.moco.jar includes an analysis tool for evaluating the quality of Pareto front approximations
through indicators such as hypervolume and inverted generational distance.

Run it using the following command:

```
$ java -cp org.sat4j.moco.jar org.sat4j.moco.analysis.Analyzer <instance file> [<label>:<output file>]+
```

`<output file>` is expected to be in the output format produced by the MOCO solver.
If there exist multiple files for different runs of the same algorithm, these should have the same `<label>`.