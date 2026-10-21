# BLACKOUT — Data Center Failure & Recovery Simulator

This pack is a coordinated, self-contained Java 17 module for the BLACKOUT team project.
All packs use the same package names, shared model classes, and folder layout. When combining
packs, copy each member's assigned files into one project; shared model files are identical.

## Run
Install JDK 17 or newer. From this folder:
```bash
mvn package
java -cp target/classes blackout.cli.Main
```
If Maven is not installed, compile with `javac` using the Java files under `src/main/java`.

## Important integration rule
Do not replace a teammate's assigned files with another pack's files. Copy only the files
listed in that pack's `PACK_CONTENTS.md`, plus shared `model` files if missing.
