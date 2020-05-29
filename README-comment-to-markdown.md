
Any block of comments before a function is assumed to be documentation.


A quick example...

~~~bash
# @description If `key` is in the list, then an environment variable named `[prefix]envVar` is exported.
#
# @arg $1 string key
# @arg $2 string value
# @arg $3 string list of '<confKey>=<envVar>' ( separated by CR )
# @arg $4? string prefix, added to the name of created environment var
#
# @exitcode 0 If a a variable is created
# @exitcode >0 otherwise
#
# @example convertConfigKeyAndExportToEnvVariableIfExists name
function convertConfigKeyAndExportToEnvVariableIfExists() {
    echo "This is a scary name for a shell function"
    return 1
}
~~~