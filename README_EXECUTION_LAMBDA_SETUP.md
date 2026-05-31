# Code Service - Lambda Execution Setup (Local + AWS)

This document explains how to configure and validate the three Lambda functions used by Code Service for code execution. The intent is to run Java, Python, and JavaScript snippets in isolated Lambda functions and return execution output (or compile/runtime errors) back to the UI.

AWS Account creation step skipped.

## 1. Execution Contract (Code Service <-> Lambda)

### Request payload (from Code Service to Lambda)
```json
{
  "code": "<source code>",
  "language": "java|python|javascript",
  "tagName": "optional label",
  "timeoutMs": 5000
}
```

### Response payload (from Lambda to Code Service)
```json
{
  "status": "COMPLETED|COMPILE_ERROR|RUNTIME_ERROR|FAILED",
  "output": "<stdout/stderr/compile error>",
  "executionTimeMs": 123,
  "memoryUsedMb": 64
}
```

### Status semantics
- COMPLETED: Code ran successfully. `output` contains stdout.
- COMPILE_ERROR: Compilation failed (Java). `output` contains compiler error output.
- RUNTIME_ERROR: Program crashed or non-zero exit code. `output` contains stderr.
- FAILED: Lambda invocation failed (timeout, validation error, etc.).

## 2. Lambda Functions (3 total)

Recommended Lambda names (configurable):
- JavaScript: `coderank-exec-js`
- Python: `coderank-exec-python`
- Java: `coderank-exec-java`

Code Service will route requests based on the `language` field.

## 3. Local Prerequisites

Install these tools on your machine:
- AWS CLI v2
- AWS SAM CLI
- Docker Desktop
- Java 21 (for local testing and building Java lambda container)

## 4. AWS Credentials (Real AWS)

Configure credentials and region (example uses ap-south-1):
```bash
aws configure --profile coderank
# AWS Access Key ID: <your key>
# AWS Secret Access Key: <your secret>
# Default region name: ap-south-1
# Default output format: json
```

If you use AWS SSO:
```bash
aws configure sso --profile coderank
aws sso login --profile coderank
```

## 5. Local Lambda (SAM Local) Setup

The fastest local validation is to run lambdas via SAM Local. Code Service can be configured to call the local Lambda endpoint.

### 5.1 Create a local lambda workspace
Suggested structure:
```
Execution-Lambda/
  template.yaml
  js/handler.js
  python/handler.py
  java/Dockerfile
```

### 5.2 Sample SAM template
Save as `template.yaml`:
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: CodeRank execution lambdas

Resources:
  ExecJavascript:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: coderank-exec-js
      Runtime: nodejs20.x
      Handler: handler.handler
      CodeUri: js/
      Timeout: 10
      MemorySize: 256

  ExecPython:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: coderank-exec-python
      Runtime: python3.12
      Handler: handler.handler
      CodeUri: python/
      Timeout: 10
      MemorySize: 256

  ExecJava:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: coderank-exec-java
      PackageType: Image
      Timeout: 15
      MemorySize: 512
      ImageUri: coderank-exec-java:latest
```

### 5.3 JavaScript lambda handler (js/handler.js)
```javascript
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

exports.handler = async (event) => {
  const code = event.code || '';
  const timeoutMs = event.timeoutMs || 5000;
  const filePath = path.join('/tmp', 'main.js');
  fs.writeFileSync(filePath, code, 'utf8');
  const start = Date.now();

  return new Promise((resolve) => {
    exec(`node ${filePath}`, { timeout: timeoutMs }, (error, stdout, stderr) => {
      const elapsed = Date.now() - start;
      if (error) {
        resolve({
          status: 'RUNTIME_ERROR',
          output: stderr || error.message,
          executionTimeMs: elapsed
        });
        return;
      }
      resolve({
        status: 'COMPLETED',
        output: stdout,
        executionTimeMs: elapsed
      });
    });
  });
};
```

### 5.4 Python lambda handler (python/handler.py)
```python
import os
import tempfile
import subprocess
import time


def handler(event, context):
    code = event.get('code', '')
    timeout_ms = event.get('timeoutMs', 5000)

    fd, path = tempfile.mkstemp(suffix='.py')
    with os.fdopen(fd, 'w') as f:
        f.write(code)

    start = time.time()
    try:
        result = subprocess.run(
            ['python3', path],
            capture_output=True,
            text=True,
            timeout=timeout_ms / 1000
        )
        output = (result.stdout or '') + (result.stderr or '')
        status = 'COMPLETED' if result.returncode == 0 else 'RUNTIME_ERROR'
    except Exception as exc:
        status = 'RUNTIME_ERROR'
        output = str(exc)

    return {
        'status': status,
        'output': output,
        'executionTimeMs': int((time.time() - start) * 1000)
    }
```

### 5.5 Java lambda container (java/Dockerfile)
Java compile/run requires a full JDK. The simplest option is a Lambda container image using Corretto 21.
```dockerfile
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21

RUN yum -y install tar gzip && yum clean all

COPY handler /var/task/handler
WORKDIR /var/task/handler

