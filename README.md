Gradle Shell Plugin
===================

[![](https://jitpack.io/v/net.tetrakoopa/gradle-shell-plugin.svg)](https://jitpack.io/#net.tetrakoopa/gradle-shell-plugin)

## About

Gradle plugins for script (bash) unit testing and packaging

## Usage

Plugin is available from [jitpack](https://jitpack.io).

```groovy
buildscript{
	repositories{
		maven { url 'https://jitpack.io' }
	}
	dependencies {
		classpath "com.github.martin-der.gradle-shell-plugin:shell:v0.2.0"
	}
}
```

### 📦 Package

#### Setup

Apply the plugin :

```groovy
apply plugin: 'net.tetrakoopa.shell-package'
```

The minimal setup needs some sources :

```groovy
shell_package {

	source {
		from ("src") {
			into "bin"
			include('**/*.sh')
		}
		from file('README.md')
	}
}
```

then run `gradle shell-build`.


Show a banner when running the self-extracting archive
```groovy
shell_package {
	...
	banner {
		source "resource/banner.txt"
		modify { line -> line.replace("{{version}}", "1.2.3-beta") }
	}

}
```

Optionally show a README or execute a script after a successful installation

```groovy
shell_package {
	...
	installer {

		readme {
			location "resource/install-readme.md"
			modify = { 
				line -> line
					.replace("{{year}}", new Date().format("yyyy")) 
					.replace("{{author}}", "Alan Turing")
			}
		}
	}
}
```

Indicate a script that can be used as main to make the package executable

```groovy
	launcher {
		script = "bin/server.sh"
		environment = [
			MY_THEME_COLOR: 'green',
			MY_VARIABLE_THAT_HOLDS_THE_CONTENT_DIRECTORY: '{{MDU-SD_CONTENT-DIRECTORY}}'
		]
	}
```

#### 💻 Usage

Package can be extracted :
```shell
./my-package install
```

Or, if a `laucher` section is provided, package can be executed :
```shell
./my-package launch
```
The script indicated by `launcher.script` with be executed.
