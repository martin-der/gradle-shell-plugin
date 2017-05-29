Gradle Shell Plugin
===================

## About

Gradle plugin for script (bash) unit testing and packaging

## Usage

### Package

In order to configure package creation just add this configuration to the `build.gradle` file :

```groovy
shell_package {
	from project.fileTree(".").include('*.sh')
	installer {
		readme.location = "README.md"
	}
}
```

### Test

In order to configure project testing,

add this code in the `build.gradle` file :

```groovy
shell_test {
	from project.fileTree(".").include('*.sh')
}
```

then, for each test script, source the the helper script at the beginning of script :

```bash
source "${MDU_SHELLTEST_TESTUNIT_RUNNER_INCLUDE}"
```

declare as many tests as needed like this

```bash
testMyFirstTest() {
	assertEquals 1 1
}

testMySecondTest() {
	assertEquals 2 2
}
```

finally run test with this command

```bash
runTests
```





## Tools

### Runner

Include test runner shell script before any test operation.
It's location is hold be the environment variable `MDU_SHELLTEST_TESTUNIT_RUNNER_INCLUDE`.

### Functions

After runner script as been sourced the following functions are available :

#### runTests

`runTests` runs all tests previously defined in the current filen then exits the script

#### mkTestResultsDir

`mkTestResultsDir` will create the directory for the current script test, using it's name for the path.
After a call to `mkTestResultsDir`, one can create files in the output directory located here `"$MDU_SHELLTEST_TEST_RESULTS_DIRECTORY"`

### Environment variables

The following environment variables can be used in **test** scripts :

* `MDU_SHELLTEST_TESTUNIT_RUNNER_INCLUDE` _path_ of script that must be source to run the tests
* `MDU_SHELLTEST_TESTUNIT_SHUNIT2_EXEC` _path_ of shunit2 executable ( should not be use directly ) 
* `MDU_SHELLTEST_PROJECT_DIRECTORY` _path_ of the project directory
* `MDU_SHELLTEST_TEST_ASSERTION_FAILURE_EXIT_CODE` tests should use this _integer value_ if they need to exit after a assertion failure
* `MDU_SHELLTEST_TEST_EXECUTION_ERROR_EXIT_CODE` tests should return this _integer value_ when encountering any configuration problem 
* `MDU_SHELLTEST_RESULTS_DIRECTORY` _path_ of the overall result directory
* `MDU_SHELLTEST_TEST_RESULTS_DIRECTORY` _path_ of the result directory dedicated to this test
* `MDU_SHELLTEST_TEST_NAME` this _string_ holds the name of the test
