Gradle Shell Plugin
===================

## About



## Usage

```groovy
shell_package {
	from project.fileTree(".").include('*.sh')
	installer {
		readme.location = "README.md"
	}
}

shell_test {
	from project.fileTree(".").include('*.sh')
}
```
