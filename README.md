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

#### 🔧 Setup

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
#### 📃 Environment variables

| Name | Where | Explanation |
|------|-------|-------------|
| MDU_SD_CACHE_DIRECTORY | Launcher | This folder is available to be used a cache directory |
| MDU_SD_FIRST_LAUNCH | Launcher | `1` when this launch is the first one, `0` when an earlier launch already ran |

`MDU_SD_FIRST_LAUNCH` is decided on whether the package has to extract itself into place, so it
resets to `1` after an `install` (the installed copy is all that is kept from then on), and stays
`1` for every launch when `keepTemporaryDirectory` is `false`, since nothing survives a run.

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

#### 🎯 Default action

By default the package refuses to do anything until its first parameter says `install` or `launch`.
An `action` block makes one of them the default, so that `./my-package` on its own is a request :

```groovy
shell_package {
	...
	action {
		defaultAction 'launch'
	}
}
```

An explicit first parameter always wins : `./my-package install` installs even when the default is
`launch`, and `./my-package launch` launches even when the default is `install`.

The option is also available under its requested name through the named-argument shorthand —
`default` being a reserved word in Groovy, `action { default = 'launch' }` does not even parse :

```groovy
shell_package {
	...
	action(default: 'launch')
}
```

Only `'launch'` and `'install'` are accepted as a default, and `'launch'` requires a `launcher`
section (there would be nothing to launch otherwise) ; the build fails otherwise. An `action`
block written without any `default` changes nothing about the generated package, so it is warned
about during the build.