CMD ["python3", "handler.py"]
```

For Java execution, use a small Python handler inside the container that calls `javac` and `java` in /tmp. The handler can read the same request contract and return status/output.

### 5.6 Run locally with SAM
From `Execution-Lambda/`:
```bash
sam build
sam local start-lambda --host 127.0.0.1 --port 3001
```

## 6. Code Service Local Config (SAM Local)

Set these environment variables when running Code Service locally:
```powershell
$env:EXECUTION_MODE="sam-local"
$env:EXECUTION_SAM_ENDPOINT="http://127.0.0.1:3001"
$env:EXECUTION_AWS_REGION="ap-south-1"
$env:EXECUTION_AWS_PROFILE="coderank"
$env:EXECUTION_LAMBDA_JS="coderank-exec-js"
$env:EXECUTION_LAMBDA_PYTHON="coderank-exec-python"
$env:EXECUTION_LAMBDA_JAVA="coderank-exec-java"
```

## 7. Deploy to AWS (Optional)

To deploy real lambdas:
```bash
sam build
sam deploy --guided --profile coderank --region ap-south-1
```

Ensure Code Service uses `EXECUTION_MODE=aws` and the lambda names above.

## 8. Notes and Safety

- Lambda executes untrusted code. Keep timeouts and memory limits tight.
- Never enable network access from the execution lambda unless required.
- Return compile errors explicitly for Java to show them in the UI.

## 9. Quick local build & run (SAM)

Follow these exact steps from a terminal to build the lambdas and run the local Lambda endpoint that `Code Service` can call.

1) Open a terminal and change into the lambda folder:

```powershell
cd "c:\Work\AirTribe\capstone project\Execution-Lambda"
```

2) Build the functions:

```powershell
# Recommended: use the container build to ensure Linux-compatible artifacts
sam build --use-container
```

If `sam build` fails for the Java image, try without `--use-container` first; if you see package-manager errors (yum/microdnf), ensure Docker Desktop is running and has enough resources.

3) Start the local Lambda endpoint (run in a new terminal window):

```powershell
sam local start-lambda --host 127.0.0.1 --port 3001
```

If you run this command from outside the `Execution-Lambda` folder, point to the template explicitly:

```powershell
sam local start-lambda -t "c:\Work\AirTribe\capstone project\Execution-Lambda\template.yaml" --host 127.0.0.1 --port 3001
```

4) Quick test invocation (PowerShell / Windows):

```powershell
aws lambda invoke --function-name coderank-exec-js \
  --endpoint-url http://127.0.0.1:3001 \
  --payload '{"code":"console.log(\"Hello from local JS lambda\")","language":"javascript"}' \
  response.json --cli-binary-format raw-in-base64-out

Get-Content response.json
```

Linux / macOS (bash):

```bash
aws lambda invoke --function-name coderank-exec-js \
  --endpoint-url http://127.0.0.1:3001 \
  --payload '{"code":"console.log(\"Hello from local JS lambda\")","language":"javascript"}' \
  response.json --cli-binary-format raw-in-base64-out

cat response.json
```

5) If the invoke fails with "could not find template" or similar, ensure you ran the `sam` commands from the `Execution-Lambda` folder or used `-t <path/to/template.yaml>`.

6) Docker / build troubleshooting notes
- If you get `/bin/sh: line 1: yum: command not found`, that means the base image uses `microdnf` instead of `yum`. The Java `Dockerfile` in this repo was already updated to use `microdnf`.
- If the Java build fails because `java-21-amazon-corretto-devel` is not available in the image's package repos, an alternative is to use an Amazon Corretto base image for Java lambdas (preinstalled JDK) and adapt the Lambda handler accordingly. Example base image:

```dockerfile
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21
# add python runtime glue or a small wrapper if you need the Lambda python handler
```

- Ensure Docker Desktop has enough memory (>= 4 GB) and CPU (>= 2 cores) allocated in its settings when building container images.

7) After `sam local start-lambda` is running, set the `Code Service` local env vars and start the service. Then open the UI and click Run.

PowerShell example to set env vars for the current session:

```powershell
$env:EXECUTION_MODE="sam-local"
$env:EXECUTION_SAM_ENDPOINT="http://127.0.0.1:3001"
$env:EXECUTION_AWS_REGION="ap-south-1"
$env:EXECUTION_AWS_PROFILE="coderank"
$env:EXECUTION_LAMBDA_JS="coderank-exec-js"
$env:EXECUTION_LAMBDA_PYTHON="coderank-exec-python"
$env:EXECUTION_LAMBDA_JAVA="coderank-exec-java"
```

8) Extra: If you prefer a scripted smoke-test, run this Node/Python curl-style test (uses `curl`):

```bash
curl -s -XPOST "http://127.0.0.1:3001/2015-03-31/functions/coderank-exec-js/invocations" \
  -H "Content-Type: application/json" \
  -d '{"code":"console.log(\"smoke\")","language":"javascript"}'
```

If you want, I can add a `test-invoke.sh`/`test-invoke.ps1` script into `Execution-Lambda/` to automate these checks.
