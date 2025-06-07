This template defines a lot a basic stuff for an open source kmp library.

Inside libraries based on this template you should consider the following:

DOCS: https://mflisar.github.io/kmp-template/

## IMPORTANT

Do not touch following directories:

    * build-logic
    * generator/docs-template
    * generator/scripts

All the above folders should be **replaced by a new version** if this template is updated.

Do only make edits in the following directories:

    * library
    * gradle

### USAGE

TODO

### UpdatePackageNames

    * is a task to update the package names in the project
    * it has 2 parameters:
        - `oldPackageName`: the old package name to be replaced
        - `newPackageName`: the new package name to be set
    * set the parameters in the `UpdatePackageNames` file
    * can also be used to update the package names at any given time

### UpdateDocs

    * is a task to update the documentation
    * no need to run it, it's jsut for local manual tests => the github action for building mkdocs will run it automatically

# TODO

* fail if mkdocs build does not find functions
* mkdocs template anpassen -> alle mflisar und ähnliche durch properties ersetzen
* generator + build-logic könnte eine library sein die man veröffentlicht - oder?