#!/bin/bash
# fake_mktemp - A mock mktemp that uses TU_TEMP_DIR instead of /tmp
set -eu

create_temp_dir() {
    local base_dir="${TU_TEMP_DIR}"
    local template="${1:-tmp.XXXXXXXXXX}"
    
    # Remove any prefix path from template if present
    template="${template##*/}"
    
    # Ensure base directory exists
    mkdir -p "$base_dir" 2>/dev/null || {
        echo "ERROR: Cannot create directory $base_dir" >&2
        return 1
    }
    
    # Generate random suffix
    local random_suffix=$(cat /dev/urandom 2>/dev/null | tr -dc 'a-zA-Z0-9' 2>/dev/null | fold -w 10 | head -n 1)
    [[ -z "$random_suffix" ]] && random_suffix="$$" # fallback
    
    # Replace X's with random characters
    local temp_name="${template//X/$random_suffix}"
    # Ensure we have enough random chars (replace all X's)
    while [[ "$temp_name" == *X* ]]; do
        local more_random=$(cat /dev/urandom 2>/dev/null | tr -dc 'a-zA-Z0-9' 2>/dev/null | fold -w 1 | head -n 1)
        [[ -z "$more_random" ]] && more_random="a"
        temp_name="${temp_name/X/$more_random}"
    done
    
    echo "$base_dir/$temp_name"
}

# Function to create temporary directory with -d flag
create_temp_dir_with_flag() {
    local dir_path=$(create_temp_dir "$@")
    if [[ $? -eq 0 ]]; then
        mkdir -p "$dir_path" 2>/dev/null || {
            echo "ERROR: Cannot create directory $dir_path" >&2
            return 1
        }
        echo "$dir_path"
        return 0
    fi
    return 1
}

# Parse arguments
TEMP_DIR=false
DRY_RUN=false
TEMPLATE="tmp.XXXXXXXXXX"
VERBOSE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--directory)
            TEMP_DIR=true
            shift
            ;;
        -t|--tmpdir)
            # Ignore tmpdir flag in fake implementation
            shift 2
            ;;
        -u|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Usage: fake_mktemp [OPTIONS] [TEMPLATE]"
            echo "Options:"
            echo "  -d, --directory    Create a temporary directory"
            echo "  -u, --dry-run      Don't create anything, just print name"
            echo "  -v, --verbose      Print debug information"
            echo "  -t, --tmpdir DIR   (Ignored - uses TU_TEMP_DIR)"
            echo "Environment:"
            echo "  TU_TEMP_DIR        Directory to use instead of /tmp"
            exit 0
            ;;
        -*)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
        *)
            TEMPLATE="$1"
            shift
            ;;
    esac
done

# Debug output
if [[ "$VERBOSE" == "true" ]]; then
    echo "DEBUG: TU_TEMP_DIR=${TU_TEMP_DIR:-/tmp}" >&2
    echo "DEBUG: TEMPLATE=$TEMPLATE" >&2
    echo "DEBUG: TEMP_DIR=$TEMP_DIR" >&2
    echo "DEBUG: DRY_RUN=$DRY_RUN" >&2
fi

# Create the temporary file/directory
if [[ "$TEMP_DIR" == "true" ]]; then
    result=$(create_temp_dir_with_flag "$TEMPLATE")
else
    result=$(create_temp_dir "$TEMPLATE")
fi

if [[ $? -ne 0 ]]; then
    exit 1
fi

# Handle dry run
if [[ "$DRY_RUN" == "true" ]]; then
    echo "$result"
else
    # Actually create the file/directory if not dry run
    if [[ "$TEMP_DIR" == "true" ]]; then
        mkdir -p "$result" 2>/dev/null || {
            echo "ERROR: Cannot create directory $result" >&2
            exit 1
        }
    else
        touch "$result" 2>/dev/null || {
            echo "ERROR: Cannot create file $result" >&2
            exit 1
        }
    fi
    echo "$result"
fi