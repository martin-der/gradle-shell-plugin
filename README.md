Gradle Shell Plugin
===================

## About

Gradle plugins for script (bash) unit testing and packaging

## Usage

Plugins are available from [jitpack](https://jitpack.io). So add its repository as dependency into your `build.gradle`:

```
buildscript{
	repositories{
		// ... other repositories
		maven { url 'https://jitpack.io' }
	}
}
```

### Package

#### Setup

Apply the plugin :

```
apply plugin: 'net.tetrakoopa.shell-package'
```

In order to configure package creation just add this configuration to the `build.gradle` file :

```groovy
shell_package {

	from fileTree("src").include('*.sh')
	from fileTree(".").include('README.md')

	installer {
		readme.location = 'README.md'
	}
}
```

#### Execution

Call `gradle packages` to generate documentation then create a zip archive and an installer.

### Test

#### Setup

Apply the plugin :

```
apply plugin: 'net.tetrakoopa.shell-test'
```

In order to configure project testing, add this code in the `build.gradle` file :

```groovy
shell_test {
	from fileTree("test").include('*.sh')
	workingDir = project.file("test")
}
```

You are now ready to create tests in the directory 'test'.

See [test plugin README](test/REAME.md) for explanations about writing test.

#### Execution

Call `gradle shell-test` to run all test.

If the [shellcheck](https://www.shellcheck.net/) command if accessible in the PATH, it is possible to generate reports about scripts. 

First, explicit what are the main script.

```
shell_test {
	from fileTree("test").include('*.sh')
	// Added the main scripts !
	scriptFrom fileTree("src").include('*.sh')
	workingDir = project.file("test")
}
```

Then call `gradle shell-check` to get report about the scripts.


