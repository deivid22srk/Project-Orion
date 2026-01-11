#!/usr/bin/env python3
import os
import subprocess
import sys

token = "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"
os.environ['GITHUB_TOKEN'] = token

script_path = "/project/workspace/upload2.sh"

try:
    result = subprocess.run(
        ['bash', script_path],
        cwd='/project/workspace',
        capture_output=True,
        text=True,
        timeout=180
    )
    
    print(result.stdout)
    if result.stderr:
        print("STDERR:", result.stderr, file=sys.stderr)
    
    sys.exit(result.returncode)
    
except subprocess.TimeoutExpired:
    print("Script execution timed out after 180 seconds")
    sys.exit(1)
except Exception as e:
    print(f"Error executing script: {e}")
    sys.exit(1)
