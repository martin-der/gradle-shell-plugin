Gradle Shell Plugin
===================

## About

Gradle plugins for script (bash) unit testing and packaging

## Usage

Plugins are available from [jitpack](https://jitpack.io). So add its repository as dependency into your `build.gradle`:

~~~
buildscript{
	repositories{
		maven { url 'https://jitpack.io' }
	}
	dependencies {
		classpath "com.github.martin-der.gradle-shell-plugin:shell:<latest-version>"
	}
}
~~~

### Package

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

Optionally execute a script after a successful installation
~~~groovy
shell_package {
	...
	userScript {
		script.location = "bin/home-init"
		question = "Do you want to init home (i.e. run 'home-init')?"
	}
}
~~~


#### Execution

Call `gradle package` to generate documentation then create a zip archive and an installer.

### Test

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

Call `gradle shell-test` to run all test.


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



