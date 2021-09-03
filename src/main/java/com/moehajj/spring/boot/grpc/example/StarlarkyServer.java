package com.moehajj.spring.boot.grpc.example;

import com.moehajj.starlarky.example.StarlarkyServiceExampleGrpc;
import com.moehajj.starlarky.example.StarlarkyReplyExample;
import com.moehajj.starlarky.example.StarlarkyRequestExample;
import com.verygood.security.larkyapi.LarkyRuntime;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class StarlarkyServer {
    public static void main(String[] args) {
        SpringApplication.run(StarlarkyServer.class, args);
    }

    @GRpcService
    public static class StarlarkyService extends StarlarkyServiceExampleGrpc.StarlarkyServiceExampleImplBase {

        private final LarkyRuntime engine = new LarkyRuntime();
        private final String INPUT_BINDING_KEY = "script_input";
        private final String OUTPUT_BINDING_KEY = "script_output";
        private final String FUNCTION_HANDLER = "handle";
        private final String INVOKER = String.format("%s = %s(%s)",
            OUTPUT_BINDING_KEY, FUNCTION_HANDLER, INPUT_BINDING_KEY);

        @Override
        public void compute(StarlarkyRequestExample request, StreamObserver<StarlarkyReplyExample> responseObserver) {

            // Extract input
            String script = String.format("%s\n%s", request.getScript(), INVOKER);
            String input = request.getInput();

            // Set script input context
            ScriptContext inputContext = new SimpleScriptContext();
            Bindings bindings = new SimpleBindings();
            bindings.put(INPUT_BINDING_KEY, input);
            inputContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            engine.setContext(inputContext);

            // Execute Script
            try {
                String output = (String) engine.executeScript(script, OUTPUT_BINDING_KEY, inputContext);
                StarlarkyReplyExample reply = StarlarkyReplyExample.newBuilder().setOutput(output).build();

                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch(ScriptException e) {
                responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
            }
        }
    }
}