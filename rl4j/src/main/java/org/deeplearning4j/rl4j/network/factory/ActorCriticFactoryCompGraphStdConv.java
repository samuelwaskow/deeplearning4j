/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.deeplearning4j.rl4j.network.factory;

import lombok.Value;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.FeedForwardToRnnPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.RnnToCnnPreProcessor;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.rl4j.network.configuration.ActorCriticNetworkConfiguration;
import org.deeplearning4j.rl4j.util.Constants;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

@Value
public class ActorCriticFactoryCompGraphStdConv implements GraphFactory {

    ActorCriticNetworkConfiguration conf;

    public ComputationGraph[] build(int[] numInputs, int numOutputs) {

        if (numInputs.length == 1)
            throw new AssertionError("Impossible to apply convolutional layer on a shape == 1");

        int h = (((numInputs[1] - 8) / 4 + 1) - 4) / 2 + 1;
        int w = (((numInputs[2] - 8) / 4 + 1) - 4) / 2 + 1;

        ComputationGraphConfiguration.GraphBuilder confB =
                        new NeuralNetConfiguration.Builder().seed(Constants.NEURAL_NET_SEED)
                                        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                                        .updater(conf.getUpdater() != null ? conf.getUpdater() : new Adam())
                                        .weightInit(WeightInit.XAVIER)
                                        .l2(conf.getL2()).graphBuilder()
                                        .addInputs("input").addLayer("0",
                                                        new ConvolutionLayer.Builder(8, 8).nIn(numInputs[0]).nOut(16)
                                                                        .stride(4, 4).activation(Activation.RELU).build(),
                                                        "input");

        confB.addLayer("1", new ConvolutionLayer.Builder(4, 4).nIn(16).nOut(32).stride(2, 2).activation(Activation.RELU).build(), "0");

        confB.addLayer("2", new DenseLayer.Builder().nIn(w * h * 32).nOut(256).activation(Activation.RELU).build(), "1");

        if (conf.isUseLSTM()) {
            confB.addLayer("3", new LSTM.Builder().nIn(256).nOut(256).activation(Activation.TANH).build(), "2");

            confB.addLayer("value", new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.IDENTITY)
                            .nIn(256).nOut(1).build(), "3");

            confB.addLayer("softmax", new RnnOutputLayer.Builder(new ActorCriticLoss()).activation(Activation.SOFTMAX)
                            .nIn(256).nOut(numOutputs).build(), "3");
        } else {
            confB.addLayer("value", new OutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.IDENTITY)
                            .nIn(256).nOut(1).build(), "2");

            confB.addLayer("softmax", new OutputLayer.Builder(new ActorCriticLoss()).activation(Activation.SOFTMAX)
                            .nIn(256).nOut(numOutputs).build(), "2");
        }

        confB.setOutputs("value", "softmax");

        if (conf.isUseLSTM()) {
            confB.inputPreProcessor("0", new RnnToCnnPreProcessor(numInputs[1], numInputs[2], numInputs[0]));
            confB.inputPreProcessor("2", new CnnToFeedForwardPreProcessor(h, w, 32));
            confB.inputPreProcessor("3", new FeedForwardToRnnPreProcessor());
        } else {
            confB.setInputTypes(InputType.convolutional(numInputs[1], numInputs[2], numInputs[0]));
        }

        ComputationGraphConfiguration cgconf = confB.build();
        ComputationGraph model = new ComputationGraph(cgconf);
        model.init();
        if (conf.getListeners() != null) {
            model.setListeners(conf.getListeners());
        } else {
            model.setListeners(new ScoreIterationListener(Constants.NEURAL_NET_ITERATION_LISTENER));
        }

        return new ComputationGraph[] { model };
    }
}
