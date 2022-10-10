# EventSignatureFilter #

## Guide ##
### 1. Main.java ###
Find **all event handler methods** from android SDK, which are stored in a database named listener.db located in the root directory of this project.

### 2. GetMethodList.java ###
After fully instrumentation, we can get a RAW LOG of method call trace, this program can extract pivot events' signatures, which are stored in a JSON file named event_signature.json located in the root directory of this project, and can help filter methods involved in the event signature set, which are stored in a txt file named MethodList.txt located in the root directory of this project. Specifically, MethodList.txt will be used in the process of finally instrumentation.

### 3. RecognizeEventSignature.java ###
After running final instrumented app, we can get a long RAW LOG, this program can recognize whether some events in the event set are executed.