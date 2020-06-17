Gradle Shell Plugin
===================

[![](https://jitpack.io/v/net.tetrakoopa/gradle-shell-plugin.svg)](https://jitpack.io/#net.tetrakoopa/gradle-shell-plugin)

## About

Gradle plugins for script (bash) unit testing and packaging

## Usage

Plugin is available from [jitpack](https://jitpack.io).

~~~
buildscript{
	repositories{
		maven { url 'https://jitpack.io' }
	}
	dependencies {
		classpath "com.github.martin-der.gradle-shell-plugin:shell:v0.1.2"
	}
}
~~~

### :package: Package

#### Setup

Apply the plugin :

~~~
apply plugin: 'net.tetrakoopa.shell-package'
~~~

This is the setup for package creation :

~~~groovy
shell_package {

	source {
		from ("src") {
			into "bin"
			include('**/*.sh')
		}
		from file('README.md')
	}
}
~~~

then run `gradle shell-build`

Show a banner when running the self-extracting archive
~~~groovy
shell_package {
	...
	banner {
		content.path = file('package/banner.txt')
	}

}
~~~

Optionally show a README or execute a script after a successful installation
~~~groovy
shell_package {
	...
	installer {
		readme.location = 'README.md'

		userScript {
			script.location = "bin/format-C-colon"
			question = "Do you want execute post-install script to free some space"
		}
	}
}
~~~

Packages are created by default in the project's build directory. This can be changed with the `output` configuration.

~~~groovy
shell_package {
	...
	output {
		distributionDirectory 'dist'
	}
}
~~~

Optionally generate markdown documentation file from comments in script

TODO syntax and examples [here](README-comment-to-markdown.md) 
 
~~~groovy
shell_package {
	...
	documentation {
		tableOfContent = true
		lot {
			into "share/doc"
		}
	}
}
~~~

#### Execution

Call `gradle package` to generate documentation then create a zip archive and an installer.

### :microscope: Test

#### Setup

Apply the plugin :

~~~
apply plugin: 'net.tetrakoopa.shell-test'
~~~

Test configuration is done by adding a collection of test script

~~~groovy
shell_test {
	from fileTree("test").include('*.sh')
	workingDir = project.file("test")
}
~~~

You are now ready to create tests in the directory 'test'.

See [test plugin README](test/README.md) for explanations about writing test.

#### Execution

Call `gradle shell-test` to run all tests.


~~~groovy
shell_test {
	from fileTree("test").include('*.sh')
	workingDir = project.file("test")

	naming {
		// will remove the '.sh' part from the generated test task name
		removeSuffix = true
	}

}
~~~

When the [shellcheck](https://www.shellcheck.net/) command if accessible in the PATH, it is possible to generate reports.
Script files must be set with `scriptFrom`. 
~~~groovy
shell_test {
    ...
	scriptFrom fileTree("src").include('**/*.sh')
	check {
        resultsDir 'check-result'
	}

}
~~~


